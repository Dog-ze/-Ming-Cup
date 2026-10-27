# 一盏茗

一盏茗是一个面向日常饮茶场景的茶文化应用。用户可根据天气、饮茶偏好与问卷信息获取茶叶推荐，记录品茶体验，并使用 AI 功能获取泡茶建议或进行问茶咨询。

项目包含用户端、后台管理端与 Spring Boot 后端，适合作为全栈课程设计或练习项目。

## 功能概览

- 根据天气、城市和用户偏好推荐茶叶
- 茶叶知识库、茶食搭配与附近店铺推荐
- 茶记记录与每日积分奖励
- AI 泡法：生成泡茶方式与注意事项
- AI 问茶：以流式对话形式推荐合适的茶
- 积分中心：每日领取积分、查看积分流水、AI 功能扣分
- 后台管理：用户、积分、茶叶知识、茶食规则、店铺、操作日志等管理功能

## 项目结构

```text
.
├─ backend/       Spring Boot 后端服务
├─ frontend/      uni-app 用户端
├─ admin-web/     Vue 管理后台
```

## 技术栈

- 后端：Java 17、Spring Boot、MyBatis-Plus、MySQL、Flyway、JWT
- 用户端：Vue 3、uni-app、Pinia、uView Plus
- 管理后台：Vue 3、Vite、Element Plus、ECharts、Pinia
- AI：DeepSeek Chat API
- 外部服务：高德地图 Web 服务 API

## 本地运行

### 1. 准备环境

- JDK 17
- MySQL 8 或兼容版本
- Node.js 与 npm
- DeepSeek API Key（使用 AI 功能时需要）
- 高德地图 Web 服务 Key（使用天气、地理位置或店铺功能时需要）

### 2. 配置后端环境变量

后端通用配置在 `backend/src/main/resources/application.yml`，本地配置模板在：

```text
backend/src/main/resources/application-local.example.yml
```

请通过 IDE 的运行配置或系统环境变量设置以下变量，不能将真实值提交到 Git：

| 变量名 | 说明 |
| --- | --- |
| `DB_URL` | MySQL 连接地址 |
| `DB_USERNAME` | 数据库账号 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | 用户端 JWT 密钥 |
| `ADMIN_JWT_SECRET` | 管理端 JWT 密钥 |
| `AMAP_WEB_SERVICE_KEY` | 高德地图 Web 服务 Key |
| `DEEPSEEK_BASE_URL` | DeepSeek 服务地址，通常为 `https://api.deepseek.com` |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `DEEPSEEK_CHAT_MODEL` | DeepSeek 模型名称 |
| `FLYWAY_ENABLED` | 是否执行数据库迁移；首次初始化或发布时设为 `true` |

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell 可使用：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 4. 启动用户端

```bash
cd frontend
npm install
npm run dev:h5
```

用户端也可以使用 HBuilderX 运行到 App、小程序或其他 uni-app 支持的平台。

### 5. 启动管理后台

```bash
cd admin-web
npm install
npm run dev
```

管理后台的开发地址以终端输出为准。

## 测试与构建

```bash
# 后端测试
cd backend
.\mvnw.cmd test

# 管理后台测试与构建
cd admin-web
npm test
npm run build

# 用户端 H5 构建
cd frontend
npm run build:h5
```

## 安全说明

- 不要提交 `application-local.yml`、`.env` 文件或任何真实密钥。
- 不要提交 `node_modules`、`target`、`dist`、`uploads` 和 IDE 本地配置。
- 如密钥曾公开出现，请在对应平台重新生成并废弃旧密钥。

## 开源说明

本项目采用 [MIT License](LICENSE) 开源，版权所有者为 Dog-ze。

你可以自由使用、复制、修改、发布和分发本项目，也可以将其用于商业项目；但在发布包含本项目代码的作品时，必须保留原始版权声明与 MIT 许可证文本。

本项目按“现状”提供，不附带任何明示或默示担保。使用、修改或部署本项目所产生的风险和责任由使用者自行承担。
