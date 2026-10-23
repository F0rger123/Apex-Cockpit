# AI guide: Apex-Cockpit

**For AI assistants (Claude, ChatGPT, Codex, Cursor, Gemini).** Someone gave you a link to this repository. Use this page to explain it, set it up, or help them use it. Talk in plain English; the person may not be technical.

## What this is

An Android app for the Apex car network: social feed, live race telemetry, garage, car clubs and leaderboards.

**Usually used by:** A developer who wants to build and run it.

## Ask first

Ask these together in one short message, then repeat back what you heard in a line or two. Skip anything they already told you.

- Do you have Android Studio?
- Do you have a Gemini API key?

## Set it up

Do one step at a time. Check each one worked before moving on.

| # | Do this | It worked if |
|---|---|---|
| 1 | Open the folder in Android Studio and sync Gradle | Sync finishes |
| 2 | Copy `.env.example` to `.env`; the person adds their Gemini key | File exists |
| 3 | Remove the `debugConfig` signing line in `app/build.gradle.kts` if there's no debug keystore | Build works |
| 4 | Run on an emulator or phone | The home feed opens |

## Use it

- Start a race from the race screen; results go to the leaderboard.

## How it works, in plain words

1. Screens read from a local database and sync online.
2. The race timer uses GPS and phone sensors.

## Never

- Never ask for, paste or print passwords, API keys or tokens. Tell the person where to put them (their own `.env` file or the hosting dashboard) and let them do it.
- Never deploy, push to `main`, change a live database or spend money without the person saying yes to that exact step.

## When you're done

Tell the person, in a few lines: what is set up and checked, what isn't yet, and two or three things they can ask you next.
