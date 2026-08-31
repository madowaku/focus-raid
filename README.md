# Focus Raid

Focus Raid is a focus timer where your real-world concentration powers a shared fantasy world.

While you focus, your companion explores. Your sessions contribute to personal loop-boss progress, world exploration, the shared armory, and scheduled world raids.

## Product principles

- Start focusing within five seconds.
- Focus time is never rolled back.
- Your companion grows from time spent together, not paid power.
- World systems reward cooperation, not rankings.
- During a focus session, the app becomes quiet.
- The world can be playful; the timer must remain useful.

## MVP vertical slice

1. Home screen with 25-minute quick start.
2. One persistent companion.
3. Tower / Abyss expedition choice.
4. Local focus timer.
5. Session result: personal boss damage + discovery + world contribution.
6. Mock shared armory and next raid card.
7. Architecture boundaries ready for Firebase Auth, Firestore, and Cloud Run.

## Stack

- React
- TypeScript
- Vite
- Vitest
- PWA-ready web app
- Firebase integration planned behind service interfaces

## Development

```bash
npm install
npm run dev
```

Run checks:

```bash
npm run typecheck
npm test
npm run build
```

## Status

Initial MVP implementation is in progress.
