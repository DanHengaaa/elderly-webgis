# 养老服务智慧决策 WebGIS 平台 —— 脚手架搭建与 GitHub 上传指南

> 适用项目：面向银发经济的养老资源空间适配评估与智慧决策 WebGIS 平台
> 现有目录：`D:\Elderly`（含 `data` / `documents` / `sql`）
> 环境：Windows + PowerShell

---

## 0. 开工前准备（一次性安装）

确认本机装好以下工具，命令行能跑通即可（括号内为推荐版本）：

| 工具 | 用途 | 验证命令 | 备注 |
|---|---|---|---|
| Git | 版本控制 | `git --version` | 官网 git-scm.com |
| Node.js (≥18 LTS) | 前端脚手架 | `node -v` / `npm -v` | 自带 npm |
| JDK (17) | Spring Boot | `java -version` | 用 17，Spring Boot 3 要求 |
| Maven 或用 IDE 自带 | Java 构建 | `mvn -v` | IDEA 自带可省 |
| Python (3.10–3.12) | AI 后端 | `python --version` | 勾选「Add to PATH」 |

> 数据库 PostgreSQL + PostGIS + pgvector、GeoServer 是**运行环境**，不影响脚手架搭建和上传 GitHub，可以稍后再装。

---

## 1. 目标目录结构

保留你已有的 `data` / `documents` / `sql`，只新增三个代码目录。最终结构：

```
D:\Elderly\                  ← Git 仓库根目录
│
├── .gitignore               ← 【必须先建】忽略规则
├── README.md                ← 项目说明
│
├── frontend\                ← 新增：Vue3 + Vite 前端
├── backend-java\            ← 新增：Spring Boot（RBAC/机构/匹配算法）
├── backend-ai\              ← 新增：Python FastAPI（LLM/RAG/AI伴诊）
│
├── sql\                     ← 已有：建表/初始化脚本（✅ 入库）
├── documents\               ← 已有：设计文档（✅ 入库）
└── data\                    ← 已有：原始GIS数据（❌ 不入库，太大）
```

---

![image-20260625124120298](C:\Users\HP\AppData\Roaming\Typora\typora-user-images\image-20260625124120298.png)

## 2. 【关键第一步】先建 .gitignore

**务必在 `git add` 之前完成这一步**，否则 `node_modules`、`data`、编译产物会被提交，仓库会爆，且后续很难清理。

在 `D:\Elderly` 根目录新建文件 `.gitignore`，内容如下：

```gitignore
# ===== 原始GIS数据（太大，不上传）=====
/data/
*.shp
*.shx
*.dbf
*.prj
*.cpg
*.tif
*.tiff
*.gdb/
*.zip

# ===== 前端 =====
frontend/node_modules/
frontend/dist/
frontend/.vite/
*.local

# ===== Java / Spring Boot =====
backend-java/target/
backend-java/.mvn/
backend-java/*.iml
backend-java/.idea/

# ===== Python =====
backend-ai/venv/
backend-ai/__pycache__/
backend-ai/**/__pycache__/
*.pyc
backend-ai/.env

# ===== 密钥与配置（API Key 绝不上传）=====
.env
*.env.local
**/application-secret.yml
*-secret.*

# ===== 通用 =====
.DS_Store
Thumbs.db
.vscode/
*.log
```

> ⚠️ 特别注意 `.env` 和 `*-secret.*`：大模型 API Key、数据库密码绝对不能进仓库。GitHub 一旦泄露 Key 会被自动扫描盗刷。

---

## 3. 搭建前端脚手架（Vue 3 + Vite）

在 PowerShell 里执行：

```powershell
cd D:\Elderly

# 用 Vite 创建 Vue 项目，目录名直接叫 frontend
npm create vite@latest frontend -- --template vue

cd frontend
npm install

# 安装你项目要用的核心依赖
npm install ol echarts axios pinia vue-router element-plus
```

依赖说明（对应你的功能模块）：

| 包 | 对应模块 |
|---|---|
| `ol`（OpenLayers）| M2 地图渲染、等时圈、图层叠加 |
| `echarts` | M5 评价雷达图、数据可视化 |
| `axios` | 调用后端 RESTful API |
| `pinia` | 全局状态（登录态、用户档案）|
| `vue-router` | 客户/机构/管理员多角色页面路由 |
| `element-plus` | UI 组件库（表单、问卷、表格）|

验证：`npm run dev`，浏览器打开提示的 `http://localhost:5173` 能看到页面即成功，然后 `Ctrl+C` 停掉。

> 想用 React 的话：把这一步换成 `npm create vite@latest frontend -- --template react`，地图库改装 `leaflet react-leaflet` 或继续用 `ol`。

---

## 4. 搭建 Java 后端脚手架（Spring Boot）

推荐用官方 **Spring Initializr** 生成，最省事：

1. 打开 https://start.spring.io
2. 按下表填写：

| 选项 | 值 |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.x（默认稳定版）|
| Group | `com.hhu.elderly` |
| Artifact | `backend-java` |
| Packaging | Jar |
| Java | 17 |

3. 右侧 **Add Dependencies** 添加：
   - `Spring Web`（RESTful API）
   - `Spring Security`（RBAC 鉴权，配合 JWT）
   - `Spring Data JPA`（数据库 ORM）
   - `PostgreSQL Driver`
   - `Lombok`（简化代码）
   - `Validation`（参数校验）

4. 点 **Generate** 下载 zip，**解压后把里面的内容放进** `D:\Elderly\backend-java\`（注意是内容，不是再套一层文件夹）。

> JWT 库 Spring Initializr 没有，后面在 `pom.xml` 里手动加 `jjwt` 即可。

验证：用 IDEA 打开 `backend-java`，等 Maven 下载完依赖，能成功启动 `Application` 主类即可。

---

## 5. 搭建 Python AI 后端脚手架（FastAPI）

```powershell
cd D:\Elderly
mkdir backend-ai
cd backend-ai

# 创建虚拟环境（venv 会被 .gitignore 忽略，不上传）
python -m venv venv
.\venv\Scripts\Activate.ps1

# 安装 AI 伴诊核心依赖
pip install fastapi "uvicorn[standard]" langchain openai psycopg2-binary pgvector python-dotenv

# 导出依赖清单（这个文件要上传）
pip freeze > requirements.txt
```

> 如果 PowerShell 报「无法加载脚本」，先执行一次：
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`，输入 Y 即可。

新建一个最小入口文件 `backend-ai\main.py`：

```python
from fastapi import FastAPI

app = FastAPI(title="养老 AI 伴诊服务")

@app.get("/health")
def health():
    return {"status": "ok"}
```

验证：`uvicorn main:app --reload`，访问 `http://127.0.0.1:8000/health` 返回 ok 即成功。

API Key 写进 `backend-ai\.env`（已被 .gitignore 忽略），例如：
```
OPENAI_API_KEY=你的key
DATABASE_URL=postgresql://user:pwd@localhost:5432/elderly
```

---

## 6. 写一份 README.md

在 `D:\Elderly` 根目录新建 `README.md`，让队友和评委一眼看懂项目：

```markdown
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
```

---

## 7. 上传到 GitHub

### 7.1 在 GitHub 网站建空仓库
1. 登录 github.com → 右上角 **+** → **New repository**
2. Repository name 填 `elderly-webgis`（或自定）
3. 选 **Private**（开发期建议私有，结题前再公开）
4. **不要**勾选 Add README / .gitignore（本地已经有了，避免冲突）
5. 点 **Create repository**，记下页面给出的仓库地址

### 7.2 本地初始化并推送

```powershell
cd D:\Elderly

git init
git branch -M main

# 关键：确认 .gitignore 已在根目录、内容正确
git status        # 检查列表里【不应】出现 data/、node_modules、venv、target

git add .
git commit -m "init: 项目脚手架（前端+双后端+sql+文档）"

# 把下面地址换成你自己的仓库地址
git remote add origin https://github.com/你的用户名/elderly-webgis.git
git push -u origin main
```

> 第一次 push 会要求登录。现在 GitHub 不能用账号密码，**用 Personal Access Token 当密码**：
> GitHub → Settings → Developer settings → Personal access tokens → 生成一个有 `repo` 权限的 token，粘贴到密码框。

### 7.3 验证
刷新 GitHub 仓库页面，能看到 `frontend` / `backend-java` / `backend-ai` / `sql` / `documents` 和 README，但**看不到 `data` 文件夹**，就说明配置全对了。

---

## 8. 团队协作（三人）日常流程

队友夏有辉、江修贤拉取代码：
```powershell
git clone https://github.com/你的用户名/elderly-webgis.git
```
> 注意：clone 下来后，`frontend` 要自己 `npm install`，`backend-ai` 要自己建 venv 并 `pip install -r requirements.txt`，因为这些目录没上传。

日常提交三连：
```powershell
git pull            # 先拉最新，避免冲突
git add .
git commit -m "feat: 完成等时圈分析接口"
git push
```

建议规范：
- 每人开自己的分支开发，再合并到 `main`：`git checkout -b feature/ai-rag`
- commit 信息用前缀：`feat:`（功能）/ `fix:`（修复）/ `docs:`（文档）

---

## 9. 关于 data 数据怎么办（重要补充）

`data/` 不进 Git，但队友需要数据怎么共享？三选一：

1. **网盘 / 学校云盘**：把 `data` 打包传网盘，README 里写下载链接 —— 最简单，推荐。
2. **数据入库**：原始 shp/Excel 先导入 PostGIS，把**导入脚本和建表语句**放进 `sql/` 上传（数据本身不传），队友跑脚本即可复现。
3. **Git LFS**：若坚持要把部分数据放仓库，用 `git lfs track "*.shp"` 走大文件存储（GitHub 免费额度 1GB，谨慎用）。

> 你的 `sql/` 文件夹正好适合走方案 2：把建表、空间索引、坐标转换的 SQL 都放进去上传，是最规范的做法。

---

## 10. 踩坑速查

| 现象 | 原因 / 解决 |
|---|---|
| 仓库几百 MB / push 巨慢 | `data` 或 `node_modules` 被提交了。`.gitignore` 没生效或加晚了，需用 `git rm -r --cached <目录>` 移除后重新提交 |
| push 提示密码错误 | GitHub 要用 Personal Access Token，不是登录密码 |
| API Key 泄露被警告 | `.env` 没被忽略。立即在平台后台**吊销并重置 Key** |
| 队友 clone 后跑不起来 | 提醒他们自己 `npm install` / 建 venv，这些目录本就不上传 |
| PowerShell 不能激活 venv | 先执行 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |

---

完成 0–7 步，你的前后端脚手架就搭好并成功上传 GitHub 了。建议按 1→2→3→4→5→6→7 的顺序做，其中 **第 2 步（.gitignore）一定要在第 7 步 git add 之前完成**。
