# 面向银发经济的养老资源空间适配评估与智慧决策 WebGIS 平台

河海大学 地理与遥感学院 · 大学生创新创业训练计划项目

## 技术架构（B/S 四层）
- 前端：Vue 3 + Vite + OpenLayers + ECharts
- Java 后端：Spring Boot（RBAC / 机构 SaaS / AHP 多因子匹配）
- AI 后端：Python FastAPI（LangChain + pgvector RAG + AI 伴诊）
- GIS 服务：GeoServer + pgRouting
- 数据库：PostgreSQL + PostGIS + pgvector

## 目录说明
| 目录 | 内容 |
|---|---|
| frontend | 前端工程 |
| backend-java | Java 业务后端 |
| backend-ai | Python AI 后端 |
| sql | 建表与初始化脚本 |
| documents | 设计文档 |
| data | 原始 GIS 数据（本地，不入库）|

## 本地启动
- 前端：`cd frontend && npm run dev`
- Java：IDEA 启动 Application
- AI：`cd backend-ai && uvicorn main:app --reload`