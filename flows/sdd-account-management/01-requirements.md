# Account Management - Requirements

> Spec-Driven Development document for SIP account lifecycle management.

**Status:** DRAFT  
**Type:** SDD (Internal Service Logic)  
**Module:** org.telon.sip2.PjSipAccount

---

## Overview

Account Management handles SIP account lifecycle including creation, configuration, registration, incoming call/message handling, and deletion. Each account represents a SIP identity with credentials and registration state.

## Functional Requirements

### FR-1: Account Extension

The account implementation MUST:
- Extend `org.pjsip.pjsua2.Account` class
- Inherit all PJSIP account functionality
- Override callbacks: onRegState, onIncomingCall, onInstantMessage
- Maintain references to: PjSipService, AccountConfigurationDTO, transport ID

### FR-2: Account Creation

The service MUST create accounts with:

**Authentication:**
- Method: Digest authentication
- Realm: Registrar server or "*" (wildcard)
- Username: From configuration
- Password: From configuration

**Identity:**
- ID URI: `"Display Name" <sip:user@domain>` or `<sip:user@domain>`
- Registrar URI: `sip:domain`
- Contact params: Optional
- Contact URI params: Optional

**Transport:**
- Default: TCP
- UDP: If configured
- TLS: If configured (requires TLS transport)
- QoS: PJ_QOS_TYPE_VOICE for media

**Registration:**
- Register on add: true (default)
- Timeout: 600 seconds (default)
- Headers: Optional Map<String, String>
- Contact params: Optional

**Video:**
- Auto-show incoming: true
- Auto-transmit outgoing: true
- Capture orientation: PJMEDIA_ORIENT_ROTATE_270DEG

### FR-3: Registration Management

The account MUST support:
- Manual registration via `register(renew: boolean)`
- Automatic registration on account creation (if regOnAdd=true)
- Re-registration via `renew=true`
- Deregistration via `renew=false`
- Registration state callback: `onRegState(OnRegStateParam prm)`

### FR-4: Registration State Callback

On registration state change:
- Store `prm.getReason()` in `reason` field
- Emit EVENT_REGISTRATION_CHANGED broadcast
- Include full account JSON in broadcast data

### FR-5: Incoming Call Handling

On incoming call:
- Create new `PjSipCall` instance with account and call ID
- Emit EVENT_CALL_RECEIVED broadcast
- Include call JSON in broadcast data

### FR-6: Incoming Message Handling

On instant message:
- Create new `PjSipMessage` instance with account and message params
- Emit EVENT_MESSAGE_RECEIVED broadcast
- Include message JSON in broadcast data

### FR-7: Account Serialization

The account MUST serialize to JSON with structure:
```json
{
  "id": number,
  "uri": "sip:user@domain",
  "name": "Display Name",
  "username": "user",
  "domain": "domain.com",
  "password": "secret",
  "proxy": "proxy.domain.com",
  "transport": "UDP",
  "contactParams": string|null,
  "contactUriParams": string|null,
  "regServer": "domain.com",
  "regTimeout": "600",
  "regContactParams": string|null,
  "regHeaders": {},
  "regOnAdd": true,
  "registration": {
    "status": number,
    "statusText": "OK",
    "active": true,
    "reason": "OK"
  }
}
```

### FR-8: Registration Status

Registration status MUST include:
- `status`: PJSIP status code (200=OK)
- `statusText`: Human-readable status
- `active`: Boolean indicating active registration
- `reason`: Last registration reason

### FR-9: Account Deletion

On account deletion:
- Remove from service's account list
- Close transport via `endpoint.transportClose(transportId)`
- Call `account.delete()` to free PJSIP resources

### FR-10: Error Handling

The account MUST handle errors:
- getInfo() failures: Return "Connecting..." for status
- Registration failures: Store reason, emit event
- Native call failures: Log and propagate to broadcast

## Non-Functional Requirements

### NFR-1: Thread Safety

- All account operations execute on HandlerThread
- Account callbacks (onRegState, etc.) execute on worker thread
- JSON serialization is thread-safe

### NFR-2: Performance

- Account creation: < 100ms (excluding network registration)
- Registration: Network-dependent, async
- JSON serialization: < 10ms

### NFR-3: Memory Management

- Account objects held in service's mAccounts list
- Prevent GC via mTrash pattern for JNI configs
- Clean deletion via evict() method

### NFR-4: Compatibility

- Support multiple accounts simultaneously
- Each account has independent registration state
- Transport shared across accounts (by protocol)

## Dependencies

### External Dependencies

- PJSIP Account class (JNI wrapper)
- PJSIP AccountConfig class
- PJSIP AuthCredInfo class
- Android Intent/Bundle for configuration

### Internal Dependencies

- `PjSipService`: Parent service, event emission
- `AccountConfigurationDTO`: Configuration object
- `PjSipCall`: Incoming call handling
- `PjSipMessage`: Incoming message handling
- `PjSipBroadcastEmiter`: Event broadcasting

## Constraints

- Single transport per account (selected at creation)
- TLS requires TLS transport (commented out by default)
- Video orientation fixed at 270° (mobile mounting)
- Digest authentication only (no Basic auth)

## Acceptance Criteria

1. Account creates successfully with valid credentials
2. Registration state changes emit EVENT_REGISTRATION_CHANGED
3. Incoming calls emit EVENT_CALL_RECEIVED
4. Incoming messages emit EVENT_MESSAGE_RECEIVED
5. Account JSON includes full registration status
6. Account deletion cleans up all resources
7. Multiple accounts can coexist
8. Transport selection respects configuration

---

*Generated by /legacy analysis*
