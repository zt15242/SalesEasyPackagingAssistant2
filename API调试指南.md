# API调试指南

## 当前问题

运行时出现错误：
```
Input should be a valid dictionary or object to extract fields from, input_type=str
```

## 调试步骤

### 1. 查看控制台输出

现在代码中已添加调试日志，运行插件后请查看 IntelliJ IDEA 的控制台输出：

```
API Request: POST http://114.66.56.200:6002/api/auth/login
API Response [200]: {实际响应内容}
```

### 2. 检查响应格式

根据控制台输出，检查API返回的内容：

**预期格式**（JSON对象）:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**可能的错误格式**:
- 返回的是字符串而不是JSON对象
- 返回的是HTML错误页面
- 返回的JSON结构不匹配

### 3. 常见问题排查

#### 问题1: API域名错误

**症状**: 返回HTML错误页面或404

**解决方法**:
```kotlin
// 检查 ApiConfig.kt 中的域名配置
var BASE_URL: String = "http://114.66.56.200:6002"  // 确认这个地址正确
```

测试方法：
```bash
# 在浏览器或curl中测试
curl -X POST http://114.66.56.200:6002/api/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=test&password=test"
```

#### 问题2: Content-Type不匹配

**症状**: 后端返回400错误或格式错误

**当前配置**:
```kotlin
connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
```

**可能需要改为**:
```kotlin
connection.setRequestProperty("Content-Type", "application/json")
```

#### 问题3: 请求格式不对

**当前使用**: Form表单格式
```
username=xiaowu&password=wuyijun123
```

**可能需要**: JSON格式
```json
{
  "username": "xiaowu",
  "password": "wuyijun123"
}
```

### 4. 临时解决方案

如果API格式确实不匹配，可以临时使用Mock服务：

```kotlin
// 在初始化时设置
ServiceFactory.setServiceMode(useMock = true)
```

### 5. 修改登录方式

如果后端需要JSON格式而不是表单格式，修改 `LoginService.kt`:

```kotlin
override fun login(username: String, password: String): LoginResult {
    try {
        // 使用JSON格式而不是表单格式
        val loginRequest = mapOf(
            "username" to username,
            "password" to password
        )
        
        val response = HttpClient.post(  // 改用post而不是postForm
            endpoint = ApiConfig.Auth.LOGIN,
            body = loginRequest,
            requireAuth = false,
            responseType = LoginResponse::class.java
        )
        
        // ... 其余代码
    }
}
```

## 调试输出示例

### 成功的情况

```
API Request: POST http://114.66.56.200:6002/api/auth/login
API Response [200]: {"access_token":"eyJhbGc...","token_type":"bearer"}
```

### 失败的情况

**情况1: 返回HTML**
```
API Request: POST http://114.66.56.200:6002/api/auth/login
API Response [404]: <!DOCTYPE html><html>...
JSON解析失败: Expected BEGIN_OBJECT but was STRING
```

**情况2: 返回错误信息**
```
API Request: POST http://114.66.56.200:6002/api/auth/login
API Response [422]: {"detail":[{"type":"model_attributes_type","loc":["body"],"msg":"Input should be a valid dictionary..."}]}
```

**情况3: 网络错误**
```
网络请求异常: Connection refused
```

## 下一步操作

1. **运行插件并查看控制台输出**
2. **将控制台输出发送给我**，包括：
   - API Request 行
   - API Response 行
   - 任何错误信息

3. **根据输出调整代码**

## 可能的修复方案

### 方案1: 改用JSON格式登录

修改 `ApiConfig.kt` 添加配置：
```kotlin
object ApiConfig {
    var USE_JSON_LOGIN = true  // 是否使用JSON格式登录
}
```

修改 `LoginService.kt`:
```kotlin
val response = if (ApiConfig.USE_JSON_LOGIN) {
    HttpClient.post(
        endpoint = ApiConfig.Auth.LOGIN,
        body = mapOf("username" to username, "password" to password),
        requireAuth = false,
        responseType = LoginResponse::class.java
    )
} else {
    HttpClient.postForm(
        endpoint = ApiConfig.Auth.LOGIN,
        formData = mapOf("username" to username, "password" to password),
        requireAuth = false,
        responseType = LoginResponse::class.java
    )
}
```

### 方案2: 添加响应适配器

如果后端返回的字段名不一致，添加字段映射：

```kotlin
private data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    
    @SerializedName("token_type")
    val tokenType: String
)
```

需要添加Gson的SerializedName注解支持。

### 方案3: 使用更宽松的解析

```kotlin
try {
    val data = gson.fromJson(response, responseType)
    // ...
} catch (e: Exception) {
    // 尝试其他解析方式
    try {
        val jsonObject = gson.fromJson(response, JsonObject::class.java)
        // 手动提取字段
    } catch (e2: Exception) {
        // 返回错误
    }
}
```

## 联系支持

如果问题仍未解决，请提供：
1. 完整的控制台输出
2. API文档中登录接口的说明
3. 使用curl或Postman测试的结果

---

**更新时间**: 2025-12-24  
**版本**: v1.2.0
