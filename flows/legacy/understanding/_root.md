# Understanding: Project Root

> Entry point for recursive understanding. Children are top-level logical domains.

## Phase: SPAWNING

## Project Overview

**SIP2 Android Core** - Universal Android SIP library based on PJSIP that provides core SIP functionality for React Native, Flutter, and native Android applications.

**Key Characteristics:**
- Android library (AAR output)
- JNI wrappers around PJSIP native library (libpjsua2.so, libopenh264.so)
- Service-based architecture with broadcast events
- Multi-platform integration support (React Native, Flutter, Native Android)
- Background service support with wake lock management

**Architecture Pattern:** Android Service + Broadcast Events
- `PjSipService` extends Android `Service`
- All operations triggered via `Intent` actions
- Results broadcasted via `PjSipBroadcastEmiter`
- Thread-safe with dedicated `HandlerThread` for SIP operations

## Validated Understanding

After analyzing core source files:

1. **PjSipService** (1240 lines) - Central service managing:
   - PJSIP endpoint initialization and lifecycle
   - Transport management (UDP, TCP, TLS)
   - Account and call collections
   - Intent-based action handling
   - Broadcast event emission
   - Audio routing (speaker/earpiece)
   - Wake lock and WiFi lock management

2. **PjSipAccount** - Account lifecycle:
   - Extends PJSIP `Account` class
   - Registration state tracking
   - Incoming call and message handling
   - JSON serialization for state

3. **PjSipCall** - Call management:
   - Extends PJSIP `Call` class
   - Hold/unhold, mute/unmute operations
   - Audio/video media handling
   - Call state machine callbacks
   - JSON serialization for state

4. **PjSipBroadcastEmiter** - Event system:
   - Broadcasts all SIP events via Android Intents
   - Events: started, account created/changed, call received/changed/terminated, message received
   - Callback correlation via `callback_id`

5. **DTO Layer** - Configuration objects:
   - `AccountConfigurationDTO` - Account settings
   - `CallSettingsDTO` - Call parameters
   - `ServiceConfigurationDTO` - Service settings
   - `SipMessageDTO` - SIP message data

6. **PJSIP JNI Wrappers** (`org.pjsip.pjsua2.*`):
   - ~200 auto-generated JNI wrapper classes
   - Direct bindings to PJSIP native library
   - Constants, enums, and data structures

## Children Identified

> Deeper concepts spawned during SPAWNING phase

| Child | Hypothesis | Priority | Status |
|-------|------------|----------|--------|
| sip-service-lifecycle | Service initialization, PJSIP stack setup, transport creation | HIGH | PENDING |
| account-management | Account CRUD, registration state machine | HIGH | PENDING |
| call-management | Call lifecycle, media handling, hold/mute operations | HIGH | PENDING |
| event-broadcasting | Event emission patterns, callback correlation | HIGH | PENDING |
| dto-layer | Configuration DTOs, JSON serialization | MEDIUM | PENDING |
| video-management | Video rendering, preview, window handling | MEDIUM | PENDING |
| pjsip-jni-integration | JNI wrapper usage, native library loading | HIGH | PENDING |
| audio-routing | Speaker/earpiece switching, audio media routing | MEDIUM | PENDING |

## Flow Recommendations

Based on analysis:

| Domain | Flow Type | Rationale |
|--------|-----------|-----------|
| sip-service-lifecycle | SDD | Internal service logic, no stakeholder docs needed |
| account-management | SDD | Core business logic, internal service |
| call-management | SDD + TDD | Core logic + correctness-critical (call state) |
| event-broadcasting | SDD | Internal event system |
| dto-layer | SDD | Simple data structures |
| video-management | SDD | Media handling logic |
| pjsip-jni-integration | ADR | Architectural decision to use PJSIP |
| audio-routing | SDD | Audio device management |

## Synthesis

> Updated after all children complete

[pending children completion]

---

*Phase: SPAWNING | Depth: 0 | Created by /legacy*
