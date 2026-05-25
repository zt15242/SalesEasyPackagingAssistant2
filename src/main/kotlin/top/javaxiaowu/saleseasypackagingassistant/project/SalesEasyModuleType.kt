package top.javaxiaowu.saleseasypackagingassistant.project

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import javax.swing.Icon

/**
 * SalesEasy 模块类型
 */
class SalesEasyModuleType : ModuleType<SalesEasyModuleBuilder>(ID) {
    
    companion object {
        const val ID = "SALESEASY_MODULE_TYPE"
        
        val INSTANCE: SalesEasyModuleType
            get() = ModuleTypeManager.getInstance().findByID(ID) as SalesEasyModuleType
    }
    
    override fun createModuleBuilder(): SalesEasyModuleBuilder {
        return SalesEasyModuleBuilder()
    }
    
    override fun getName(): String {
        return "SalesEasy Project"
    }
    
    override fun getDescription(): String {
        return "SalesEasy project with OAuth configuration and standard directory structure"
    }
    
    override fun getNodeIcon(isOpened: Boolean): Icon {
        return AllIcons.Nodes.Module
    }
}
