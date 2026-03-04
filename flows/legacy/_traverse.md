# Traversal State

> Persistent recursion stack for tree traversal. AI reads this to know where it is and what to do next.

## Existing Flows Index

| Flow Path | Type | Topics | Key Decisions |
|-----------|------|--------|---------------|
| *None - First run* | - | - | - |

## Mode

- **BFS** (no comment): Breadth-first, analyze all domains systematically
- **DFS** (with comment): Depth-first, focus deeply on specific topic

## Source Path

project root (/Users/anton/proj/telon/g/sip2-android-core)

## Focus (DFS only)

[none]

## Algorithm

```
RECURSIVE-UNDERSTAND(node):
    1. ENTER: Push node to stack, set phase = ENTERING
    2. EXPLORE: Read code, form understanding, set phase = EXPLORING
    3. SPAWN: Identify children (deeper concepts), set phase = SPAWNING
    4. RECURSE: For each child -> RECURSIVE-UNDERSTAND(child)
    5. SYNTHESIZE: Combine children insights, set phase = SYNTHESIZING
    6. EXIT: Pop from stack, bubble up summary, set phase = EXITING
```

## Current Stack

> Read top-to-bottom = root-to-current. Last item = where AI is now.

```
/ (root)                           SPAWNING
└── event-broadcasting             ENTERING
```

## Stack Operations Log

| # | Operation | Node | Phase | Result |
|---|-----------|------|-------|--------|
| 1 | PUSH | / (root) | ENTERING | Stack initialized |
| 2 | UPDATE | / (root) | EXPLORING→SPAWNING | Created _root.md with 8 domains |
| 3 | RECURSE | sip-service-lifecycle | ENTERING→EXITING | Created SDD |
| 4 | RECURSE | account-management | ENTERING→EXITING | Created SDD |
| 5 | RECURSE | call-management | ENTERING→EXITING | Created SDD |
| 6 | PAUSED | - | - | Session pause for user review |
| 7 | RESUME | / (root) | SPAWNING | Continuing BFS traversal |
| 8 | RECURSE | event-broadcasting | ENTERING | Pushed to stack, creating _node.md |

## Current Position

- **Node**: event-broadcasting
- **Phase**: ENTERING
- **Depth**: 1
- **Path**: /event-broadcasting

## Pending Children

> Children identified but not yet explored (LIFO - last added explored first)

```
event-broadcasting (current)
dto-layer
video-management
pjsip-jni-integration
audio-routing
```

## Visited Nodes

> Completed nodes with their summaries

| Node Path | Summary | Flow Created |
|-----------|---------|--------------|
| sip-service-lifecycle | Service-based PJSIP lifecycle, HandlerThread isolation, UDP/TCP transports, voice-optimized media (8kHz, SPEEX EC, no VAD) | flows/sdd-sip-service-lifecycle/ (01-requirements.md, 02-specifications.md) |
| account-management | Account CRUD, Digest auth, transport selection (TCP default), video pre-config (270°), callbacks | flows/sdd-account-management/ (01-requirements.md, 02-specifications.md) |
| call-management | Call lifecycle, hold/unhold, mute/unmute, redirect (302), media auto-connect with 1.5x gain | flows/sdd-call-management/ (01-requirements.md) |

## Visited Nodes

> Completed nodes with their summaries

| Node Path | Summary | Flow Created |
|-----------|---------|--------------|
| endpoint-initialization | Native lib loading, endpoint creation, thread registration, EpConfig | - |
| sip-service-lifecycle | Service-based PJSIP lifecycle, HandlerThread isolation, UDP/TCP transports, voice-optimized media config | flows/sdd-sip-service-lifecycle/ (01-requirements.md, 02-specifications.md) |

## Visited Nodes

> Completed nodes with their summaries

| Node Path | Summary | Flow Created |
|-----------|---------|--------------|
| - | - | - |

## Next Action

```
1. Create understanding/_root.md with project overview
2. Identify top-level logical domains
3. Transition to EXPLORING phase
```

---

## Phase Definitions

### ENTERING
- Just arrived at this node
- Create _node.md file
- Read relevant source files
- Form initial hypothesis

### EXPLORING
- Deep analysis of this node's scope
- Validate/refine hypothesis
- Identify what belongs here vs. children

### SPAWNING
- Identify child concepts that need deeper exploration
- Add children to Pending stack
- Children are LOGICAL concepts, not filesystem paths

### SYNTHESIZING
- All children completed (or no children)
- Combine insights from children
- Update this node's _node.md with full understanding

### EXITING
- Pop from stack
- Bubble up summary to parent
- Mark as visited

---

## Resume Protocol

When `/legacy` starts:
1. Read _traverse.md
2. Find current position (top of stack)
3. Check phase
4. Continue from that phase

If interrupted mid-phase:
- Re-enter same phase (idempotent operations)

---

*Updated by /legacy recursive traversal*
