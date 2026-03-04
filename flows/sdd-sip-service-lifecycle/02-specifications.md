# SIP Service Lifecycle - Specifications

> Technical specifications derived from code analysis.

**Status:** DRAFT  
**Type:** SDD  
**Module:** org.telon.sip2.PjSipService

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   PjSipService                          │
│  (extends android.app.Service)                          │
├─────────────────────────────────────────────────────────┤
│ Fields:                                                 │
│  - mEndpoint: Endpoint                                  │
│  - mUdpTransportId: int                                 │
│  - mTcpTransportId: int                                 │
│  - mAccounts: List<PjSipAccount>                        │
│  - mCalls: List<PjSipCall>                              │
│  - mHandler: Handler (worker thread)                    │
│  - mEmitter: PjSipBroadcastEmiter                       │
│  - mTrash: List<Object> (JNI object refs)               │
├─────────────────────────────────────────────────────────┤
│ Lifecycle Methods:                                      │
│  - onStartCommand(intent, flags, startId): int          │
│  - onDestroy(): void                                    │
│  - onBind(intent): IBinder (returns null)               │
├─────────────────────────────────────────────────────────┤
│ Private Methods:                                        │
│  - load(): void                                         │
│  - handle(intent): void                                 │
│  - job(runnable): void                                  │
│  - evict(account): void                                 │
│  - evict(call): void                                    │
└─────────────────────────────────────────────────────────┘
```

## Initialization Flow

### Sequence: Service Start

```
Android System → PjSipService.onStartCommand()
                    │
                    ├─► if (!mInitialized):
                    │     ├─► Create HandlerThread (FOREGROUND priority)
                    │     ├─► Create Handler (bound to worker looper)
                    │     ├─► Initialize PjSipBroadcastEmiter
                    │     ├─► Get system services:
                    │     │     - AudioManager
                    │     │     - PowerManager (wake locks)
                    │     │     - WifiManager (WiFi locks)
                    │     └─► mInitialized = true
                    │
                    ├─► post load() to worker thread
                    │
                    └─► post handle(intent) to worker thread
```

### Sequence: PJSIP Load

```
load() [worker thread]
  │
  ├─► System.loadLibrary("openh264")
  ├─► System.loadLibrary("pjsua2")
  │
  ├─► mEndpoint = new Endpoint()
  ├─► mEndpoint.libCreate()
  ├─► mEndpoint.libRegisterThread(currentThread.name)
  │
  ├─► Post to UI thread:
  │     └─► mEndpoint.libRegisterThread(mainThread.name)
  │
  ├─► epConfig = new EpConfig()
  ├─► Configure epConfig:
  │     ├─► logConfig.level = 10
  │     ├─► logConfig.consoleLevel = 10
  │     ├─► logConfig.writer = new PjSipLogWriter()
  │     ├─► uaConfig.userAgent = "React Native Sip2 (version)"
  │     ├─► uaConfig.stunServer = [config.stunServers]
  │     ├─► medConfig.clockRate = 8000
  │     ├─► medConfig.quality = 4
  │     ├─► medConfig.ecOptions = 1 (SPEEX)
  │     ├─► medConfig.ecTailLen = 0
  │     ├─► medConfig.threadCnt = 2
  │     ├─► medConfig.channelCount = 1
  │     └─► medConfig.noVad = true
  │
  ├─► mEndpoint.libInit(epConfig)
  ├─► mTrash.add(epConfig)
  │
  ├─► Create UDP transport:
  │     ├─► transportConfig = new TransportConfig()
  │     ├─► transportConfig.qosType = PJ_QOS_TYPE_VOICE
  │     └─► mUdpTransportId = endpoint.transportCreate(UDP, config)
  │
  ├─► Create TCP transport:
  │     ├─► transportConfig = new TransportConfig()
  │     ├─► transportConfig.qosType = PJ_QOS_TYPE_VOICE
  │     └─► mTcpTransportId = endpoint.transportCreate(TCP, config)
  │
  └─► mEndpoint.libStart()
```

## Data Structures

### ServiceConfigurationDTO

```java
class ServiceConfigurationDTO {
    - userAgent: String
    - stunServers: String (comma-separated)
    
    + fromMap(Map): ServiceConfigurationDTO
    + toJson(): JSONObject
    + isUserAgentNotEmpty(): boolean
    + isStunServersNotEmpty(): boolean
}
```

### AccountConfigurationDTO

```java
class AccountConfigurationDTO {
    - name: String
    - username: String
    - domain: String
    - password: String
    - proxy: String
    - transport: String (UDP|TCP|TLS)
    - contactParams: String
    - contactUriParams: String
    - regServer: String
    - regTimeout: Integer
    - regHeaders: Map<String, String>
    - regContactParams: String
    - regOnAdd: boolean
    
    + fromIntent(Intent): AccountConfigurationDTO
    + getNomalizedRegServer(): String
    + getRegUri(): String
    + getIdUri(): String
    + isTransportNotEmpty(): boolean
    + isRegTimeoutNotEmpty(): boolean
    + isProxyNotEmpty(): boolean
}
```

## Intent Actions

### Input Actions (Handled by Service)

| Action | Extra Parameters | Description |
|--------|-----------------|-------------|
| `ACTION_START` | service: ServiceConfigurationDTO | Start/restart service |
| `ACTION_SET_SERVICE_CONFIGURATION` | service: ServiceConfigurationDTO | Update service config |
| `ACTION_CREATE_ACCOUNT` | account: AccountConfigurationDTO | Create SIP account |
| `ACTION_REGISTER_ACCOUNT` | account_id: int, renew: boolean | Register account |
| `ACTION_DELETE_ACCOUNT` | account_id: int | Delete account |
| `ACTION_MAKE_CALL` | account_id: int, destination: String, settings: CallSettingsDTO | Make outgoing call |
| `ACTION_HANGUP_CALL` | call_id: int | Hangup call |
| `ACTION_ANSWER_CALL` | call_id: int | Answer incoming call |
| `ACTION_HOLD_CALL` | call_id: int | Hold call |
| `ACTION_UNHOLD_CALL` | call_id: int | Unhold call |
| `ACTION_MUTE_CALL` | call_id: int | Mute microphone |
| `ACTION_UNMUTE_CALL` | call_id: int | Unmute microphone |
| `ACTION_USE_SPEAKER_CALL` | call_id: int | Route to speaker |
| `ACTION_USE_EARPIECE_CALL` | call_id: int | Route to earpiece |
| `ACTION_XFER_CALL` | call_id: int, destination: String | Transfer call |
| `ACTION_DTMF_CALL` | call_id: int, digit: String | Send DTMF tone |

### Output Events (Broadcast by Emitter)

| Event | Data | Description |
|-------|------|-------------|
| `EVENT_STARTED` | accounts: [], calls: [], settings: {} | Service started |
| `EVENT_HANDLED` | callback_id: int, data?: {}, exception?: String | Action completed |
| `EVENT_ACCOUNT_CREATED` | account: AccountConfigurationDTO | Account created |
| `EVENT_REGISTRATION_CHANGED` | account: AccountDTO | Registration state changed |
| `EVENT_CALL_RECEIVED` | call: CallDTO | Incoming call |
| `EVENT_CALL_CHANGED` | call: CallDTO | Call state updated |
| `EVENT_CALL_TERMINATED` | call: CallDTO | Call ended |
| `EVENT_MESSAGE_RECEIVED` | message: SipMessageDTO | SIP message received |

## Thread Model

```
┌─────────────────────────────────────────────────────────┐
│                    Main Thread (UI)                     │
│  - Registered with PJSIP via libRegisterThread()        │
│  - Handles UI callbacks                                 │
│  - Posts commands to worker thread                      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              HandlerThread "PjSipService"               │
│  Priority: PROCESS.THREAD_PRIORITY_FOREGROUND           │
│  ─────────────────────────────────────────────────────  │
│  Message Queue (Handler):                               │
│  - load() - PJSIP initialization                        │
│  - handle(intent) - Action processing                   │
│  - job(runnable) - Arbitrary SIP operations             │
│  - evict(account) - Account removal                     │
│  - evict(call) - Call removal                           │
└─────────────────────────────────────────────────────────┘
```

## JNI Object Management

### Object Trash Pattern

```java
// Prevent GC of JNI objects still in use by native code
private List<Object> mTrash = new LinkedList<>();

// During initialization:
mTrash.add(epConfig);          // Endpoint config
mTrash.add(transportConfig);   // Transport configs
mTrash.add(cred);              // Auth credentials
mTrash.add(callSettings);      // Call settings
```

**Rationale:** JNI objects have native pointers that must remain valid. If Java GC collects the wrapper object while native code still references it, crashes occur. The trash list maintains strong references until service destruction.

## Error Handling

### Native Library Loading

```java
try {
    System.loadLibrary("openh264");
} catch (UnsatisfiedLinkError error) {
    Log.e(TAG, "Error while loading OpenH264", error);
    throw new RuntimeException(error);  // Fatal, cannot continue
}
```

### PJSIP Operations

```java
try {
    mEndpoint.libInit(epConfig);
} catch (Exception e) {
    Log.e(TAG, "Error while starting PJSIP", e);
    // Continue with degraded functionality or emit error event
}
```

## Configuration Values

### Media Configuration

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| clockRate | 8000 | Narrowband voice (compatibility) |
| quality | 4 | Medium-high (small filter resampling) |
| ecOptions | 1 | SPEEX echo cancellation |
| ecTailLen | 0 | Disabled (device-specific tuning) |
| threadCnt | 2 | Parallel media processing |
| channelCount | 1 | Mono (voice calls) |
| noVad | true | Always transmit (no clipping) |

### Logging

| Parameter | Value |
|-----------|-------|
| logConfig.level | 10 |
| logConfig.consoleLevel | 10 |

## Lifecycle States

```
┌──────────────┐
│   Created    │
└──────┬───────┘
       │ onStartCommand()
       ▼
┌──────────────┐
│ Initializing │ ──► load() on worker thread
└──────┬───────┘
       │ libStart() complete
       ▼
┌──────────────┐
│    Ready     │ ◄──► Handle actions (create account, make call, etc.)
└──────┬───────┘
       │ onDestroy()
       ▼
┌──────────────┐
│  Destroying  │ ──► libDestroy(), quit worker thread
└──────────────┘
```

## Testing Considerations

### Unit Test Challenges

1. **Native Dependencies**: PJSIP library requires mocking or instrumentation tests
2. **Android Service**: Requires Android framework (Robolectric or emulator)
3. **Thread Synchronization**: Worker thread operations need awaitility
4. **Broadcast Events**: Require BroadcastReceiver registration in tests

### Recommended Test Strategy

- **Unit Tests**: DTO serialization, configuration parsing
- **Instrumentation Tests**: Service lifecycle, account creation, call handling
- **Integration Tests**: End-to-end SIP call flow with mock PJSIP

---

*Generated by /legacy analysis*
