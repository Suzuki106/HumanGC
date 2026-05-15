# HumanGC — 论文含人率检测系统

> 全球领先的论文"含人率"检测平台。越像人写的论文，含人率越高，越被批评。越像屎的论文，含人率越低，越被表扬。

## 这是什么

AIGC检测告诉你论文有多少是AI写的。HumanGC反过来——检测你的论文有多少是**人**写的。

DeepSeek AI直接分析论文文本，按反学术标准打分。逻辑太通顺？差评。语句太流畅？扣分。格式太规范？建议一键变史。

## 功能

| 功能 | 说明 |
|------|------|
| **含人率检测** | 上传论文，DeepSeek AI直接评估含人率0-100%，输出中文总结 |
| **一键变史** | 不选模式，一个按钮直接毁成答辩。谷歌翻译模拟+搬史缝合+断章取义+逆天改写 |
| **AI阅卷** | 反学术标准的抽象点评，2026中文互联网冲浪腔——写得好挨骂，写得烂被夸 |
| **排行榜** | 个人/地域/高校三维排行，含人率越低排名越高 |

## 技术栈

- **前后端**: Vue 3 + Vite + SpringBoot 3，单端口8080同源部署
- **数据库**: MySQL 8.0 + MyBatis-Plus
- **AI**: DeepSeek API（含人率评估 + 阅卷点评 + 一键变史）
- **文件解析**: Apache PDFBox + Apache POI（支持PDF/DOCX/TXT）

## 快速启动

### 环境要求
- JDK 21+
- Node.js 18+
- MySQL 8.0
- DeepSeek API Key

### 启动

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE humangc CHARACTER SET utf8mb4"

# 2. 配置 application.yml 中的数据库连接信息

# 3. 构建前端
cd frontend && npm install && npm run build

# 4. 部署前端到后端 static 目录
cp -r dist/* ../backend/src/main/resources/static/

# 5. 启动后端
cd ../backend
export DEEPSEEK_API_KEY=your_api_key
mvn spring-boot:run
```

打开 `http://localhost:8080`

## 项目结构

```
HumanGC/
├── backend/                          SpringBoot Maven 项目
│   └── src/main/java/com/humangc/
│       ├── controller/               REST API + SPA路由
│       ├── service/                  AI集成（含人率/阅卷/变史）
│       ├── mapper/                   MyBatis-Plus 数据访问
│       ├── entity/dto/config/        实体/传输对象/配置
│       └── resources/
│           ├── application.yml       应用配置
│           └── static/               前端构建产物
├── frontend/                         Vue 3 + Vite 项目
│   └── src/
│       ├── views/                    页面（首页/检测/变史/排行榜/阅卷/打赏/详情）
│       ├── components/               组件（上传/仪表盘/导航/倒闭进度条）
│       ├── stores/api/router/        状态管理/API/路由
└── docs/                             设计文档
```

## API

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/upload` | 上传论文（PDF/DOCX/TXT） |
| POST | `/api/detect/{id}` | AI评估含人率 |
| POST | `/api/shitsify/{id}` | 一键变史 |
| GET | `/api/review/{id}` | AI阅卷点评 |
| GET | `/api/leaderboard?type=&page=&size=` | 排行榜 |
| POST | `/api/donate` | 打赏续命 |
| GET | `/api/server-status` | 倒闭进度 |

## 免责声明

娱乐项目，纯属恶搞。论文写得好不好跟含人率没关系。

## License

MIT
