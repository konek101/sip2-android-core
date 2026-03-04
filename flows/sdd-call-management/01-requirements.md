# Call Management - Requirements & Specifications

> Spec-Driven Development document for SIP call lifecycle management.

**Status:** DRAFT  
**Type:** SDD (Internal Service Logic)  
**Module:** org.telon.sip2.PjSipCall

---

## Overview

Call Management handles individual SIP call lifecycle including state transitions, media control, hold/mute operations, and call actions (answer, hangup, transfer, redirect).

## Architecture

### Class Structure

```java
public class PjSipCall extends Call {
    // State
    - account: PjSipAccount
    - isHeld: boolean
    - isMuted: boolean
    
    // Static (shared)
    + static videoWindow: VideoWindow
    + static videoPreview: VideoPreview
    + static mediaListeners: CopyOnWriteArrayList<PjSipVideoMediaChange>
    
    // Operations
    + hold(): void
    + unhold(): void
    + mute(): void
    + unmute(): void
    + redirect(destination: String): void
    
    // Callbacks
    + onCallState(prm: OnCallStateParam): void
    + onCallMediaEvent(prm: OnCallMediaEventParam): void
    + onCallMediaState(prm: OnCallMediaStateParam): void
    
    // Serialization
    + toJson(): JSONObject
    + toJsonString(): String
}
```

## Functional Requirements

### FR-1: Call Extension

The call implementation MUST:
- Extend `org.pjsip.pjsua2.Call` class
- Inherit all PJSIP call functionality
- Override callbacks: onCallState, onCallMediaEvent, onCallMediaState
- Maintain references to: PjSipAccount, isHeld, isMuted flags

### FR-2: Hold Operation

On hold():
- Check if already held (return if true)
- Set isHeld = true
- Emit EVENT_CALL_CHANGED broadcast
- Send re-INVITE with hold SDP via `setHold(CallOpParam)`

### FR-3: Unhold Operation

On unhold():
- Check if not held (return if false)
- Set isHeld = false
- Emit EVENT_CALL_CHANGED broadcast
- Send re-INVITE to release from hold via `reinvite(CallOpParam)`

### FR-4: Mute Operation

On mute():
- Check if already muted (return if true)
- Set isMuted = true
- Call doMute(true)
- Emit EVENT_CALL_CHANGED broadcast

**doMute(true) implementation:**
- Get call info
- For each media:
  - If type == AUDIO and status == ACTIVE:
    - Cast to AudioMedia
    - Get AudDevManager
    - Call `mgr.getCaptureDevMedia().stopTransmit(audioMedia)`

### FR-5: Unmute Operation

On unmute():
- Check if not muted (return if false)
- Set isMuted = false
- Call doMute(false)
- Emit EVENT_CALL_CHANGED broadcast

**doMute(false) implementation:**
- Get call info
- For each media:
  - If type == AUDIO and status == ACTIVE:
    - Cast to AudioMedia
    - Get AudDevManager
    - Call `mgr.getCaptureDevMedia().startTransmit(audioMedia)`

### FR-6: Redirect Operation

On redirect(destination):
- Create Contact header with destination
- Create SipTxOption with Contact headers
- Create CallOpParam with status code 302 (Moved Temporarily)
- Call `answer(prm)` with redirect params

### FR-7: Call State Callback

On call state change:
- Call super.onCallState(prm)
- Emit EVENT_CALL_STATE_CHANGED broadcast via `service.emmitCallStateChanged(this, prm)`

### FR-8: Call Media Event Callback

On call media event:
- Call super.onCallMediaEvent(prm)
- Notify all mediaListeners: `listener.onChange()`

### FR-9: Call Media State Callback

On call media state change:
- Get call info
- For each media:
  - If type == AUDIO and status == ACTIVE:
    - Cast to AudioMedia
    - Adjust RX level: 1.5x
    - Adjust TX level: 1.5x
    - Start transmit to playback device
    - Start capture transmit to audio media
- Emit EVENT_CALL_CHANGED broadcast

### FR-10: Call Serialization

The call MUST serialize to JSON with structure:
```json
{
  "id": 123,
  "callId": "abc123@192.168.1.1",
  "accountId": 1,
  "localContact": "sip:user@192.168.1.1:5060",
  "localUri": "sip:user@domain",
  "remoteContact": "sip:remote@192.168.1.2:5060",
  "remoteUri": "sip:remote@domain",
  "state": 4,
  "stateText": "Confirmed",
  "connectDuration": 120,
  "totalDuration": 125,
  "held": false,
  "muted": false,
  "speaker": true,
  "lastStatusCode": 200,
  "lastReason": "OK",
  "remoteOfferer": true,
  "remoteAudioCount": 1,
  "remoteVideoCount": 0,
  "audioCount": 1,
  "videoCount": 0,
  "media": [...],
  "provisionalMedia": [...]
}
```

### FR-11: Duration Calculation

- connectDuration: -1 if not connected, else info.getConnectDuration().getSec()
- totalDuration: info.getTotalDuration().getSec()
- Connected states: CONFIRMED, DISCONNECTED

### FR-12: Media Info Serialization

Media array structure:
```json
[{
  "dir": "INOUT",
  "type": "AUDIO",
  "status": "ACTIVE",
  "audioStream": {
    "confSlot": 0
  },
  "videoStream": {
    "captureDevice": 0,
    "windowId": -1
  }
}]
```

## Non-Functional Requirements

### NFR-1: Thread Safety

- All call operations execute on HandlerThread
- mediaListeners uses CopyOnWriteArrayList for thread-safe iteration
- State flags (isHeld, isMuted) are simple booleans (no synchronization needed)

### NFR-2: Audio Quality

- RX gain: 1.5x (50% boost)
- TX gain: 1.5x (50% boost)
- Prevents clipping while ensuring audibility

### NFR-3: Video Support

- Static VideoWindow: Shared across all calls
- Static VideoPreview: Shared preview window
- Media listeners notified of video changes

### NFR-4: Performance

- Hold/unhold: < 200ms (network-dependent)
- Mute/unmute: < 50ms (local operation)
- JSON serialization: < 10ms

## Dependencies

### External Dependencies

- PJSIP Call class (JNI wrapper)
- PJSIP CallOpParam, CallSetting
- PJSIP AudioMedia, AudDevManager
- PJSIP SipHeader, SipTxOption

### Internal Dependencies

- `PjSipService`: Parent service, event emission
- `PjSipAccount`: Parent account
- `PjSipVideoMediaChange`: Video event listener interface
- `PjSipBroadcastEmiter`: Event broadcasting

## Constraints

- Single audio stream per call (channelCount=1)
- Video window shared statically
- Mute only affects capture (TX) audio
- Hold sends re-INVITE (requires SIP server support)

## Acceptance Criteria

1. Hold sets isHeld flag and sends re-INVITE
2. Unhold clears isHeld flag and sends re-INVITE
3. Mute disconnects capture media
4. Unmute reconnects capture media
5. Redirect answers with 302 status code
6. Call state changes emit events
7. Media state auto-connects audio with 1.5x gain
8. JSON serialization includes full call state

---

*Generated by /legacy analysis*
