# API集成说明文档

## 概述

本文档说明了 SalesEasy 打包助手插件的 API 集成实现，包括登录功能和通知功能。

## 架构设计

### 1. 配置管理 (`ApiConfig.kt`)

**位置**: `src/main/kotlin/top/javaxiaowu/saleseasypackagingassistant/config/ApiConfig.kt`

**功能**:
- 集中管理所有 API 相关配置
- 提供统一的 API 端点定义
- 支持动态修改基础域名

**使用方式**:
```kotlin
// 获取当前基础URL
val baseUrl = ApiConfig.BASE_URL  // http://114.66.56.200:6002

// 修改基础URL（如需切换环境）
ApiConfig.setBaseUrl("http://new-domain.com:8080")

// 获取完整的API URL
val loginUrl = ApiConfig.getApiUrl(ApiConfig.Auth.LOGIN)
// 结果: http://114.66.56.200:6002/api/auth/login
```

**配置的API端点**:
- 认证模块 (`Auth`): 登录、注册、获取用户信息
- 用户管理 (`Users`): 用户列表、详情、更新、删除
- 项目管理 (`Projects`): 项目CRUD、成员管理
- 任务管理 (`Tasks`): 任务CRUD
- BUG管理 (`Bugs`): BUG管理、评论
- 通知管理 (`Notifications`): 通知列表、已读标记、删除
- 代码发布 (`Deployments`): 发布管理、版本控制
- 环境管理 (`Environments`): 环境配置
- 统计看板 (`Statistics`): 数据统计
- 知识库 (`Knowledge`): 文档管理
- AI配置 (`AIConfig`): AI模型配置
- 健康检查 (`Health`): 服务状态检查

### 2. HTTP客户端 (`HttpClient.kt`)

**位置**: `src/main/kotlin/top/javaxiaowu/saleseasypackagingassistant/utils/HttpClient.kt`

**功能**:
- 封装所有 HTTP 请求逻辑
- 自动处理认证 Token
- 统一的错误处理
- 支持 GET、POST、PUT、DELETE 请求
- 支持表单数据和 JSON 数据

**使用方式**:
```kotlin
// GET 请求
val response = HttpClient.get(
    endpoint = "users/123",
    requireAuth = true,
    responseType = UserResponse::class.java
)

// POST 请求（JSON）
val response = HttpClient.post(
    endpoint = "projects/",
    body = createProjectRequest,
    requireAuth = true,
    responseType = ProjectResponse::class.java
)

// POST 请求（表单）
val response = HttpClient.postForm(
    endpoint = "auth/login",
    formData = mapOf("username" to "admin", "password" to "pass"),
    requireAuth = false,
    responseType = LoginResponse::class.java
)

// 设置认证Token
HttpClient.setAuthToken("your_jwt_token")

// 清除认证Token
HttpClient.clearAuthToken()
```

### 3. 登录服务 (`LoginService.kt`)

**位置**: `src/main/kotlin/top/javaxiaowu/saleseasypackagingassistant/services/LoginService.kt`

**实现类**:

#### `ApiLoginService` - 真实API实现
使用真实的后端 API 进行用户认证。

**登录流程**:
1. 调用 `/api/auth/login` 接口获取 access_token
2. 保存 token 到 HttpClient
3. 调用 `/api/auth/me` 获取用户详细信息
4. 返回登录结果

**使用示例**:
```kotlin
val loginService = ApiLoginService()
val result = loginService.login("username", "password")

if (result.success) {
    println("登录成功: ${result.userInfo?.username}")
    println("Token: ${result.token}")
} else {
    println("登录失败: ${result.message}")
}
```

#### `MockLoginService` - 模拟实现
用于开发和测试环境，不需要真实的后端服务。

### 4. 通知服务 (`NotificationService.kt`)

**位置**: `src/main/kotlin/top/javaxiaowu/saleseasypackagingassistant/services/NotificationService.kt`

**实现类**:

#### `ApiNotificationService` - 真实API实现
使用真实的后端 API 进行通知管理。

**功能**:
- 获取通知列表（支持分页）
- 获取未读通知数量
- 标记单个通知为已读
- 标记所有通知为已读
- 删除通知
- 订阅实时通知（WebSocket，待实现）

**使用示例**:
```kotlin
val notificationService = ApiNotificationService()

// 获取第一页通知（每页20条）
val notifications = notificationService.getNotifications(page = 1, pageSize = 20)

// 获取未读数量
val unreadCount = notificationService.getUnreadCount()

// 标记为已读
notificationService.markAsRead("notification_id")

// 标记所有为已读
notificationService.markAllAsRead()

// 删除通知
notificationService.deleteNotification("notification_id")
```

#### `MockNotificationService` - 模拟实现
用于开发和测试环境，提供示例通知数据。

### 5. 服务工厂 (`ServiceFactory.kt`)

**位置**: `src/main/kotlin/top/javaxiaowu/saleseasypackagingassistant/services/ServiceFactory.kt`

**功能**:
- 统一管理服务实例
- 支持在真实服务和模拟服务之间切换
- 单例模式，确保服务实例唯一性

**使用方式**:
```kotlin
// 使用真实API服务（默认）
ServiceFactory.setServiceMode(useMock = false)

// 使用模拟服务（开发/测试）
ServiceFactory.setServiceMode(useMock = true)

// 获取登录服务
val loginService = ServiceFactory.getLoginService()

// 获取通知服务
val notificationService = ServiceFactory.getNotificationService()

// 重置所有服务实例
ServiceFactory.resetServices()
```

## 配置说明

### 修改API域名

有三种方式可以修改API域名:

#### 方式1: 代码中直接修改
```kotlin
ApiConfig.setBaseUrl("http://new-domain.com:8080")
```

#### 方式2: 修改配置文件
编辑 `ApiConfig.kt` 文件，修改 `BASE_URL` 的默认值:
```kotlin
var BASE_URL: String = "http://your-domain.com:port"
```

#### 方式3: 环境变量（推荐）
可以扩展 `ApiConfig.kt` 支持从环境变量读取:
```kotlin
var BASE_URL: String = System.getenv("API_BASE_URL") ?: "http://114.66.56.200:6002"
```

然后在运行时设置环境变量 `API_BASE_URL`。

### 切换服务模式

在应用启动时设置服务模式:

```kotlin
// 在插件初始化时
class MyPlugin : Plugin {
    override fun initialize() {
        // 生产环境使用真实API
        ServiceFactory.setServiceMode(useMock = false)
        
        // 或者根据配置决定
        val useMock = System.getProperty("use.mock.services", "false").toBoolean()
        ServiceFactory.setServiceMode(useMock = useMock)
    }
}
```

## API接口说明

### 登录接口

**端点**: `POST /api/auth/login`

**请求格式**: `application/x-www-form-urlencoded`

**请求参数**:
```
username=your_username
password=your_password
```

**响应示例**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

### 获取用户信息接口

**端点**: `GET /api/auth/me`

**请求头**:
```
Authorization: Bearer {access_token}
```

**响应示例**:
```json
{
  "id": "user_id",
  "username": "admin",
  "email": "admin@example.com",
  "full_name": "Admin User",
  "role": "admin"
}
```

### 获取通知列表接口

**端点**: `GET /api/notifications/`

**查询参数**:
- `skip`: 跳过的记录数（分页）
- `limit`: 返回的记录数（分页）
- `unread_only`: 只返回未读通知（可选）

**请求头**:
```
Authorization: Bearer {access_token}
```

**响应示例**:
```json
[
  {
    "id": "notification_id",
    "title": "系统消息",
    "message": "欢迎使用系统",
    "type": "info",
    "is_read": false,
    "created_at": "2025-12-24T15:00:00Z"
  }
]
```

### 获取未读通知数量接口

**端点**: `GET /api/notifications/unread/count`

**请求头**:
```
Authorization: Bearer {access_token}
```

**响应示例**:
```json
{
  "count": 5
}
```

### 标记通知为已读接口

**端点**: `PUT /api/notifications/{notification_id}/read`

**请求头**:
```
Authorization: Bearer {access_token}
```

### 标记所有通知为已读接口

**端点**: `PUT /api/notifications/read-all`

**请求头**:
```
Authorization: Bearer {access_token}
```

### 删除通知接口

**端点**: `DELETE /api/notifications/{notification_id}`

**请求头**:
```
Authorization: Bearer {access_token}
```

## 错误处理

所有API响应都包装在 `ApiResponse` 中:

```kotlin
data class ApiResponse<T>(
    val success: Boolean,      // 请求是否成功
    val data: T? = null,       // 响应数据
    val message: String? = null, // 错误消息
    val code: Int = 200        // HTTP状态码
)
```

**使用示例**:
```kotlin
val response = HttpClient.get(...)

if (response.success) {
    // 请求成功
    val data = response.data
    // 处理数据
} else {
    // 请求失败
    val errorMessage = response.message
    val statusCode = response.code
    // 处理错误
}
```

## 常见错误码

- `200`: 成功
- `201`: 创建成功
- `400`: 请求参数错误
- `401`: 未授权（未登录或token无效）
- `403`: 禁止访问（无权限）
- `404`: 资源不存在
- `500`: 服务器内部错误

## 安全性说明

1. **Token管理**: 
   - Token 存储在内存中，不持久化
   - 登出时自动清除 Token
   - Token 自动添加到需要认证的请求中

2. **HTTPS**: 
   - 生产环境建议使用 HTTPS
   - 修改 `ApiConfig.BASE_URL` 为 `https://` 开头的地址

3. **密码传输**: 
   - 密码通过 POST 请求体传输
   - 建议使用 HTTPS 加密传输

## 扩展开发

### 添加新的API端点

1. 在 `ApiConfig.kt` 中添加端点定义:
```kotlin
object NewModule {
    const val LIST = "new-module/"
    const val DETAIL = "new-module/{id}"
}
```

2. 在服务类中调用:
```kotlin
val response = HttpClient.get(
    endpoint = ApiConfig.NewModule.LIST,
    requireAuth = true,
    responseType = YourResponseType::class.java
)
```

### 添加新的服务

1. 定义服务接口:
```kotlin
interface YourService {
    fun doSomething(): Result
}
```

2. 实现真实服务:
```kotlin
class ApiYourService : YourService {
    override fun doSomething(): Result {
        // 调用 HttpClient
    }
}
```

3. 实现模拟服务:
```kotlin
class MockYourService : YourService {
    override fun doSomething(): Result {
        // 返回模拟数据
    }
}
```

4. 在 `ServiceFactory` 中添加:
```kotlin
fun getYourService(): YourService {
    return if (useMockServices) {
        MockYourService()
    } else {
        ApiYourService()
    }
}
```

## 测试建议

1. **单元测试**: 使用 `MockLoginService` 和 `MockNotificationService` 进行单元测试
2. **集成测试**: 使用 `ApiLoginService` 和 `ApiNotificationService` 连接真实后端
3. **切换环境**: 通过 `ServiceFactory.setServiceMode()` 轻松切换测试环境

## 依赖项

- **Gson**: 用于 JSON 序列化和反序列化
  ```kotlin
  implementation("com.google.code.gson:gson:2.10.1")
  ```

## 未来改进

1. **WebSocket支持**: 实现实时通知推送
2. **请求缓存**: 添加响应缓存机制
3. **重试机制**: 网络失败时自动重试
4. **请求队列**: 管理并发请求
5. **Token刷新**: 自动刷新过期的Token
6. **离线支持**: 本地数据缓存和同步

## 联系方式

如有问题或建议，请联系开发团队。

---

**文档版本**: v1.0.0  
**最后更新**: 2025-12-24
