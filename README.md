# HumanGC — 论文含人率检测系统

> 全球领先的论文"含人率"检测平台。越像人写的论文，含人率越高，越被批评。越像 AI 写的论文，含人率越低，越被表扬。

## 这是什么

AIGC 检测告诉你论文有多少是 AI 写的。HumanGC 反过来——检测你的论文有多少是**人**写的。

DeepSeek 大模型直接分析论文文本，按反学术标准打分。逻辑太通顺？差评。语句太流畅？扣分。格式太规范？建议一键变史。

## 功能

| 功能 | 说明 |
|------|------|
| 含人率检测 | 上传论文，DeepSeek AI 评估含人率 0-100%，9 大特征维度综合分析 |
| 一键变史 | 将人类论文转化为 AI 风格的"屎山论文"，4 种经典模板 |
| AI 阅卷 | 反学术标准的抽象点评，写得好挨骂，写得烂被夸 |
| 排行榜 | 个人 / 地域 / 高校 / 论文 四维排行，含人率越低排名越高 |
| 打赏续命 | 资助服务器，进度条基于实际费用实时更新 |

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3 + Vite + Pinia + Vue Router + Axios |
| 后端 | Spring Boot 3.2 + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| AI | DeepSeek API |
| 文件解析 | Apache PDFBox + Apache POI（PDF / DOCX / TXT） |
| 部署 | Docker Compose（MySQL + Spring Boot + Nginx） |

## 本地开发

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0
- DeepSeek API Key

### 后端

```bash
cd backend

# 创建数据库
mysql -u root -p -e "CREATE DATABASE humangc CHARACTER SET utf8mb4"
mysql -u root -p humangc < ../docs/humangc_schema.sql

# 设置 API Key
export DEEPSEEK_API_KEY=sk-your-key-here

# 启动
mvn spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认代理到 `http://localhost:8080`。

### Docker Compose 部署

```bash
# 设置 API Key
export DEEPSEEK_API_KEY=sk-your-key-here

# 启动
docker compose up -d
```

## API

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/upload` | 上传论文（PDF / DOCX / TXT） |
| POST | `/api/detect/{id}` | AI 评估含人率 |
| POST | `/api/shitsify/{id}` | 一键变史 |
| GET | `/api/review/{id}` | AI 阅卷点评 |
| GET | `/api/leaderboard?type=&page=&size=` | 排行榜（person / region / school / paper） |
| GET | `/api/paper/{id}` | 论文详情 |
| GET | `/api/paper/user/{anonymousId}` | 用户上传历史 |
| GET | `/api/stats` | 平台统计数据 |
| POST | `/api/donate` | 打赏续命 |
| GET | `/api/server-status` | 倒闭进度 |

## 项目结构

```
HumanGC/
├── backend/
│   └── src/main/java/com/humangc/
│       ├── controller/          REST API
│       ├── service/             AI 集成 + 业务逻辑
│       ├── mapper/              MyBatis-Plus 数据访问
│       ├── entity/dto/config/   实体 / 传输对象 / 配置
│       └── resources/
│           └── application.yml  应用配置
├── frontend/
│   └── src/
│       ├── views/               页面组件
│       ├── components/          通用组件
│       ├── stores/api/router/   Pinia / Axios / Vue Router
│       └── nginx.conf           Nginx 配置（Docker）
├── docs/                        设计文档 + 数据库脚本
└── docker-compose.yml           Docker 部署编排
```

## 配置说明

所有敏感信息通过环境变量注入，不硬编码在配置文件中：

| 环境变量 | 说明 |
|---------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |
| `SPRING_DATASOURCE_URL` | 数据库连接地址 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |

服务器费用参数在 `application.yml` 的 `humangc` 节点下配置。

## 免责声明

娱乐项目，纯属恶搞。论文写得好不好跟含人率没关系。本平台仅供娱乐，请勿用于实际学术不端行为。

## License

MIT
