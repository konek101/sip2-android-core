# Code to Flow Mapping

## Overview

Maps analyzed code modules to generated flows.

## Flow Type Detection Rules

| Indicator | Flow Type |
|-----------|-----------|
| `*.test.*`, `*.spec.*`, `__tests__/` | TDD |
| `components/`, `*.tsx`, `*.vue`, `templates/` | VDD |
| `README.md`, public exports, API docs | DDD |
| Internal logic, no UI, no public API | SDD |

## Mapping Table

| Code Path | Flow | Type | Action | Status | Notes |
|-----------|------|------|--------|--------|-------|
| PjSipService.java | flows/sdd-sip-service-lifecycle/ | SDD | CREATED | DRAFT | Service lifecycle, PJSIP init |
| PjSipAccount.java | flows/sdd-account-management/ | SDD | CREATED | DRAFT | Account CRUD, registration |
| PjSipCall.java | flows/sdd-call-management/ | SDD | CREATED | DRAFT | Call operations, media |
| PjSipBroadcastEmiter.java | - | SDD | PENDING | - | Event broadcasting |
| dto/*.java | - | SDD | PENDING | - | Data transfer objects |
| PjSipVideo*.java | - | SDD | PENDING | - | Video handling |
| PjSipUtils.java | - | SDD | PENDING | - | Utility functions |
| pjsip/pjsua2/*.java | - | ADR | PENDING | - | JNI wrappers (auto-generated) |

### Action Values
- **CREATED** - New flow created
- **UPDATED** - Existing flow appended to (additive changes only)
- **UNCHANGED** - Flow exists, no new information found
- **CONFLICT** - Analysis contradicts existing documentation (needs reconciliation)
- **PENDING** - Not yet analyzed

## ADR Mapping

| Code Pattern | ADR | Type | Status |
|--------------|-----|------|--------|
| PJSIP library usage | ADR: PJSIP as SIP Stack | constraining | PENDING |
| Service-based architecture | ADR: Service Architecture | enabling | PENDING |
| HandlerThread isolation | ADR: Threading Model | enabling | PENDING |
| Broadcast event pattern | ADR: Broadcast Events | enabling | PENDING |
| 8kHz audio sample rate | ADR: Narrowband Audio | constraining | PENDING |
| SPEEX echo cancellation | ADR: SPEEX EC | constraining | PENDING |
| TCP default transport | ADR: TCP Default | constraining | PENDING |
| Video orientation 270° | ADR: Video Rotation | constraining | PENDING |

## Unmapped (needs manual review)

| Code Path | Reason |
|-----------|--------|
| pjsip/pjsua2/*.java (~200 files) | Auto-generated JNI wrappers - document as reference only |
| jniLibs/*.so | Native binaries - out of scope |

---

*Auto-generated. Update as analysis progresses.*
