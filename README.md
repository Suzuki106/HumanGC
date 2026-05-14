# HumanGC — 论文含人率检测系统

> 全球领先的论文"含人率"检测平台。越像人写的论文，含人率越高，越被批评。越像屎的论文，含人率越低，越被表扬。

## 这是什么

AIGC检测告诉你论文有多少是AI写的。HumanGC反过来——检测你的论文有多少是**人**写的。

逻辑太通顺？差评。语句太流畅？扣分。格式太规范？你太像人了，建议一键变史。

## 功能

| 功能 | 说明 |
|------|------|
| **含人率检测** | 上传论文，从9个维度分析"人类痕迹"，输出0-100%含人率 |
| **一键变史** | 4种风格模板，AI驱动将论文改写成逻辑混乱的学术废品 |
| **排行榜** | 个人/地域/高校三维排行，含人率越低排名越高 |
| **AI阅卷** | 反学术标准的AI点评——写得好挨骂，写得烂被夸 |

## 技术栈

- **前端**: Vue 3 + Vite + Vue Router + Pinia
- **后端**: SpringBoot 3 + MyBatis-Plus
- **数据库**: MySQL 8.0
- **AI**: DeepSeek API（阅卷点评 + 一键变史）

## 快速启动

### 环境要求
- JDK 21+
- Node.js 18+
- MySQL 8.0

### 后端

```bash
cd backend

# 配置数据库（application.yml 中修改 datasource 连接信息）

# 创建数据库
mysql -u root -p -e "CREATE DATABASE humangc CHARACTER SET utf8mb4"

# 启动
export DEEPSEEK_API_KEY=your_api_key
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

## 项目结构

```
HumanGC/
├── backend/          SpringBoot Maven 项目
│   └── src/main/java/com/humangc/
│       ├── controller/    REST API
│       ├── service/       业务逻辑（含人率计算、AI集成）
│       ├── mapper/        MyBatis-Plus 数据访问
│       ├── entity/        数据实体
│       └── config/        配置
├── frontend/         Vue 3 + Vite 项目
│   └── src/
│       ├── views/         页面组件
│       ├── components/    通用组件
│       ├── stores/        Pinia 状态管理
│       ├── api/           API 调用
│       └── router/        路由
└── docs/             设计文档
```

## API

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/upload` | 上传论文文件（PDF/DOCX/TXT） |
| POST | `/api/detect/{id}` | 计算含人率 |
| POST | `/api/shitsify/{id}` | 一键变史 |
| GET | `/api/review/{id}` | AI阅卷点评 |
| GET | `/api/leaderboard?type=&page=&size=` | 排行榜 |
| POST | `/api/donate` | 打赏续命 |
| GET | `/api/server-status` | 倒闭进度 |

## 免责声明

这是一个娱乐项目，纯属恶搞。不要当真。你论文写得好不好跟含人率没关系。

## License

MIT
