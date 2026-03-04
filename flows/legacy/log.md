# Legacy Analysis Log

## Session History

### 2026-03-04 - Initial BFS Traversal

**Mode**: BFS
**Target**: project root (sip2-android-core)

**Analyzed**:
- **PjSipService.java** (1240 lines): Main SIP service, HandlerThread isolation, PJSIP initialization, UDP/TCP transports, voice-optimized media config
- **PjSipAccount.java**: Account lifecycle, Digest auth, registration callbacks, incoming call/message handling
- **PjSipCall.java** (370 lines): Call operations (hold/mute/redirect), media state callbacks, audio gain adjustment
- **PjSipBroadcastEmiter.java**: Event broadcasting pattern
- **AccountConfigurationDTO.java**: Account configuration structure
- **PjSipLogWriter.java**: PJSIP log integration
- **PJSIP JNI wrappers** (~200 classes): Auto-generated bindings

**Created**:
- **flows/sdd-sip-service-lifecycle/**: SDD for SIP service initialization and lifecycle
  - 01-requirements.md: 10 functional requirements, 4 non-functional
  - 02-specifications.md: Architecture, flows, data structures, thread model
- **flows/sdd-account-management/**: SDD for account CRUD and registration
  - 01-requirements.md: 10 functional requirements, 4 non-functional
  - 02-specifications.md: Class structure, creation flow, serialization
- **flows/sdd-call-management/**: SDD for call lifecycle
  - 01-requirements.md: 12 functional requirements, 4 non-functional

**Understanding Tree**:
- / (root) - Project overview, 8 domains identified
- /sip-service-lifecycle - Service architecture, PJSIP lifecycle ✓
- /sip-service-lifecycle/endpoint-initialization - Native loading, EpConfig ✓
- /account-management - Account CRUD, callbacks ✓
- /call-management - Call operations, media handling ✓

**Key Findings**:
- Service-based architecture with HandlerThread for SIP operations
- Broadcast event pattern for cross-process communication
- Voice-optimized: 8kHz, SPEEX EC, no VAD, 1.5x audio gain
- TCP default transport (more reliable for mobile)
- Video pre-configured: auto-show, auto-transmit, 270° rotation
- Object trash pattern for JNI memory safety

**Next depth**:
- event-broadcasting: PjSipBroadcastEmiter, event types, callback correlation
- dto-layer: All DTO classes, JSON serialization
- video-management: VideoWindow, VideoPreview, media listeners
- pjsip-jni-integration: JNI wrapper patterns, native library structure
- audio-routing: AudioManager, speaker/earpiece switching

---

*Append new entries at the top.*
