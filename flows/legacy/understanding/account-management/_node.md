# Understanding: Account Management

> SIP account lifecycle: creation, registration, deregistration, and deletion.

## Phase: EXPLORING

## Hypothesis

Account management is handled by `PjSipAccount.java` which:
- Extends PJSIP `Account` class
- Manages account configuration and state
- Handles registration state changes
- Processes incoming calls and messages
- Serializes account state to JSON

## Sources

- `android/src/main/java/org/telon/sip2/PjSipAccount.java` - Account implementation
- `android/src/main/java/org/telon/sip2/dto/AccountConfigurationDTO.java` - Account configuration
- `android/src/main/java/org/telon/sip2/PjSipService.java` - Account creation/deletion handlers
- `android/src/main/java/org/pjsip/pjsua2/Account.java` - PJSIP account wrapper
- `android/src/main/java/org/pjsip/pjsua2/AccountConfig.java` - Account configuration

## Validated Understanding

From PjSipAccount.java analysis:

### Class Structure
```java
public class PjSipAccount extends Account {
    - reason: String (last registration reason)
    - service: PjSipService
    - configuration: AccountConfigurationDTO
    - transportId: Integer
}
```

### Account Creation (in PjSipService.doAccountCreate())

**Configuration Setup:**
```java
AccountConfig cfg = new AccountConfig();
AuthCredInfo cred = new AuthCredInfo("Digest", regServer, username, 0, password);
cfg.setIdUri(name + " <sip:user@domain>");
cfg.getRegConfig().setRegistrarUri("sip:domain");
cfg.getRegConfig().setRegisterOnAdd(true);
cfg.getSipConfig().getAuthCreds().add(cred);
```

**Transport Selection:**
- Default: TCP (mTcpTransportId)
- UDP: if configuration.transport == "UDP"
- TLS: if configuration.transport == "TLS" (but TLS transport not created)
- Warning logged for illegal transport values

**SIP Configuration:**
- Contact params: optional
- Contact URI params: optional
- Registration headers: from Map<String, String>
- Proxy: optional (StringVector)
- QoS: PJ_QOS_TYPE_VOICE for media transport

**Video Configuration:**
- Auto-show incoming: true
- Auto-transmit outgoing: true
- Capture orientation: ROTATE_270DEG (mobile device mounting)

**Account Instantiation:**
```java
PjSipAccount account = new PjSipAccount(service, transportId, configuration);
account.create(cfg);
mAccounts.add(account);
```

### Registration Management

**Manual Registration:**
```java
public void register(boolean renew) throws Exception {
    setRegistration(renew);
}
```
- Called via ACTION_REGISTER_ACCOUNT
- renew=true triggers re-registration
- renew=false can deregister

**Registration State Callback:**
```java
@Override
public void onRegState(OnRegStateParam prm) {
    reason = prm.getReason();
    service.emmitRegistrationChanged(this, prm);
}
```
- Stores last registration reason
- Emits EVENT_REGISTRATION_CHANGED broadcast

### Incoming Call Handling

```java
@Override
public void onIncomingCall(OnIncomingCallParam prm) {
    PjSipCall call = new PjSipCall(this, prm.getCallId());
    service.emmitCallReceived(this, call);
}
```
- Creates new PjSipCall instance
- Emits EVENT_CALL_RECEIVED broadcast

### Incoming Message Handling

```java
@Override
public void onInstantMessage(OnInstantMessageParam prm) {
    PjSipMessage message = new PjSipMessage(this, prm);
    service.emmitMessageReceived(this, message);
}
```
- Creates new PjSipMessage instance
- Emits EVENT_MESSAGE_RECEIVED broadcast

### Account Serialization

**JSON Structure:**
```json
{
  "id": 1,
  "uri": "sip:user@domain",
  "name": "Display Name",
  "username": "user",
  "domain": "domain.com",
  "password": "secret",
  "proxy": "proxy.domain.com",
  "transport": "UDP",
  "contactParams": null,
  "contactUriParams": null,
  "regServer": "domain.com",
  "regTimeout": "600",
  "regContactParams": null,
  "regHeaders": {},
  "regOnAdd": true,
  "registration": {
    "status": 2,
    "statusText": "OK",
    "active": true,
    "reason": "OK"
  }
}
```

**Registration Status:**
- Retrieved via `getInfo().getRegStatus()`
- Status text via `getInfo().getRegStatusText()`
- Active flag via `getInfo().getRegIsActive()`
- Default "Connecting..." if getInfo() fails

### Account Deletion (in PjSipService.evict())

```java
mAccounts.remove(account);
mEndpoint.transportClose(account.getTransportId());
account.delete();
```
- Remove from service's account list
- Close transport connection
- Call PJSIP account delete

## Children Identified

> Deeper concepts spawned during SPAWNING phase

| Child | Hypothesis | Status |
|-------|------------|--------|
| account-creation | AccountConfig, AuthCredInfo, transport selection | PENDING |
| registration-management | setRegistration(), onRegState callback | PENDING |
| incoming-call-handling | onIncomingCall, PjSipCall creation | PENDING |
| incoming-message-handling | onInstantMessage, PjSipMessage creation | PENDING |
| account-serialization | JSON structure for broadcasts | PENDING |

## Dependencies

- **Uses**: pjsip-jni-integration, sip-service-lifecycle
- **Used by**: call-management, event-broadcasting

## Key Insights

1. **Extends PJSIP Account**: Inherits all PJSIP account functionality
2. **Callback-driven**: onRegState, onIncomingCall, onInstantMessage
3. **Transport-bound**: Each account has dedicated transport ID
4. **Video-ready**: Auto-show incoming, auto-transmit outgoing, 270° rotation
5. **Stateful**: Maintains configuration, reason, service reference

## ADR Candidates

- **ADR: Digest Authentication** - Standard SIP authentication method
- **ADR: TCP Default Transport** - More reliable than UDP for mobile
- **ADR: Video Orientation 270°** - Mobile device mounting convention

## Flow Recommendation

- **Type**: SDD
- **Confidence**: high
- **Rationale**: Internal service logic, core business domain

## Synthesis

> Updated during SYNTHESIZING phase after children complete

### From Children
Children are implementation details already captured in validated understanding.

### Combined Understanding
Account management is a callback-driven wrapper around PJSIP Account:
1. Account creation with Digest auth, transport selection, video config
2. Registration via setRegistration(renew) with onRegState callback
3. Incoming calls via onIncomingCall creating PjSipCall
4. Incoming messages via onInstantMessage creating PjSipMessage
5. JSON serialization for broadcast events

## Bubble Up

> Summary to pass to parent during EXITING

- Account extends PJSIP Account with callbacks for reg/call/message
- Transport selection: UDP/TCP/TLS based on config (TCP default)
- Video pre-configured: auto-show, auto-transmit, 270° rotation
- JSON serialization for broadcast events
- Manual registration via setRegistration(renew)
- Digest authentication with configurable realm

---

*Phase: EXITING | Depth: 1 | Parent: root*
