# 项目管理系统 API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8000`
- **认证方式**: Bearer Token (JWT)
- **请求头**: 
  ```
  Authorization: Bearer {access_token}
  Content-Type: application/json
  ```

---

## 1. 认证模块 (`/api/auth`)

### 1.1 用户注册
- **接口**: `POST /api/auth/register`
- **描述**: 注册新用户
- **请求体**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "full_name": "string",
  "role": "developer" // admin, project_manager, developer, tester
}
```
- **响应**: UserResponse

### 1.2 用户登录
- **接口**: `POST /api/auth/login`
- **描述**: 用户登录获取token
- **请求体**:
```json
{
  "username": "string",
  "password": "string"
}
```
- **响应**:
```json
{
  "access_token": "string",
  "token_type": "bearer"
}
```

### 1.3 获取当前用户信息
- **接口**: `GET /api/auth/me`
- **描述**: 获取当前登录用户信息
- **需要认证**: ✅
- **响应**: UserResponse

---

## 2. 用户管理 (`/api/users`)

### 2.1 获取用户列表
- **接口**: `GET /api/users/`
- **需要认证**: ✅

### 2.2 获取指定用户
- **接口**: `GET /api/users/{user_id}`
- **需要认证**: ✅

### 2.3 更新用户信息
- **接口**: `PUT /api/users/{user_id}`
- **需要认证**: ✅
- **权限**: 管理员或本人

### 2.4 删除用户
- **接口**: `DELETE /api/users/{user_id}`
- **需要认证**: ✅
- **权限**: 仅管理员

### 2.5 修改密码
- **接口**: `POST /api/users/change-password`
- **需要认证**: ✅
- **请求体**:
```json
{
  "old_password": "string",
  "new_password": "string"
}
```

### 2.6 获取用户统计
- **接口**: `GET /api/users/me/stats`
- **需要认证**: ✅
- **响应**: 用户的项目、任务、BUG统计数据

---

## 3. 项目管理 (`/api/projects`)

### 3.1 创建项目
- **接口**: `POST /api/projects/`
- **需要认证**: ✅
- **请求体**:
```json
{
  "name": "string",
  "description": "string",
  "project_manager_id": "string",
  "start_date": "datetime",
  "end_date": "datetime"
}
```

### 3.2 获取项目列表
- **接口**: `GET /api/projects/`
- **需要认证**: ✅
- **描述**: 返回用户参与的所有项目

### 3.3 获取项目详情
- **接口**: `GET /api/projects/{project_id}`
- **需要认证**: ✅

### 3.4 更新项目
- **接口**: `PUT /api/projects/{project_id}`
- **需要认证**: ✅
- **权限**: 项目所有者或项目经理

### 3.5 添加团队成员
- **接口**: `POST /api/projects/{project_id}/members/{user_id}`
- **需要认证**: ✅
- **权限**: 项目所有者或项目经理

### 3.6 移除团队成员
- **接口**: `DELETE /api/projects/{project_id}/members/{user_id}`
- **需要认证**: ✅
- **权限**: 项目所有者或项目经理

---

## 4. 任务管理 (`/api/tasks`)

### 4.1 创建任务
- **接口**: `POST /api/tasks/`
- **需要认证**: ✅
- **请求体**:
```json
{
  "title": "string",
  "description": "string",
  "project_id": "string",
  "assigned_to": "string",
  "collaborators": ["string"],
  "priority": "medium", // low, medium, high, urgent
  "estimated_hours": 8.0,
  "due_date": "datetime"
}
```

### 4.2 获取任务列表
- **接口**: `GET /api/tasks/`
- **需要认证**: ✅
- **查询参数**:
  - `project_id`: 项目ID（可选）
  - `assigned_to`: 负责人ID（可选）
  - `status`: 任务状态（可选）

### 4.3 获取任务详情
- **接口**: `GET /api/tasks/{task_id}`
- **需要认证**: ✅

### 4.4 更新任务
- **接口**: `PUT /api/tasks/{task_id}`
- **需要认证**: ✅

### 4.5 删除任务
- **接口**: `DELETE /api/tasks/{task_id}`
- **需要认证**: ✅
- **权限**: 任务创建者或项目经理

---

## 5. BUG管理 (`/api/bugs`)

### 5.1 创建BUG
- **接口**: `POST /api/bugs/`
- **需要认证**: ✅
- **请求体**:
```json
{
  "title": "string",
  "description": "string",
  "project_id": "string",
  "assigned_to": "string",
  "severity": "medium", // low, medium, high, critical
  "steps_to_reproduce": "string",
  "bug_images": ["base64_string"]
}
```

### 5.2 获取BUG列表
- **接口**: `GET /api/bugs/`
- **需要认证**: ✅
- **查询参数**: project_id, assigned_to, status

### 5.3 获取BUG详情
- **接口**: `GET /api/bugs/{bug_id}`
- **需要认证**: ✅

### 5.4 更新BUG
- **接口**: `PUT /api/bugs/{bug_id}`
- **需要认证**: ✅

### 5.5 上传BUG附件
- **接口**: `POST /api/bugs/{bug_id}/attachments`
- **需要认证**: ✅
- **请求**: multipart/form-data

### 5.6 开始复测
- **接口**: `POST /api/bugs/{bug_id}/retest`
- **需要认证**: ✅
- **权限**: 测试人员

### 5.7 关闭BUG
- **接口**: `POST /api/bugs/{bug_id}/close`
- **需要认证**: ✅
- **权限**: 测试人员

### 5.8 重新打开BUG
- **接口**: `POST /api/bugs/{bug_id}/reopen`
- **需要认证**: ✅
- **权限**: 测试人员

### 5.9 删除BUG
- **接口**: `DELETE /api/bugs/{bug_id}`
- **需要认证**: ✅

### 5.10 获取BUG评论
- **接口**: `GET /api/bugs/{bug_id}/comments`
- **需要认证**: ✅

### 5.11 添加BUG评论
- **接口**: `POST /api/bugs/{bug_id}/comments`
- **需要认证**: ✅

---

## 6. 统计看板 (`/api/statistics`)

### 6.1 获取项目统计
- **接口**: `GET /api/statistics/projects/{project_id}`
- **需要认证**: ✅
- **响应**:
```json
{
  "project_id": "string",
  "project_name": "string",
  "total_tasks": 0,
  "completed_tasks": 0,
  "task_completion_rate": 0.0,
  "total_bugs": 0,
  "open_bugs": 0,
  "closed_bugs": 0,
  "bug_rate": 0.0,
  "team_size": 0
}
```

### 6.2 获取所有项目统计概览
- **接口**: `GET /api/statistics/overview`
- **需要认证**: ✅

---

## 7. 通知管理 (`/api/notifications`)

### 7.1 获取通知列表
- **接口**: `GET /api/notifications/`
- **需要认证**: ✅
- **查询参数**: `unread_only` (boolean)

### 7.2 获取未读通知数量
- **接口**: `GET /api/notifications/unread/count`
- **需要认证**: ✅

### 7.3 标记通知为已读
- **接口**: `PUT /api/notifications/{notification_id}/read`
- **需要认证**: ✅

### 7.4 标记所有通知为已读
- **接口**: `PUT /api/notifications/read-all`
- **需要认证**: ✅

### 7.5 删除通知
- **接口**: `DELETE /api/notifications/{notification_id}`
- **需要认证**: ✅

---

## 8. 代码发布管理 (`/api/deployments`)

### 8.1 创建代码发布申请（包）
- **接口**: `POST /api/deployments/`
- **需要认证**: ✅
- **请求**: multipart/form-data
- **参数**:
  - `title`: 包名称
  - `description`: 描述
  - `project_id`: 项目ID
  - `deployment_type`: logic_code 或 page_code
  - `environment_id`: 环境ID
  - `package_path`: 包路径
  - `remote_package_id`: 远程包ID

### 8.2 上传新版本
- **接口**: `POST /api/deployments/{deployment_id}/versions`
- **需要认证**: ✅
- **请求**: multipart/form-data
- **参数**:
  - `file`: ZIP文件
  - `description`: 版本描述

### 8.3 获取发布列表
- **接口**: `GET /api/deployments/`
- **需要认证**: ✅
- **查询参数**: project_id, deployment_type

### 8.4 获取发布详情
- **接口**: `GET /api/deployments/{deployment_id}`
- **需要认证**: ✅

### 8.5 获取特定版本详情
- **接口**: `GET /api/deployments/{deployment_id}/versions/{version}`
- **需要认证**: ✅

### 8.6 更新发布申请
- **接口**: `PUT /api/deployments/{deployment_id}`
- **需要认证**: ✅

### 8.7 审核版本
- **接口**: `POST /api/deployments/{deployment_id}/versions/{version}/review`
- **需要认证**: ✅
- **权限**: 项目经理
- **参数**:
  - `action`: approved 或 rejected
  - `comment`: 审核意见

### 8.8 部署版本
- **接口**: `POST /api/deployments/{deployment_id}/versions/{version}/deploy`
- **需要认证**: ✅
- **权限**: 项目经理或管理员

### 8.9 重新分析版本
- **接口**: `POST /api/deployments/{deployment_id}/versions/{version}/reanalyze`
- **需要认证**: ✅

### 8.10 版本对比
- **接口**: `GET /api/deployments/{deployment_id}/compare`
- **需要认证**: ✅
- **查询参数**: version1, version2

### 8.11 删除发布申请
- **接口**: `DELETE /api/deployments/{deployment_id}`
- **需要认证**: ✅

### 8.12 删除特定版本
- **接口**: `DELETE /api/deployments/{deployment_id}/versions/{version}`
- **需要认证**: ✅

### 8.13 获取远程包列表
- **接口**: `GET /api/deployments/remote-packages`
- **需要认证**: ✅
- **查询参数**: environment_id

### 8.14 匹配或创建远程包
- **接口**: `POST /api/deployments/match-or-create-package`
- **需要认证**: ✅

---

## 9. 环境管理 (`/api/environments`)

### 9.1 创建环境配置
- **接口**: `POST /api/environments/{project_id}`
- **需要认证**: ✅
- **请求体**:
```json
{
  "name": "string",
  "url": "string",
  "username": "string",
  "password": "string",
  "description": "string"
}
```

### 9.2 获取项目环境列表
- **接口**: `GET /api/environments/project/{project_id}`
- **需要认证**: ✅

### 9.3 更新环境配置
- **接口**: `PUT /api/environments/{environment_id}`
- **需要认证**: ✅

### 9.4 删除环境配置
- **接口**: `DELETE /api/environments/{environment_id}`
- **需要认证**: ✅

### 9.5 登录环境
- **接口**: `POST /api/environments/{environment_id}/login`
- **需要认证**: ✅
- **查询参数**: force_refresh (boolean)

### 9.6 获取环境Cookies
- **接口**: `GET /api/environments/{environment_id}/cookies`
- **需要认证**: ✅

---

## 10. AI配置管理 (`/api/ai-config`)

### 10.1 创建AI配置
- **接口**: `POST /api/ai-config/`
- **需要认证**: ✅
- **权限**: 仅管理员
- **请求体**:
```json
{
  "provider": "openai", // openai, qwen, zhipu, claude, deepseek, none
  "api_key": "string",
  "model": "string",
  "base_url": "string",
  "is_enabled": true,
  "description": "string"
}
```

### 10.2 获取AI配置列表
- **接口**: `GET /api/ai-config/`
- **需要认证**: ✅
- **权限**: 仅管理员

### 10.3 获取当前启用的AI配置
- **接口**: `GET /api/ai-config/active`
- **需要认证**: ✅

### 10.4 获取指定AI配置
- **接口**: `GET /api/ai-config/{config_id}`
- **需要认证**: ✅
- **权限**: 仅管理员

### 10.5 更新AI配置
- **接口**: `PUT /api/ai-config/{config_id}`
- **需要认证**: ✅
- **权限**: 仅管理员

### 10.6 删除AI配置
- **接口**: `DELETE /api/ai-config/{config_id}`
- **需要认证**: ✅
- **权限**: 仅管理员

### 10.7 测试AI配置
- **接口**: `POST /api/ai-config/{config_id}/test`
- **需要认证**: ✅
- **权限**: 仅管理员

---

## 11. BI商业智能 (`/api/bi`)

### 11.1 数据源管理

#### 11.1.1 创建数据源
- **接口**: `POST /api/bi/datasources`
- **需要认证**: ✅

#### 11.1.2 获取数据源列表
- **接口**: `GET /api/bi/datasources`
- **需要认证**: ✅
- **查询参数**: project_id

#### 11.1.3 获取数据源详情
- **接口**: `GET /api/bi/datasources/{datasource_id}`
- **需要认证**: ✅

#### 11.1.4 更新数据源
- **接口**: `PUT /api/bi/datasources/{datasource_id}`
- **需要认证**: ✅

#### 11.1.5 删除数据源
- **接口**: `DELETE /api/bi/datasources/{datasource_id}`
- **需要认证**: ✅

#### 11.1.6 获取数据源对象
- **接口**: `GET /api/bi/datasources/{datasource_id}/objects`
- **需要认证**: ✅

#### 11.1.7 预览数据源数据
- **接口**: `POST /api/bi/datasources/{datasource_id}/preview`
- **需要认证**: ✅

### 11.2 报表管理

#### 11.2.1 创建报表
- **接口**: `POST /api/bi/reports`
- **需要认证**: ✅

#### 11.2.2 获取报表列表
- **接口**: `GET /api/bi/reports`
- **需要认证**: ✅

#### 11.2.3 获取报表详情
- **接口**: `GET /api/bi/reports/{report_id}`
- **需要认证**: ✅

#### 11.2.4 执行报表查询
- **接口**: `POST /api/bi/reports/{report_id}/execute`
- **需要认证**: ✅

#### 11.2.5 更新报表
- **接口**: `PUT /api/bi/reports/{report_id}`
- **需要认证**: ✅

#### 11.2.6 删除报表
- **接口**: `DELETE /api/bi/reports/{report_id}`
- **需要认证**: ✅

### 11.3 仪表板管理

#### 11.3.1 创建仪表板
- **接口**: `POST /api/bi/dashboards`
- **需要认证**: ✅

#### 11.3.2 获取仪表板列表
- **接口**: `GET /api/bi/dashboards`
- **需要认证**: ✅

#### 11.3.3 获取仪表板详情
- **接口**: `GET /api/bi/dashboards/{dashboard_id}`
- **需要认证**: ✅

#### 11.3.4 更新仪表板
- **接口**: `PUT /api/bi/dashboards/{dashboard_id}`
- **需要认证**: ✅

#### 11.3.5 删除仪表板
- **接口**: `DELETE /api/bi/dashboards/{dashboard_id}`
- **需要认证**: ✅

### 11.4 系统配置

#### 11.4.1 获取启用的数据源类型
- **接口**: `GET /api/bi/config/datasource-types`
- **需要认证**: ✅

---

## 12. 知识库管理 (`/api/knowledge`)

### 12.1 上传知识库文档
- **接口**: `POST /api/knowledge/`
- **需要认证**: ✅
- **请求**: multipart/form-data
- **参数**:
  - `title`: 文档标题（必填）
  - `project_id`: 项目ID（必填）
  - `category`: 分类（可选）
  - `description`: 描述（可选）
  - `tags`: 标签，逗号分隔（可选）
  - `file`: 文件（必填）
- **支持格式**: MD, Word, Excel, PDF, TXT, 图片
- **响应**: KnowledgeResponse

### 12.2 获取知识库列表
- **接口**: `GET /api/knowledge/`
- **需要认证**: ✅
- **查询参数**:
  - `project_id`: 项目ID（可选）
  - `category`: 分类（可选）
  - `keyword`: 搜索关键词（可选）
  - `file_type`: 文件类型（可选）
  - `status`: 状态（可选）
- **响应**: List[KnowledgeResponse]

### 12.3 获取文档详情
- **接口**: `GET /api/knowledge/{knowledge_id}`
- **需要认证**: ✅
- **描述**: 获取文档详情，自动增加浏览次数
- **响应**: KnowledgeResponse

### 12.4 更新文档信息
- **接口**: `PUT /api/knowledge/{knowledge_id}`
- **需要认证**: ✅
- **权限**: 文档创建者或管理员
- **请求体**:
```json
{
  "title": "string",
  "category": "string",
  "description": "string",
  "content": "string",
  "tags": ["string"],
  "status": "published"
}
```

### 12.5 删除文档
- **接口**: `DELETE /api/knowledge/{knowledge_id}`
- **需要认证**: ✅
- **权限**: 文档创建者或管理员

### 12.6 获取分类列表
- **接口**: `GET /api/knowledge/categories/list`
- **需要认证**: ✅
- **查询参数**: `project_id`（可选）
- **响应**:
```json
{
  "categories": ["string"]
}
```

### 12.7 获取标签列表
- **接口**: `GET /api/knowledge/tags/list`
- **需要认证**: ✅
- **查询参数**: `project_id`（可选）
- **响应**:
```json
{
  "tags": ["string"]
}
```

### 12.8 获取知识库统计
- **接口**: `GET /api/knowledge/statistics/{project_id}`
- **需要认证**: ✅
- **响应**:
```json
{
  "total_docs": 0,
  "file_type_stats": {
    "markdown": 10,
    "word": 5,
    "pdf": 3
  },
  "status_stats": {
    "published": 15,
    "draft": 3
  },
  "total_views": 100
}
```

---

## 13. 健康检查 (`/api`)

### 12.1 健康检查
- **接口**: `GET /api/health`
- **描述**: 检查服务运行状态和数据库连接

### 12.2 就绪检查
- **接口**: `GET /api/ready`
- **描述**: 检查服务是否准备好接收请求

### 12.3 存活检查
- **接口**: `GET /api/live`
- **描述**: 检查服务进程是否存活

---

## 数据模型

### 用户角色 (UserRole)
- `admin`: 管理员
- `project_manager`: 项目经理
- `developer`: 开发人员
- `tester`: 测试人员

### 任务状态 (TaskStatus)
- `todo`: 待办
- `in_progress`: 进行中
- `completed`: 已完成
- `cancelled`: 已取消

### 任务优先级 (TaskPriority)
- `low`: 低
- `medium`: 中
- `high`: 高
- `urgent`: 紧急

### BUG状态 (BugStatus)
- `open`: 打开
- `assigned`: 已分配
- `in_progress`: 处理中
- `fixed`: 已修复
- `testing`: 测试中
- `closed`: 已关闭
- `reopened`: 重新打开

### BUG严重程度 (BugSeverity)
- `low`: 低
- `medium`: 中
- `high`: 高
- `critical`: 严重

### 发布类型 (DeploymentType)
- `logic_code`: 逻辑代码包
- `page_code`: 页面代码

### 发布状态 (DeploymentStatus)
- `pending`: 待审核
- `analyzing`: AI分析中
- `analysis_completed`: 分析完成
- `approved`: 审核通过
- `rejected`: 审核拒绝
- `deploying`: 部署中
- `deployed`: 已部署
- `failed`: 部署失败

### 风险等级 (RiskLevel)
- `low`: 低风险
- `medium`: 中风险
- `high`: 高风险
- `critical`: 严重风险

### AI提供商 (AIProvider)
- `none`: 不使用AI
- `openai`: OpenAI GPT
- `qwen`: 阿里通义千问
- `zhipu`: 智谱AI
- `claude`: Anthropic Claude
- `deepseek`: DeepSeek

---

## 错误码说明

- `200`: 成功
- `201`: 创建成功
- `204`: 删除成功（无内容）
- `400`: 请求参数错误
- `401`: 未授权（未登录或token无效）
- `403`: 禁止访问（无权限）
- `404`: 资源不存在
- `500`: 服务器内部错误

---

## 使用示例

### 登录并获取token
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 使用token访问受保护接口
```bash
curl -X GET http://localhost:8000/api/projects/ \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 创建项目
```bash
curl -X POST http://localhost:8000/api/projects/ \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "新项目",
    "description": "项目描述",
    "project_manager_id": "USER_ID"
  }'
```

---

## 注意事项

1. 所有需要认证的接口都必须在请求头中携带有效的JWT token
2. 文件上传接口使用 `multipart/form-data` 格式
3. 日期时间格式为 ISO 8601 标准
4. 所有ID字段均为MongoDB ObjectId的字符串形式
5. 图片数据使用base64编码传输
6. API支持CORS跨域请求

---

**文档版本**: v1.0.0  
**最后更新**: 2025-12-24
