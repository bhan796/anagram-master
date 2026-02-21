# Server

Authoritative multiplayer backend scaffold for Anagram Master.

## Stack

- Node.js + TypeScript
- Express + Socket.IO
- Prisma (Postgres)
- Redis (ioredis)
- Vitest + ESLint + Prettier

## Setup

1. `npm install`
2. `Copy-Item .env.example .env`
3. Ensure Postgres + Redis are running locally
4. `npm run prisma:generate`
5. `npm run prisma:migrate -- --name init`
6. `npm run dev`

## Scripts

- `npm run dev`
- `npm run build`
- `npm run start`
- `npm run test`
- `npm run lint`
- `npm run format`
- `npm run prisma:generate`
- `npm run prisma:migrate`
- `npm run prisma:seed`

## Scope in Phase 0

- Runtime scaffold, env validation, socket server bootstrap, and health endpoint.
- Matchmaking/state machine/game rules will be implemented in later phases.