package top.javaxiaowu.saleseasypackagingassistant.services

import top.javaxiaowu.saleseasypackagingassistant.ui.NotificationItem
import top.javaxiaowu.saleseasypackagingassistant.ui.NotificationType
import com.google.gson.reflect.TypeToken
import top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.PaginatedResponse

/**
 * 通知服务接口
 * 后续需要实现真实的通知推送逻辑
 */
interface NotificationService {
    
    /**
     * 获取通知列表
     * @param page 页码
     * @param pageSize 每页数量
     * @return 通知列表
     */
    fun getNotifications(page: Int = 1, pageSize: Int = 20): List<NotificationItem>
    
    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     */
    fun markAsRead(notificationId: String)
    
    /**
     * 标记所有通知为已读
     */
    fun markAllAsRead()
    
    /**
     * 删除通知
     * @param notificationId 通知ID
     */
    fun deleteNotification(notificationId: String)
    
    /**
     * 清空所有通知
     */
    fun clearAllNotifications()
    
    /**
     * 获取未读通知数量
     * @return 未读数量
     */
    fun getUnreadCount(): Int
    
    /**
     * 订阅实时通知
     * @param callback 通知回调函数
     */
    fun subscribeNotifications(callback: (NotificationItem) -> Unit)
    
    /**
     * 取消订阅实时通知
     */
    fun unsubscribeNotifications()
}

/**
 * 通知服务的真实实现
 * 使用 API 进行通知管理
 */
class ApiNotificationService : NotificationService {
    
    private var notificationCallback: ((NotificationItem) -> Unit)? = null
    
    /**
     * 通知响应数据类
     */
    private data class NotificationResponse(
        val id: String,
        val title: String,
        val message: String,
        val type: String,
        val is_read: Boolean,
        val created_at: String
    )
    
    /**
     * 未读数量响应
     */
    private data class UnreadCountResponse(
        val count: Int
    )
    
    override fun getNotifications(page: Int, pageSize: Int): List<NotificationItem> {
        try {
            val queryParams = mapOf(
                "skip" to ((page - 1) * pageSize).toString(),
                "limit" to pageSize.toString()
            )
            
            val response = top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.get<Array<NotificationResponse>>(
                endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Notifications.LIST,
                queryParams = queryParams,
                requireAuth = true,
                responseType = Array<NotificationResponse>::class.java
            )
            
            if (response.success && response.data != null) {
                return response.data.map { notification ->
                    NotificationItem(
                        title = notification.title,
                        message = notification.message,
                        type = parseNotificationType(notification.type),
                        timestamp = parseTimestamp(notification.created_at),
                        isRead = notification.is_read
                    )
                }
            }
            
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }
    
    override fun markAsRead(notificationId: String) {
        try {
            val endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Notifications.MARK_READ
                .replace("{notification_id}", notificationId)
            
            top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.put(
                endpoint = endpoint,
                requireAuth = true,
                responseType = String::class.java
            )
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    override fun markAllAsRead() {
        try {
            top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.put(
                endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Notifications.MARK_ALL_READ,
                requireAuth = true,
                responseType = String::class.java
            )
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    override fun deleteNotification(notificationId: String) {
        try {
            val endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Notifications.DELETE
                .replace("{notification_id}", notificationId)
            
            top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.delete(
                endpoint = endpoint,
                requireAuth = true,
                responseType = String::class.java
            )
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    override fun clearAllNotifications() {
        // API可能不支持批量删除，需要逐个删除或者调用特定接口
        try {
            val notifications = getNotifications(1, 1000)
            // 这里简化处理，实际应该调用批量删除API
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    override fun getUnreadCount(): Int {
        try {
            val response = top.javaxiaowu.saleseasypackagingassistant.utils.HttpClient.get(
                endpoint = top.javaxiaowu.saleseasypackagingassistant.config.ApiConfig.Notifications.UNREAD_COUNT,
                requireAuth = true,
                responseType = UnreadCountResponse::class.java
            )
            
            return if (response.success && response.data != null) {
                response.data.count
            } else {
                0
            }
        } catch (e: Exception) {
            return 0
        }
    }
    
    override fun subscribeNotifications(callback: (NotificationItem) -> Unit) {
        // TODO: 实现 WebSocket 连接
        // 示例: webSocketClient.connect("ws://114.66.56.200:6002/ws/notifications") { ... }
        this.notificationCallback = callback
    }
    
    override fun unsubscribeNotifications() {
        // TODO: 关闭 WebSocket 连接
        this.notificationCallback = null
    }
    
    /**
     * 解析通知类型
     */
    private fun parseNotificationType(type: String): NotificationType {
        return when (type.lowercase()) {
            "info" -> NotificationType.INFO
            "success" -> NotificationType.SUCCESS
            "warning" -> NotificationType.WARNING
            "error" -> NotificationType.ERROR
            else -> NotificationType.INFO
        }
    }
    
    /**
     * 解析时间戳
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // 简化处理，实际应该使用日期解析库
            System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

/**
 * 通知服务的模拟实现（用于测试）
 * 保留用于开发和测试环境
 */
class MockNotificationService : NotificationService {
    
    private val notifications = mutableListOf<NotificationItem>()
    private val readNotifications = mutableSetOf<String>()
    private var notificationCallback: ((NotificationItem) -> Unit)? = null
    
    init {
        // 添加一些示例通知
        addMockNotifications()
    }
    
    override fun getNotifications(page: Int, pageSize: Int): List<NotificationItem> {
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, notifications.size)
        
        return if (startIndex < notifications.size) {
            notifications.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }
    
    override fun markAsRead(notificationId: String) {
        readNotifications.add(notificationId)
    }
    
    override fun markAllAsRead() {
        notifications.forEach { notification ->
            readNotifications.add(notification.hashCode().toString())
        }
    }
    
    override fun deleteNotification(notificationId: String) {
        notifications.removeIf { it.hashCode().toString() == notificationId }
    }
    
    override fun clearAllNotifications() {
        notifications.clear()
        readNotifications.clear()
    }
    
    override fun getUnreadCount(): Int {
        return notifications.count { !readNotifications.contains(it.hashCode().toString()) }
    }
    
    override fun subscribeNotifications(callback: (NotificationItem) -> Unit) {
        this.notificationCallback = callback
    }
    
    override fun unsubscribeNotifications() {
        this.notificationCallback = null
    }
    
    /**
     * 模拟推送新通知
     * 实际使用时,这个方法会被 WebSocket 消息触发
     */
    fun pushNotification(notification: NotificationItem) {
        notifications.add(0, notification)
        notificationCallback?.invoke(notification)
    }
    
    /**
     * 添加模拟通知数据
     */
    private fun addMockNotifications() {
        notifications.add(
            NotificationItem(
                title = "系统消息",
                message = "欢迎使用 SalesEasy 打包助手",
                type = NotificationType.INFO,
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = false
            )
        )
        
        notifications.add(
            NotificationItem(
                title = "功能提示",
                message = "右键文件夹选择 'Package Files' 即可打包",
                type = NotificationType.INFO,
                timestamp = System.currentTimeMillis() - 7200000,
                isRead = true
            )
        )
        
        notifications.add(
            NotificationItem(
                title = "版本更新",
                message = "插件已更新到 v1.0.0",
                type = NotificationType.SUCCESS,
                timestamp = System.currentTimeMillis() - 86400000,
                isRead = true
            )
        )
    }
}
