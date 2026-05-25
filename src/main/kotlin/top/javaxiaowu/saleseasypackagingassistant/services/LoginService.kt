package top.javaxiaowu.saleseasypackagingassistant.services

/**
 * 登录服务接口
 * 后续需要实现真实的登录逻辑
 */
interface LoginService {
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    fun login(username: String, password: String): LoginResult
    
    /**
     * 用户登出
     */
    fun logout()
    
    /**
     * 检查登录状态
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean
    
    /**
     * 获取当前用户信息
     * @return 用户信息,未登录返回 null
     */
    fun getCurrentUser(): UserInfo?
}

/**
 * 登录结果数据类
 */
data class LoginResult(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val userInfo: UserInfo? = null
)

/**
 * 用户信息数据类
 */
data class UserInfo(
    val userId: String,
    val username: String,
    val email: String? = null,
    val avatar: String? = null,
    val role: String? = null
)

/**
 * 登录服务的真实实现
 * 使用 API 进行用户认证
 */
class ApiLoginService : LoginService {
    
    private var currentUser: UserInfo? = null
    private var token: String? = null
    
    /**
     * 登录响应数据类
     */
    private data class LoginResponse(
        val access_token: String,
        val token_type: String
    )
    
    /**
     * 用户信息响应数据类
     */
    private data class UserResponse(
        val id: String,
        val username: String,
        val email: String?,
        val full_name: String?,
        val role: String?
    )
    
    override fun login(username: String, password: String): LoginResult {
        if (username.isEmpty() || password.isEmpty()) {
            return LoginResult(
                success = false,
                message = "用户名或密码不能为空"
            )
        }
        
        try {
            // 调用登录API - 使用JSON格式而不是表单格式
            val loginRequest = mapOf(
                "username" to username,
                "password" to password
            )
            
            val response = top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.post(
                endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Auth.LOGIN,
                body = loginRequest,
                requireAuth = false,
                responseType = LoginResponse::class.java
            )
            
            if (!response.success || response.data == null) {
                return LoginResult(
                    success = false,
                    message = response.message ?: "登录失败"
                )
            }
            
            // 保存token
            this.token = response.data.access_token
            top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.setAuthToken(this.token)
            
            // 获取用户信息
            val userInfoResponse = top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.get(
                endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Auth.ME,
                requireAuth = true,
                responseType = UserResponse::class.java
            )
            
            if (userInfoResponse.success && userInfoResponse.data != null) {
                val userData = userInfoResponse.data
                this.currentUser = UserInfo(
                    userId = userData.id,
                    username = userData.username,
                    email = userData.email,
                    avatar = null,
                    role = userData.role
                )
                
                return LoginResult(
                    success = true,
                    message = "登录成功",
                    token = this.token,
                    userInfo = this.currentUser
                )
            } else {
                // 登录成功但获取用户信息失败
                this.currentUser = UserInfo(
                    userId = "",
                    username = username,
                    email = null,
                    avatar = null,
                    role = null
                )
                
                return LoginResult(
                    success = true,
                    message = "登录成功",
                    token = this.token,
                    userInfo = this.currentUser
                )
            }
            
        } catch (e: Exception) {
            return LoginResult(
                success = false,
                message = "登录异常: ${e.message}"
            )
        }
    }
    
    override fun logout() {
        try {
            // 清除本地状态
            this.currentUser = null
            this.token = null
            top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.clearAuthToken()
        } catch (e: Exception) {
            // 忽略登出错误
        }
    }
    
    override fun isLoggedIn(): Boolean {
        return currentUser != null && token != null
    }
    
    override fun getCurrentUser(): UserInfo? {
        return currentUser
    }
}

/**
 * 登录服务的模拟实现（用于测试）
 * 保留用于开发和测试环境
 */
class MockLoginService : LoginService {
    
    private var currentUser: UserInfo? = null
    private var token: String? = null
    
    override fun login(username: String, password: String): LoginResult {
        // 模拟登录验证
        return if (username.isNotEmpty() && password.isNotEmpty()) {
            // 模拟成功
            val mockToken = "mock_token_${System.currentTimeMillis()}"
            val userInfo = UserInfo(
                userId = "user_${username.hashCode()}",
                username = username,
                email = "$username@example.com",
                role = "user"
            )
            
            this.currentUser = userInfo
            this.token = mockToken
            
            LoginResult(
                success = true,
                message = "登录成功",
                token = mockToken,
                userInfo = userInfo
            )
        } else {
            LoginResult(
                success = false,
                message = "用户名或密码不能为空"
            )
        }
    }
    
    override fun logout() {
        this.currentUser = null
        this.token = null
    }
    
    override fun isLoggedIn(): Boolean {
        return currentUser != null && token != null
    }
    
    override fun getCurrentUser(): UserInfo? {
        return currentUser
    }
}
