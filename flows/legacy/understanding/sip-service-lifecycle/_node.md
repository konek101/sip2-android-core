# Understanding: SIP Service Lifecycle

> Manages PJSIP endpoint initialization, transport creation, and service lifecycle.

## Phase: EXPLORING

## Hypothesis

The SIP service lifecycle is centered around `PjSipService.java` which:
- Extends Android `Service` class
- Initializes PJSIP stack in a background thread
- Creates transports (UDP, TCP, TLS)
- Manages endpoint lifecycle
- Handles configuration via Intent extras

## Sources

- `android/src/main/java/org/telon/sip2/PjSipService.java` - Main service implementation (1240 lines)
- `android/src/main/java/org/telon/sip2/PjSipLogWriter.java` - Custom log writer for PJSIP
- `android/src/main/java/org/telon/sip2/dto/AccountConfigurationDTO.java` - Account configuration DTO
- `android/src/main/java/org/pjsip/pjsua2/Endpoint.java` - PJSIP endpoint JNI wrapper
- `android/src/main/java/org/pjsip/pjsua2/EpConfig.java` - Endpoint configuration
- `android/src/main/java/org/pjsip/pjsua2/TransportConfig.java` - Transport configuration

## Validated Understanding

After analyzing PjSipService.java:

### Service Initialization Flow
1. `onStartCommand()` receives Intent with action and configuration
2. If not initialized: creates `HandlerThread` (foreground priority), `Handler` for message queue
3. Initializes `PjSipBroadcastEmiter`, `AudioManager`, `PowerManager`, `WifiManager`
4. Posts `load()` job to worker thread
5. Handles intent action immediately

### PJSIP Stack Initialization (`load()` method)
1. **Load native libraries**: `libopenh264.so` (H.264 codec), `libpjsua2.so` (PJSIP)
2. **Create endpoint**: `new Endpoint()`, `libCreate()`, `libRegisterThread()`
3. **Register main thread**: Posts runnable to main Looper for UI thread registration
4. **Configure endpoint** (`EpConfig`):
   - Log level: 10 (verbose)
   - User agent: "React Native Sip2 (version)" or custom
   - STUN servers: configurable
   - Media config: 8000Hz clock rate, quality=4, echo cancellation enabled
   - Thread count: 2 for media
   - Channel count: 1 (mono), no VAD
5. **Create transports**:
   - UDP transport (default, with QoS for voice)
   - TCP transport (with QoS for voice)
   - TLS transport (commented out in current code)
6. **Start library**: `libStart()`

### Transport Management
- Three transport IDs stored: `mUdpTransportId`, `mTcpTransportId`, `mTlsTransportId`
- QoS type: `PJ_QOS_TYPE_VOICE` for all transports
- Account creation selects transport based on configuration

### Service Lifecycle
- `onStartCommand()`: Returns `START_NOT_STICKY` (don't restart if killed)
- `onDestroy()`: Quits worker thread safely, destroys endpoint, unregisters receivers
- Thread safety: All SIP operations posted to `HandlerThread` via `job()` method

### Configuration Management
- `ServiceConfigurationDTO`: User agent, STUN servers, codecs
- `AccountConfigurationDTO`: Account credentials, transport, registration settings
- Configuration passed via Intent extras as Serializable Map

### Key Patterns
- **Single worker thread**: All SIP operations on dedicated `HandlerThread`
- **Broadcast events**: All results emitted via `PjSipBroadcastEmiter`
- **Intent-based API**: Actions defined in `PjActions` class
- **Object trash**: `mTrash` list prevents GC of JNI objects

## Children Identified

> Deeper concepts spawned during SPAWNING phase

| Child | Hypothesis | Status |
|-------|------------|--------|
| endpoint-initialization | PJSIP library init, thread registration, EpConfig | PENDING |
| transport-management | UDP/TCP/TLS transport creation, QoS settings | PENDING |
| codec-configuration | Codec enumeration, priority settings | PENDING |
| service-lifecycle | Android service lifecycle, wake locks, WiFi locks | PENDING |

## Dependencies

- **Uses**: pjsip-jni-integration (JNI wrappers for PJSIP)
- **Used by**: account-management, call-management, event-broadcasting

## Key Insights

1. **Thread isolation**: SIP operations run on dedicated HandlerThread with foreground priority to prevent ANR
2. **Native library management**: Two native libraries loaded (openh264, pjsua2), errors throw RuntimeException
3. **Dual transport**: UDP and TCP created by default, TLS commented out (security consideration)
4. **Echo cancellation**: Enabled with `ecOptions=1` (SPEEX), tail length 0 (disabled in current config)
5. **Audio optimization**: 8kHz sample rate, mono, quality=4, no VAD (voice activity detection disabled)

## ADR Candidates

- **ADR: PJSIP as SIP Stack** - Choice of PJSIP library for SIP protocol
- **ADR: Service-based Architecture** - Android Service for background SIP operations
- **ADR: Broadcast Event Pattern** - Intent broadcasts for cross-process communication
- **ADR: Threading Model** - HandlerThread for SIP operations isolation

## Flow Recommendation

- **Type**: SDD
- **Confidence**: high
- **Rationale**: Internal service logic, no stakeholder-facing documentation needed. Core business logic for SIP operations.

## Synthesis

> Updated during SYNTHESIZING phase after children complete

### From Children

**endpoint-initialization:**
- Native libraries loaded in dependency order (openh264 → pjsua2)
- Thread registration mandatory for all PJSIP-using threads (worker + UI)
- Media config optimized for voice: 8kHz, mono, SPEEX EC, no VAD
- Object trash pattern prevents JNI GC issues
- TLS transport commented out (UDP/TCP only)

### Combined Understanding

The SIP service lifecycle is a well-orchestrated sequence:

1. **Service Start**: Android calls `onStartCommand()`, initializes HandlerThread
2. **Native Load**: OpenH264 (video codec) → pjsua2 (PJSIP core)
3. **Endpoint Init**: Create, libCreate, register threads (worker + UI)
4. **Configuration**: EpConfig with voice-optimized media settings
5. **Transport Create**: UDP + TCP with QoS for voice
6. **Library Start**: Begin PJSIP main loop

**Key Architectural Decisions:**
- Single HandlerThread for all SIP operations (thread isolation)
- Broadcast event pattern for cross-process communication
- Intent-based API for action dispatching
- Object trash pattern for JNI memory safety

**Voice Optimization:**
- 8kHz narrowband audio (compatibility over quality)
- SPEEX echo cancellation
- No VAD (always transmit, avoids clipping)
- Mono channel, quality=4

## Bubble Up

> Summary to pass to parent during EXITING

- Service-based architecture with HandlerThread isolation
- PJSIP endpoint with UDP/TCP transports
- Voice-optimized media config (8kHz, SPEEX EC, no VAD)
- Broadcast event pattern for platform wrappers
- Intent-based action API with callback correlation

---

*Phase: EXITING | Depth: 1 | Parent: root*
