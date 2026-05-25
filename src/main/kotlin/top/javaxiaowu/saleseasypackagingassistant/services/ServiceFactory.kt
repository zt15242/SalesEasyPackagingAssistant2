package top.javaxiaowu.saleseasypackagingassistant.services

/**
 * 服务工厂类
 * 用于创建和管理服务实例
 */
object ServiceFactory {
    
    /**
     * 是否使用模拟服务
     * 设置为 false 使用真实的 API 服务
     * 设置为 true 使用模拟服务（用于开发和测试）
     */
    var useMockServices: Boolean = false
    
    private var loginServiceInstance: LoginService? = null
    private var notificationServiceInstance: NotificationService? = null
    
    /**
     * 获取登录服务实例
     */
    fun getLoginService(): LoginService {
        if (loginServiceInstance == null) {
            loginServiceInstance = if (useMockServices) {
                MockLoginService()
            } else {
                ApiLoginService()
            }
        }
        return loginServiceInstance!!
    }
    
    /**
     * 获取通知服务实例
     */
    fun getNotificationService(): NotificationService {
        if (notificationServiceInstance == null) {
            notificationServiceInstance = if (useMockServices) {
                MockNotificationService()
            } else {
                ApiNotificationService()
            }
        }
        return notificationServiceInstance!!
    }
    
    /**
     * 重置所有服务实例
     * 用于切换服务模式或重新初始化
     */
    fun resetServices() {
        loginServiceInstance = null
        notificationServiceInstance = null
    }
    
    /**
     * 设置服务模式
     * @param useMock 是否使用模拟服务
     */
    fun setServiceMode(useMock: Boolean) {
        if (useMockServices != useMock) {
            useMockServices = useMock
            resetServices()
        }
    }
}
