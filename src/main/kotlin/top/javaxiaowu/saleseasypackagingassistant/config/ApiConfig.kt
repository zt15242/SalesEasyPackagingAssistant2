package top.javaxiaowu.saleseasypackagingassistant.config

/**
 * API配置类
 * 集中管理所有API相关的配置信息
 */
object ApiConfig {
    
    /**
     * API基础域名
     * 可以通过环境变量或配置文件进行修改
     */
    var BASE_URL: String = "http://114.66.56.200:6002"
        private set
    
    /**
     * API路径前缀
     */
    private const val API_PREFIX = "/api"
    
    /**
     * 设置基础URL
     * @param url 新的基础URL
     */
    fun setBaseUrl(url: String) {
        BASE_URL = url.trimEnd('/')
    }
    
    /**
     * 获取完整的API URL
     * @param endpoint API端点路径
     * @return 完整的URL
     */
    fun getApiUrl(endpoint: String): String {
        val cleanEndpoint = endpoint.trimStart('/')
        return "$BASE_URL$API_PREFIX/$cleanEndpoint"
    }
    
    /**
     * 认证相关API端点
     */
    object Auth {
        const val LOGIN = "auth/login"
        const val REGISTER = "auth/register"
        const val ME = "auth/me"
        const val LOGOUT = "auth/logout"
    }
    
    /**
     * 用户管理API端点
     */
    object Users {
        const val LIST = "users/"
        const val DETAIL = "users/{user_id}"
        const val UPDATE = "users/{user_id}"
        const val DELETE = "users/{user_id}"
        const val CHANGE_PASSWORD = "users/change-password"
        const val STATS = "users/me/stats"
    }
    
    /**
     * 项目管理API端点
     */
    object Projects {
        const val CREATE = "projects/"
        const val LIST = "projects/"
        const val DETAIL = "projects/{project_id}"
        const val UPDATE = "projects/{project_id}"
        const val ADD_MEMBER = "projects/{project_id}/members/{user_id}"
        const val REMOVE_MEMBER = "projects/{project_id}/members/{user_id}"
    }
    
    /**
     * 任务管理API端点
     */
    object Tasks {
        const val CREATE = "tasks/"
        const val LIST = "tasks/"
        const val DETAIL = "tasks/{task_id}"
        const val UPDATE = "tasks/{task_id}"
        const val DELETE = "tasks/{task_id}"
    }
    
    /**
     * BUG管理API端点
     */
    object Bugs {
        const val CREATE = "bugs/"
        const val LIST = "bugs/"
        const val DETAIL = "bugs/{bug_id}"
        const val UPDATE = "bugs/{bug_id}"
        const val DELETE = "bugs/{bug_id}"
        const val ATTACHMENTS = "bugs/{bug_id}/attachments"
        const val RETEST = "bugs/{bug_id}/retest"
        const val CLOSE = "bugs/{bug_id}/close"
        const val REOPEN = "bugs/{bug_id}/reopen"
        const val COMMENTS = "bugs/{bug_id}/comments"
    }
    
    /**
     * 通知管理API端点
     */
    object Notifications {
        const val LIST = "notifications/"
        const val UNREAD_COUNT = "notifications/unread/count"
        const val MARK_READ = "notifications/{notification_id}/read"
        const val MARK_ALL_READ = "notifications/read-all"
        const val DELETE = "notifications/{notification_id}"
    }
    
    /**
     * 代码发布管理API端点
     */
    object Deployments {
        const val CREATE = "deployments/"
        const val LIST = "deployments/"
        const val DETAIL = "deployments/{deployment_id}"
        const val UPDATE = "deployments/{deployment_id}"
        const val DELETE = "deployments/{deployment_id}"
        const val UPLOAD_VERSION = "deployments/{deployment_id}/versions"
        const val VERSION_DETAIL = "deployments/{deployment_id}/versions/{version}"
        const val REVIEW = "deployments/{deployment_id}/versions/{version}/review"
        const val DEPLOY = "deployments/{deployment_id}/versions/{version}/deploy"
        const val REANALYZE = "deployments/{deployment_id}/versions/{version}/reanalyze"
        const val COMPARE = "deployments/{deployment_id}/compare"
        const val REMOTE_PACKAGES = "deployments/remote-packages"
        const val MATCH_OR_CREATE = "deployments/match-or-create-package"
    }
    
    /**
     * 环境管理API端点
     */
    object Environments {
        const val CREATE = "environments/{project_id}"
        const val LIST = "environments/project/{project_id}"
        const val UPDATE = "environments/{environment_id}"
        const val DELETE = "environments/{environment_id}"
        const val LOGIN = "environments/{environment_id}/login"
        const val COOKIES = "environments/{environment_id}/cookies"
    }
    
    /**
     * 统计看板API端点
     */
    object Statistics {
        const val PROJECT = "statistics/projects/{project_id}"
        const val OVERVIEW = "statistics/overview"
    }
    
    /**
     * 知识库管理API端点
     */
    object Knowledge {
        const val UPLOAD = "knowledge/"
        const val LIST = "knowledge/"
        const val DETAIL = "knowledge/{knowledge_id}"
        const val UPDATE = "knowledge/{knowledge_id}"
        const val DELETE = "knowledge/{knowledge_id}"
        const val CATEGORIES = "knowledge/categories/list"
        const val TAGS = "knowledge/tags/list"
        const val STATISTICS = "knowledge/statistics/{project_id}"
    }
    
    /**
     * AI配置管理API端点
     */
    object AIConfig {
        const val CREATE = "ai-config/"
        const val LIST = "ai-config/"
        const val ACTIVE = "ai-config/active"
        const val DETAIL = "ai-config/{config_id}"
        const val UPDATE = "ai-config/{config_id}"
        const val DELETE = "ai-config/{config_id}"
        const val TEST = "ai-config/{config_id}/test"
    }
    
    /**
     * 健康检查API端点
     */
    object Health {
        const val HEALTH = "health"
        const val READY = "ready"
        const val LIVE = "live"
    }
    
    /**
     * HTTP请求超时配置（毫秒）
     */
    object Timeout {
        const val CONNECT = 10000L
        const val READ = 30000L
        const val WRITE = 30000L
    }
}
