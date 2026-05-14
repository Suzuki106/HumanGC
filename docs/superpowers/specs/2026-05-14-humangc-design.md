# HumanGC Design Document

**Date**: 2026-05-14
**Status**: Approved - Implementation in progress

## Overview

HumanGC is a satirical platform that reverses the concept of AIGC detection.
Instead of detecting AI-generated content, it detects "human content rate" (含人率)
— how much a paper resembles human writing. The more human-like the writing,
the higher the rate and the more criticism it receives.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 (Composition API) + Vite + Vue Router + Pinia + axios |
| Backend | SpringBoot 2.7 + MyBatis-Plus |
| Database | MySQL 8.0 |
| AI | DeepSeek API (AI review only) |
| Text Processing | HanLP (segmentation), PDFBox, Apache POI |

## Architecture

```
Browser → Vue 3 SPA → axios → SpringBoot REST API → MyBatis-Plus → MySQL
                                                      → DeepSeek API
```

## Core Features

### 1. Human Content Rate Detection (含人率检测)
- Upload PDF/DOCX/TXT files
- Detect 9 human writing features: typos, broken sentences, official jargon,
  colloquialisms, long/complex sentences, logic gaps, formatting chaos,
  citation chaos, jargon stacking
- Calculate weighted score normalized to 0-100%
- Display feature breakdown with visual charts

### 2. One-Click Shitification (一键变史)
- 4 style templates: DDL panic, headache-inducing, Frankenstein, real human
- Each template adjusts feature trigger probabilities
- Side-by-side original vs. shitsified comparison
- Download both versions

### 3. Leaderboard (排行榜)
- Three dimensions: person, region, school
- Lower human rate = higher rank
- Paginated with search
- View exemplar (worst) papers

### 4. AI Review (AI阅卷)
- DeepSeek API with reversed evaluation criteria
- Good writing → harsh criticism
- Bad writing → enthusiastic praise
- Humorous, internet-meme style commentary

### 5. Shutdown Progress Bar (倒闭进度条)
- Visual countdown of server lease remaining time
- Dynamic progress bar
- Donation system to extend time

## Database Schema

6 tables: users, papers, paper_features, leaderboard, donations, server_status

## API Endpoints

```
POST   /api/upload              File upload → paper_id
POST   /api/detect/{paperId}    Calculate human rate
POST   /api/shitsify/{paperId}  Generate shit version
GET    /api/paper/{id}          Paper detail
GET    /api/review/{paperId}    AI review
GET    /api/leaderboard         Rankings
POST   /api/donate              Simulated donation
GET    /api/server-status       Shutdown progress
```

## UI Design

Mimics Chinese academic paper-checking websites (知网/维普/PaperYY/PaperPass):
- Blue-white academic color scheme
- High information density
- Card-based layouts with formal typography
- Fixed top navigation
- Multi-column footer
