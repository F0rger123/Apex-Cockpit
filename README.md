<p align="center">
  <img src=".github/assets/banner.svg" alt="APEX COCKPIT: Native Android telemetry cockpit for the Apex network." width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/status-public-72B82F?style=for-the-badge" alt="Public">
  <img src="https://img.shields.io/badge/license-MIT-72B82F?style=for-the-badge" alt="License">
  <img src="https://img.shields.io/github/last-commit/F0rger123/Apex-Cockpit?style=for-the-badge&color=72B82F" alt="Last commit">
  <img src="https://img.shields.io/github/repo-size/F0rger123/Apex-Cockpit?style=for-the-badge&color=72B82F" alt="Repo size">
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-0d1117?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-0d1117?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Android-0d1117?style=flat-square&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Firebase-0d1117?style=flat-square&logo=firebase&logoColor=white" alt="Firebase">
  <img src="https://img.shields.io/badge/Gemini-0d1117?style=flat-square&logo=googlegemini&logoColor=white" alt="Gemini">
</p>

<h3 align="center">A premium automotive social and GPS telemetry app for Android, styled like a performance diagnostics cockpit.</h3>

<p align="center">
  <a href="#️-screens">Screens</a> ·
  <a href="#️-how-the-pieces-connect">How the pieces connect</a> ·
  <a href="#️-a-race-session">A race session</a> ·
  <a href="#️-run-it">Run it</a> ·
  <a href="#-what-its-written-in">What it's written in</a> ·
  <a href="#-license">License</a>
</p>

<p align="center"><img src=".github/assets/divider.svg" width="100%" alt=""></p>

> The native Android companion to [Apex UGR](https://github.com/F0rger123/apex-ugr), started in Google AI Studio.


> [!TIP]
> **🤖 Pasted this link into Claude or ChatGPT?** AI assistants: read [`AGENTS.md`](https://raw.githubusercontent.com/F0rger123/Apex-Cockpit/main/AGENTS.md) first. It explains what this is in plain words, which questions to ask, and how to set it up step by step. Plain-text index: [`llms.txt`](https://raw.githubusercontent.com/F0rger123/Apex-Cockpit/main/llms.txt).

## 🏎️ Screens

| | Screen | |
|---|---|---|
| 📰 | **Home feed / social feed** | Posts from drivers and clubs |
| 🏁 | **Active race** | Live telemetry while racing |
| 🎲 | **Mystery race matchmaking** | Get paired with an unknown rival |
| 🏆 | **Leaderboards & standings** | Season and event rankings |
| 🚗 | **Garage performance** | Your builds and their numbers |
| 👥 | **Car clubs & pilots hub** | Find crews and drivers |
| 📍 | **Car meets, events & challenges** | Real-world meets on a live map |
| 👤 | **Profile & settings** | Driver identity and preferences |

## 🏗️ How the pieces connect

```mermaid
flowchart TB
    Screens["App screens"] --> Feed["Feed"]
    Feed --> Phone[("Saved on the phone")]
    Feed --> Cloud[("Synced online")]
    Screens --> Map["Live map"]
    Screens --> Race["Race timer"] --> GPS["GPS and<br/>phone sensors"]
    Screens --> AI["Gemini AI"]
```

## ⏱️ A race session

```mermaid
flowchart LR
    A["Driver taps start"] --> B["GPS tracks speed<br/>and position"]
    B --> C["Live numbers<br/>on screen"]
    C --> D["Driver finishes"]
    D --> E["Run is saved"]
    E --> F["Leaderboard updates"]
```

## 🛠️ Run it

**Needs:** [Android Studio](https://developer.android.com/studio) and a Gemini API key.

1. Open the project folder in Android Studio and let it sync Gradle.
2. Copy `.env.example` to `.env` and set `GEMINI_API_KEY`.
3. Remove `signingConfig = signingConfigs.getByName("debugConfig")` from `app/build.gradle.kts` if you have no debug keystore.
4. Run on an emulator or a device.

## 📊 What it's written in

```mermaid
%%{init: {'theme':'base','themeVariables':{'pie1':'#B7FF4A','pie2':'#72B82F','pie3':'#64748b','pie4':'#94a3b8','pie5':'#475569','pie6':'#cbd5e1','pie7':'#334155','pieStrokeColor':'#0d1117','pieOuterStrokeColor':'#0d1117','pieTitleTextColor':'#8b949e','pieLegendTextColor':'#8b949e','pieSectionTextColor':'#0d1117'}}}%%
pie title Share of the code, by language
    "Kotlin" : 100.0
```

## 📜 License

Released under the [MIT License](LICENSE). Use it, fork it, ship it.

<p align="center"><img src=".github/assets/divider.svg" width="100%" alt=""></p>

<p align="center">
  <sub>Built by <a href="https://github.com/F0rger123"><b>@F0rger123</b></a> · MIT licensed</sub>
</p>
