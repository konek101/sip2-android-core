# Account Management - Specifications

> Technical specifications derived from code analysis.

**Status:** DRAFT  
**Type:** SDD  
**Module:** org.telon.sip2.PjSipAccount

---

## Architecture

### Class Structure

```java
public class PjSipAccount extends Account {
    // Fields
    - reason: String                    // Last registration reason
    - service: PjSipService             // Parent service reference
    - configuration: AccountConfigurationDTO  // Account configuration
    - transportId: Integer              // Associated transport ID
    
    // Constructor
    + PjSipAccount(service, transportId, configuration)
    
    // Public Methods
    + register(renew: boolean): void
    + getService(): PjSipService
    + getTransportId(): int
    + getConfiguration(): AccountConfigurationDTO
    + getRegistrationStatusText(): String
    + toJson(): JSONObject
    + toJsonString(): String
    
    // Override Methods
    + onRegState(prm: OnRegStateParam): void
    + onIncomingCall(prm: OnIncomingCallParam): void
    + onInstantMessage(prm: OnInstantMessageParam): void
}
```

### Inheritance Hierarchy

```
org.pjsip.pjsua2.Account (JNI wrapper)
    ↑
org.telon.sip2.PjSipAccount
```

## Account Creation Flow

### Sequence: doAccountCreate(configuration)

```
PjSipService.doAccountCreate(configuration)
  │
  ├─► cfg = new AccountConfig()
  │
  ├─► cred = new AuthCredInfo(
  │       "Digest",
  │       configuration.getNomalizedRegServer(),  // "*" if empty
  │       configuration.getUsername(),
  │       0,  // Data type (0 = plain text)
  │       configuration.getPassword()
  │   )
  │
  ├─► idUri = configuration.getIdUri()
  │   // "Name" <sip:user@domain> or <sip:user@domain>
  │
  ├─► regUri = configuration.getRegUri()
  │   // "sip:domain"
  │
  ├─► cfg.setIdUri(idUri)
  ├─► cfg.getRegConfig().setRegistrarUri(regUri)
  ├─► cfg.getRegConfig().setRegisterOnAdd(configuration.isRegOnAdd())
  ├─► cfg.getSipConfig().getAuthCreds().add(cred)
  │
  ├─► // Optional configuration
  │   ├─► if contactParams != null: cfg.getSipConfig().setContactParams(contactParams)
  │   ├─► if contactUriParams != null: cfg.getSipConfig().setContactUriParams(contactUriParams)
  │   ├─► if regHeaders != null:
  │   │     headers = new SipHeaderVector()
  │   │     for (key, value) in regHeaders:
  │   │       hdr = new SipHeader()
  │   │       hdr.setHName(key)
  │   │       hdr.setHValue(value)
  │   │       headers.add(hdr)
  │   │     cfg.getRegConfig().setHeaders(headers)
  │
  ├─► // Transport selection
  │   transportId = mTcpTransportId  // Default
  │   switch configuration.getTransport():
  │     case "UDP":  transportId = mUdpTransportId
  │     case "TLS":  transportId = mTlsTransportId
  │     default:     Log.w(TAG, "Illegal transport, using TCP")
  │
  ├─► cfg.getSipConfig().setTransportId(transportId)
  │
  ├─► // Proxy configuration
  │   if configuration.isProxyNotEmpty():
  │     v = new StringVector()
  │     v.add(configuration.getProxy())
  │     cfg.getSipConfig().setProxies(v)
  │
  ├─► // QoS for media transport
  │   cfg.getMediaConfig().getTransportConfig().setQosType(PJ_QOS_TYPE_VOICE)
  │
  ├─► // Video configuration
  │   cfg.getVideoConfig().setAutoShowIncoming(true)
  │   cfg.getVideoConfig().setAutoTransmitOutgoing(true)
  │   cap_dev = cfg.getVideoConfig().getDefaultCaptureDevice()
  │   mEndpoint.vidDevManager().setCaptureOrient(
  │       cap_dev, 
  │       PJMEDIA_ORIENT_ROTATE_270DEG, 
  │       true
  │   )
  │
  ├─► // Create account instance
  │   account = new PjSipAccount(this, transportId, configuration)
  │   account.create(cfg)
  │
  ├─► // Prevent GC
  │   mTrash.add(cfg)
  │   mTrash.add(cred)
  │
  ├─► mAccounts.add(account)
  │
  └─► return account
```

## Registration Flow

### Sequence: register(renew)

```
PjSipAccount.register(renew: boolean)
  │
  └─► setRegistration(renew)
      │
      ├─► renew=true:  Trigger re-registration
      ├─► renew=false: Deregister
      │
      └─► [Async] onRegState(OnRegStateParam prm)
          │
          ├─► reason = prm.getReason()
          └─► service.emmitRegistrationChanged(this, prm)
```

### Registration State Callback

```java
@Override
public void onRegState(OnRegStateParam prm) {
    reason = prm.getReason();  // Store for serialization
    service.emmitRegistrationChanged(this, prm);  // Broadcast event
}
```

**Broadcast Data:**
```json
{
  "id": 1,
  "uri": "sip:user@domain",
  "registration": {
    "status": 200,
    "statusText": "OK",
    "active": true,
    "reason": "OK"
  }
}
```

## Incoming Call Flow

### Sequence: onIncomingCall

```
PJSIP Stack → PjSipAccount.onIncomingCall(OnIncomingCallParam prm)
  │
  ├─► call = new PjSipCall(this, prm.getCallId())
  └─► service.emmitCallReceived(this, call)
```

**Broadcast Data:**
```json
{
  "id": 123,
  "callId": "abc123@192.168.1.1",
  "accountId": 1,
  "state": 1,  // PJSIP_INV_STATE_INCOMING
  "stateText": "Incoming",
  "remoteContact": "sip:caller@domain",
  "remoteUri": "sip:caller@domain"
}
```

## Incoming Message Flow

### Sequence: onInstantMessage

```
PJSIP Stack → PjSipAccount.onInstantMessage(OnInstantMessageParam prm)
  │
  ├─► message = new PjSipMessage(this, prm)
  └─► service.emmitMessageReceived(this, message)
```

## Account Serialization

### toJson() Implementation

```java
public JSONObject toJson() {
    JSONObject json = new JSONObject();
    JSONObject registration = new JSONObject();
    
    try {
        // Registration status
        registration.put("status", getInfo().getRegStatus());
        registration.put("statusText", getInfo().getRegStatusText());
        registration.put("active", getInfo().getRegIsActive());
        registration.put("reason", reason);
        
        // Account info
        json.put("id", getId());
        json.put("uri", getInfo().getUri());
        json.put("name", configuration.getName());
        json.put("username", configuration.getUsername());
        json.put("domain", configuration.getDomain());
        json.put("password", configuration.getPassword());
        json.put("proxy", configuration.getProxy());
        json.put("transport", configuration.getTransport());
        
        json.put("contactParams", configuration.getContactParams());
        json.put("contactUriParams", configuration.getContactUriParams());
        
        json.put("regServer", configuration.getRegServer());
        json.put("regTimeout", configuration.isRegTimeoutNotEmpty() 
            ? String.valueOf(configuration.getRegTimeout()) 
            : "");
        json.put("regContactParams", configuration.getRegContactParams());
        json.put("regHeaders", configuration.getRegHeaders());
        json.put("regOnAdd", configuration.isRegOnAdd());
        
        json.put("registration", registration);
        
        return json;
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

### JSON Schema

```typescript
interface AccountDTO {
  id: number;
  uri: string;
  name: string | null;
  username: string;
  domain: string;
  password: string;
  proxy: string | null;
  transport: string;  // "UDP" | "TCP" | "TLS"
  contactParams: string | null;
  contactUriParams: string | null;
  regServer: string | null;
  regTimeout: string;  // Numeric string or empty
  regContactParams: string | null;
  regHeaders: Record<string, string>;
  regOnAdd: boolean;
  registration: {
    status: number;  // PJSIP status code
    statusText: string;
    active: boolean;
    reason: string;
  };
}
```

## Account Deletion Flow

### Sequence: evict(account)

```
PjSipService.evict(PjSipAccount account)
  │
  ├─► mAccounts.remove(account)
  │
  ├─► mEndpoint.transportClose(account.getTransportId())
  │   └─► Closes transport connection
  │
  └─► account.delete()
      └─► Frees PJSIP resources
```

## Configuration Methods

### AccountConfigurationDTO

```java
public class AccountConfigurationDTO {
    // Basic auth
    public String name;
    public String username;
    public String domain;
    public String password;
    
    // Network
    public String proxy;
    public String transport;  // "UDP", "TCP", "TLS"
    
    // SIP params
    private String contactParams;
    private String contactUriParams;
    public String regServer;
    public Integer regTimeout;  // Default: 600
    public Map<String, String> regHeaders;
    public String regContactParams;
    public boolean regOnAdd;  // Default: true
    
    // Helper methods
    public String getNomalizedRegServer(): String  // "*" if empty
    public String getRegUri(): String  // "sip:domain"
    public String getIdUri(): String  // "Name" <sip:user@domain>
    public boolean isTransportNotEmpty(): boolean
    public boolean isRegTimeoutNotEmpty(): boolean
    public boolean isProxyNotEmpty(): boolean
    
    public static AccountConfigurationDTO fromIntent(Intent intent)
}
```

### fromIntent() Implementation

```java
public static AccountConfigurationDTO fromIntent(Intent intent) {
    AccountConfigurationDTO c = new AccountConfigurationDTO();
    c.name = intent.getStringExtra("name");
    c.username = intent.getStringExtra("username");
    c.domain = intent.getStringExtra("domain");
    c.password = intent.getStringExtra("password");
    c.proxy = intent.getStringExtra("proxy");
    c.transport = intent.getStringExtra("transport");
    c.contactParams = intent.getStringExtra("contactParams");
    c.contactUriParams = intent.getStringExtra("contactUriParams");
    c.regServer = intent.getStringExtra("regServer");
    c.regTimeout = 600;  // Default
    c.regOnAdd = intent.getBooleanExtra("regOnAdd", true);
    
    // Parse timeout
    if (intent.hasExtra("regTimeout")) {
        String regTimeout = intent.getStringExtra("regTimeout");
        if (regTimeout != null && !regTimeout.isEmpty()) {
            int timeout = Integer.parseInt(regTimeout);
            if (timeout > 0) {
                c.regTimeout = timeout;
            }
        }
    }
    
    c.regContactParams = intent.getStringExtra("regContactParams");
    
    if (intent.hasExtra("regHeaders")) {
        c.regHeaders = (Map<String, String>) intent.getSerializableExtra("regHeaders");
    }
    
    return c;
}
```

## Callback Methods

### onRegState

```java
@Override
public void onRegState(OnRegStateParam prm) {
    reason = prm.getReason();
    service.emmitRegistrationChanged(this, prm);
}
```

**Triggered by:**
- Initial registration (on account add)
- Manual registration (register(renew))
- Re-registration (timeout expiry)
- Network changes

### onIncomingCall

```java
@Override
public void onIncomingCall(OnIncomingCallParam prm) {
    PjSipCall call = new PjSipCall(this, prm.getCallId());
    service.emmitCallReceived(this, call);
}
```

**Triggered by:**
- Incoming SIP INVITE

### onInstantMessage

```java
@Override
public void onInstantMessage(OnInstantMessageParam prm) {
    PjSipMessage message = new PjSipMessage(this, prm);
    service.emmitMessageReceived(this, message);
}
```

**Triggered by:**
- Incoming SIP MESSAGE request

## Error Handling

### getInfo() Failure

```java
public String getRegistrationStatusText() {
    try {
        return getInfo().getRegStatusText();
    } catch (Exception e) {
        return "Connecting...";
    }
}
```

### toJson() Failure

```java
try {
    // ... serialization
    return json;
} catch (Exception e) {
    throw new RuntimeException(e);
}
```

## Testing Considerations

### Unit Test Challenges

1. **PJSIP Dependency**: Account extends JNI wrapper, requires mocking
2. **Callback Testing**: onRegState, onIncomingCall need test harness
3. **Service Integration**: Requires PjSipService instance

### Recommended Test Strategy

- **Mock PjSipService**: For callback verification
- **Test DTO serialization**: AccountConfigurationDTO, JSON output
- **Integration tests**: Full account creation/deletion flow
- **Callback tests**: Verify event emission on callbacks

---

*Generated by /legacy analysis*
