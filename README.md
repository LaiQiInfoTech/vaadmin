# vaadmin

基于 **Vaadin + Spring Boot + JPA** 的后台管理/CRUD 组件库，提供通用的表格页、表单、筛选/分页/排序与实体选择等基础能力，用于快速搭建管理后台。

## 运行与配置（Profile: `vaadmin`）

本项目默认使用 `vaadmin` profile（见 `dev.w0fv1.vaadmin.VaadminApplication`），推荐通过环境变量覆盖数据库等配置：

- `VAADMIN_DB_URL`（默认：`jdbc:postgresql://localhost:5432/vaadmin`）
- `VAADMIN_DB_USERNAME`（默认：`postgres`）
- `VAADMIN_DB_PASSWORD`（默认：空）
- `VAADMIN_DDL_AUTO`（默认：`validate`，仅开发环境可改为 `update/create/create-drop`）
- `VAADMIN_LOG_LEVEL`（默认：`INFO`）

示例见 `src/main/resources/application-vaadmin.example.properties`。

## 构建

```powershell
.\gradlew.bat tasks
.\gradlew.bat build
```

## 构建可复现性（Gradle 仓库开关）

默认不启用 `mavenLocal()` 与 prerelease/milestone 仓库，避免“只在某台机器可构建”的问题。如确有需要：

```powershell
.\gradlew.bat build -PuseMavenLocal=true
.\gradlew.bat build -PusePreReleaseRepos=true
```

## GitHub Packages 依赖/发布

部分依赖与发布目标使用 GitHub Packages，需要设置：

- `GITHUB_USERNAME`
- `GITHUB_TOKEN`

### 1) 凭据去哪整？

**在本机开发（拉取私有包/发布）**：到 GitHub 生成一个 Personal Access Token (PAT)。

- 推荐使用 Fine-grained PAT：至少需要对对应仓库/组织的 **Packages** 权限（读用于拉取，写用于发布）。
- 也可用 classic PAT：至少 `read:packages`（拉取）和 `write:packages`（发布）；若还要删除包则 `delete:packages`。

把 PAT 配到环境变量：

```powershell
$env:GITHUB_USERNAME="你的GitHub用户名"
$env:GITHUB_TOKEN="你的PAT"
```

或用 Gradle 参数：

```powershell
.\gradlew.bat build -Pgpr.user=你的GitHub用户名 -Pgpr.key=你的PAT
```

**说明（关于“公开包是否需要 token”）**：对 Maven 包而言，GitHub Packages 在很多场景仍会要求认证才能下载依赖；如果你本地不带 token 解析失败（401/403），就需要按上面方式提供 PAT，或者把依赖发布到 Maven Central/其他公开 Maven 仓库。

**在 GitHub Actions（CI 发布）**：不需要手动生成 PAT，直接用 `secrets.GITHUB_TOKEN` 即可（仓库 Settings 里默认存在）。工作流已在 `.github/workflows/publish-github-packages.yml` 配好，并声明 `packages: write` 权限。
