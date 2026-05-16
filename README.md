# HumanGC — 论文含人率检测系统

> 全球领先的论文"含人率"检测平台。越像人写的论文，含人率越高，越被批评。越像 AI 写的论文，含人率越低，越被表扬。

## 这是什么

AIGC 检测告诉你论文有多少是 AI 写的。HumanGC 反过来——检测你的论文有多少是**人**写的。

混合检测方案：**7 维度统计算法**（基于 AIGC 检测研究逆向设计）+ **DeepSeek AI 定性评估**，按反学术标准打分。逻辑太通顺？差评。语句太流畅？扣分。格式太规范？建议一键变史。

## 检测原理

### 统计层（权重 40%）

基于 Fast-DetectGPT、GLTR、45-Feature AIGT Detection 等研究的逆向设计，使用 Jieba 中文分词，计算 7 个维度：

| 维度 | 权重 | 说明 |
|------|------|------|
| 句子突发性 | 22% | 句长变异系数（CV），AI 文本句长均匀，人类参差不齐 |
| 词组多样性 | 16% | 词级 Bigram TTR，抓模板结构重复 |
| 词汇多样性 | 22% | Type-Token Ratio，AI 重复用词 |
| 平均句长 | 12% | 中文文本复杂度代理指标 |
| 标点多样性 | 10% | 人类使用更丰富的标点组合 |
| 句长波动 | 10% | 极差比 + 离群句比例 |
| 罕用词比例 | 8% | 只出现一次的词占比 |

### AI 层（权重 60%）

DeepSeek 大模型按反学术标准定性评分：逻辑严密扣分、语言通顺扣分、格式规范扣分。逻辑混乱、语句不通、格式随意则加分。

### 最终含人率 = 统计分 × 0.4 + AI 分 × 0.6

检测结果附带雷达图展示 7 维度得分。

## 功能

| 功能 | 说明 |
|------|------|
| 含人率检测 | 上传论文或粘贴文字，混合算法评估含人率 0-100%，雷达图可视化 |
| 一键变史 | 将人类论文转化为 AI 风格的"屎山论文" |
| AI 阅卷 | 反学术标准的锐评，写得好挨骂，写得烂被夸 |
| 隐私控制 | 上传后可选择公开或私密，私密论文原文对其他用户隐藏 |
| 排行榜 | 个人 / 地域 / 高校 / 论文 四维排行 |
| 打赏续命 | 基于实际云服务费用的进度条（¥4.87/天） |

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3 + Vite + Pinia + Vue Router + Axios + ECharts |
| 后端 | Spring Boot 3.2 + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| AI | DeepSeek API |
| 中文分词 | Jieba (jieba-analysis) |
| 文件解析 | Apache PDFBox + Apache POI（PDF / DOCX / TXT） |
| 部署 | Docker Compose（MySQL + Spring Boot + Nginx） |

## 本地开发

### 环境要求

- JDK 17+
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
export DEEPSEEK_API_KEY=sk-your-key-here
docker compose up -d
```

## 项目结构

```
HumanGC/
├── backend/
│   └── src/main/java/com/humangc/
│       ├── controller/          REST API
│       ├── service/             AI 集成 + 统计算法
│       ├── mapper/              MyBatis-Plus 数据访问
│       ├── entity/dto/config/   实体 / 传输对象 / 配置
│       └── resources/
│           └── application.yml  应用配置
├── frontend/
│   └── src/
│       ├── views/               页面组件
│       ├── components/          通用组件（含雷达图）
│       ├── stores/api/router/   Pinia / Axios / Vue Router
│       └── nginx.conf           Nginx 配置（Docker）
├── docs/                        数据库脚本
└── docker-compose.yml           Docker 部署编排
```

## 配置说明

| 环境变量 | 说明 |
|---------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |
| `SPRING_DATASOURCE_URL` | 数据库连接地址 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |

服务器费用参数在 `application.yml` 的 `humangc` 节点下配置。

## 免责声明

本平台检测结果由 AI 模型生成，仅供娱乐参考，不构成任何学术评价或鉴定依据。请勿用于实际学术不端行为。

## License

MIT
