# Anagram Arena Web App

Browser client that mirrors the Android app experience and connects to the same Socket.IO backend.

## Stack

- React + TypeScript + Vite
- Socket.IO client
- Android-matching neon arcade theme tokens/components

## Run

1. From repo root: `npm install`
2. Start backend: `npm run server:dev`
3. Start web app: `npm run web:dev`
4. Open the Vite URL shown in terminal.

## Environment

Set `VITE_SERVER_URL` if backend is not local:

- PowerShell: `$env:VITE_SERVER_URL="https://anagram-server-production.up.railway.app"`
- Then run `npm run web:dev`

Or create `web-app/.env.local`:

```env
VITE_SERVER_URL=https://anagram-server-production.up.railway.app
```

## Included Flows

- Home / Practice / Profile / Settings
- Offline letters practice
- Offline conundrum practice
- Online matchmaking + full 5-round match flow
- Match history and basic stats via backend profile APIs
