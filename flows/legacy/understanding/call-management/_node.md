# Understanding: Call Management

> Individual call lifecycle: creation, state machine, media control, hold/mute operations.

## Phase: EXPLORING

## Validated Understanding

From PjSipCall.java analysis (370 lines):

### Class Structure
```java
public class PjSipCall extends Call {
    - account: PjSipAccount
    - isHeld: boolean
    - isMuted: boolean
    + static videoWindow: VideoWindow
    + static videoPreview: VideoPreview
    + static mediaListeners: CopyOnWriteArrayList<PjSipVideoMediaChange>
}
```

### Call Operations

**Hold/Unhold:**
```java
hold() → setHold(CallOpParam) → emits call changed
unhold() → reinvite(CallOpParam) → emits call changed
```

**Mute/Unmute:**
```java
mute() → doMute(true) → disconnects capture media
unmute() → doMute(false) → reconnects capture media
```

**Redirect:**
```java
redirect(destination) → answer with 302 Moved Temporarily
```

### Callbacks
```java
onCallState(prm) → service.emmitCallStateChanged(this, prm)
onCallMediaEvent(prm) → notifies mediaListeners
onCallMediaState(prm) → connects audio media, emits call updated
```

### Audio Media Handling
```java
onCallMediaState:
  for each media:
    if AUDIO and ACTIVE:
      audioMedia = AudioMedia.typecastFromMedia(media)
      audioMedia.adjustRxLevel(1.5)
      audioMedia.adjustTxLevel(1.5)
      audioMedia.startTransmit(playbackDev)
      captureDev.startTransmit(audioMedia)
```

### JSON Serialization
```json
{
  "id": 123,
  "callId": "abc@192.168.1.1",
  "accountId": 1,
  "localContact": "...",
  "localUri": "...",
  "remoteContact": "...",
  "remoteUri": "...",
  "state": 4,  // PJSIP_INV_STATE_CONFIRMED
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

### Key Insights
1. **Extends PJSIP Call**: Inherits all call operations
2. **State tracking**: isHeld, isMuted flags
3. **Static video**: Shared VideoWindow and VideoPreview
4. **Media listeners**: CopyOnWriteArrayList for thread-safe iteration
5. **Audio levels**: 1.5x gain adjustment for both TX and RX

## ADR Candidates
- Audio gain adjustment (1.5x)
- Static video window sharing

## Flow Recommendation
- **Type**: SDD + TDD
- **Confidence**: high
- **Rationale**: Core logic + correctness-critical call state

## Bubble Up
- Call extends PJSIP Call with hold/mute/redirect operations
- State callbacks: onCallState, onCallMediaState
- Audio media auto-connected with 1.5x gain
- JSON serialization with full call state

---

*Phase: EXPLORING | Depth: 1 | Parent: root*
