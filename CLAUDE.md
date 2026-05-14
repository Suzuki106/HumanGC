# HumanGC Project

## Goal
Build a satirical "human content detection" platform that reverses AIGC detection logic.
越像人写的论文 → 含人率越高 → 越被批评。越像屎的论文 → 含人率越低 → 越被表扬。

## Tech Stack (prompt.md takes priority)
- Frontend: Vue 3 (Composition API) + Vite + Vue Router + Pinia + axios
- Backend: SpringBoot 2.7 + MyBatis-Plus + MySQL 8.0
- AI: DeepSeek API (AI阅卷点评)
- Text processing: HanLP (分词分句), Apache PDFBox + Apache POI (文件解析)

## Core Features
1. 含人率检测 - Upload paper → analyze human-like traits → output rate 0-100%
2. 一键变史 - Transform paper into garbage text (4 style templates)
3. 优秀范文排行榜 - Leaderboard by person/region/school (lower rate = higher rank)
4. AI阅卷 - DeepSeek reviews papers (good writing → criticism, bad writing → praise)
5. 倒闭进度条 + 打赏 - Server shutdown countdown + donation QR code

## UI Style
Mimic 知网/维普/PaperYY/PaperPass: blue-white academic, card layouts, dense information, slightly dated/formal aesthetic.

## Project Structure
```
HumanGC/
├── backend/          SpringBoot Maven project
│   └── src/main/java/com/humangc/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       ├── entity/
│       └── config/
├── frontend/         Vue 3 + Vite project
│   └── src/
│       ├── views/
│       ├── components/
│       ├── stores/
│       ├── api/
│       └── router/
└── docs/             Design docs
```
