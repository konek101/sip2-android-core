# Understanding: Endpoint Initialization

> PJSIP library initialization, thread registration, and endpoint configuration.

## Phase: EXPLORING

## Hypothesis

Endpoint initialization involves:
- Loading native libraries (openh264, pjsua2)
- Creating and initializing the PJSIP endpoint
- Registering threads for PJSIP access
- Configuring endpoint (logging, media, UA settings)

## Sources

- `android/src/main/java/org/telon/sip2/PjSipService.java` (lines 106-260) - load() method
- `android/src/main/java/org/pjsip/pjsua2/Endpoint.java` - PJSIP endpoint wrapper
- `android/src/main/java/org/pjsip/pjsua2/EpConfig.java` - Endpoint configuration
- `android/src/main/java/org/pjsip/pjsua2/LogConfig.java` - Logging configuration
- `android/src/main/java/org/pjsip/pjsua2/MediaConfig.java` - Media configuration
- `android/src/main/java/org/pjsip/pjsua2/UaConfig.java` - User agent configuration

## Validated Understanding

From PjSipService.java load() method analysis:

### Native Library Loading (lines 108-123)
```java
System.loadLibrary("openh264");  // H.264 video codec
System.loadLibrary("pjsua2");    // PJSIP core library
```
- Errors caught and wrapped in RuntimeException
- OpenH264 loaded first (dependency for video)
- pjsua2 is the main PJSIP library

### Endpoint Creation (lines 126-130)
```java
mEndpoint = new Endpoint();
mEndpoint.libCreate();
mEndpoint.libRegisterThread(Thread.currentThread().getName());
```
- `new Endpoint()`: Creates JNI wrapper object
- `libCreate()`: Initializes PJSIP library internals
- `libRegisterThread()`: Registers current thread for PJSIP access (required before any PJSIP calls)

### Main Thread Registration (lines 132-143)
```java
Handler uiHandler = new Handler(Looper.getMainLooper());
Runnable runnable = () -> {
    mEndpoint.libRegisterThread(Thread.currentThread().getName());
};
uiHandler.post(runnable);
```
- Posts registration to main Looper
- Ensures UI thread can also make PJSIP calls
- Required because PJSIP is not thread-safe without registration

### Endpoint Configuration (EpConfig) (lines 146-220)

**Log Configuration:**
- `logConfig.level = 10`: Maximum verbosity
- `logConfig.consoleLevel = 10`: Console output
- Custom `PjSipLogWriter` installed

**User Agent Configuration:**
- Default: "React Native Sip2 (version)"
- Customizable via ServiceConfigurationDTO

**STUN Configuration:**
- Configurable STUN servers for NAT traversal

**Media Configuration:**
- `clockRate = 8000`: 8kHz sample rate (narrowband voice)
- `quality = 4`: Medium-high quality (table: 5-10 = large filter, 3-4 = small filter)
- `ecOptions = 1`: SPEEX echo cancellation
- `ecTailLen = 0`: Echo tail length (0 = disabled)
- `threadCnt = 2`: Media processing threads
- `channelCount = 1`: Mono audio
- `noVad = true`: Voice Activity Detection disabled (always transmit)

**Transport Configuration:**
- UDP: Created with QoS type `PJ_QOS_TYPE_VOICE`
- TCP: Created with QoS type `PJ_QOS_TYPE_VOICE`
- TLS: Code commented out (not created)

### Library Start (line 257)
```java
mEndpoint.libStart();
```
- Starts PJSIP main loop
- Must be called after configuration, before any SIP operations

### Object Trash Pattern (lines 222-224)
```java
mTrash.add(epConfig);
mTrash.add(transportConfig);
```
- Prevents GC of JNI objects still in use by native code
- Critical for preventing crashes from dangling native pointers

## Children Identified

> Deeper concepts spawned during SPAWNING phase

| Child | Hypothesis | Status |
|-------|------------|--------|
| native-library-loading | System.loadLibrary calls, error handling | PENDING |
| endpoint-creation | Endpoint instantiation, libCreate | PENDING |
| thread-registration | libRegisterThread for worker and UI threads | PENDING |
| endpoint-configuration | EpConfig settings: log, media, UA, STUN | PENDING |

## Dependencies

- **Uses**: pjsip-jni-integration
- **Used by**: transport-management, codec-configuration

## Key Insights

1. **Thread registration is mandatory**: Every thread using PJSIP must call libRegisterThread first
2. **Dual-thread architecture**: Worker thread (HandlerThread) + UI thread both registered
3. **Native library dependency order**: OpenH264 before pjsua2
4. **Object trash pattern**: Critical for JNI memory management
5. **Conservative audio settings**: 8kHz, mono, no VAD for maximum compatibility

## ADR Candidates

- **ADR: 8kHz Audio Sample Rate** - Narrowband vs wideband audio decision
- **ADR: SPEEX Echo Cancellation** - Choice of echo canceller
- **ADR: No VAD** - Always transmit audio, even during silence

## Flow Recommendation

- **Type**: SDD
- **Confidence**: high
- **Rationale**: Internal initialization logic, no stakeholder docs needed

## Synthesis

> Updated during SYNTHESIZING phase after children complete

### From Children
Children are implementation details already captured in validated understanding.

### Combined Understanding
Endpoint initialization is a well-defined sequence:
1. Load native libraries (openh264 → pjsua2)
2. Create endpoint and register current thread
3. Register UI thread via Handler
4. Configure EpConfig (log, media, UA, STUN)
5. Create transports (UDP, TCP)
6. Start library

Critical patterns:
- Thread registration mandatory for PJSIP access
- Object trash prevents JNI GC issues
- Media config: 8kHz, mono, SPEEX EC, no VAD

## Bubble Up

> Summary to pass to parent during EXITING

- Native libraries loaded in dependency order (openh264 → pjsua2)
- Thread registration mandatory for all PJSIP-using threads (worker + UI)
- Media config optimized for voice: 8kHz, mono, SPEEX EC, no VAD
- Object trash pattern prevents JNI GC issues
- TLS transport commented out (UDP/TCP only)

---

*Phase: EXITING | Depth: 2 | Parent: sip-service-lifecycle*
