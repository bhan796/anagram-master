# Architecture Overview

## MVP Principles

- Server authoritative for all multiplayer scoring and timers.
- Core game rule logic isolated from UI/controller layers.
- Practice mode fully offline on iOS client.
- Reconnect support for online match continuity.

## Phases

- Phase 0: Bootstrap
- Phase 1: Shared game engine + offline practice
- Phase 2: Backend multiplayer state machine + matchmaking
- Phase 3: iOS online integration
- Phase 4: Polish + persistence