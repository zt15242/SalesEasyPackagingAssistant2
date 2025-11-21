package top.javaxiaowu.saleseasypackagingassistant.ui

import javax.swing.tree.DefaultMutableTreeNode

class CheckboxTreeNode(userObject: Any?) : DefaultMutableTreeNode(userObject) {
    var isChecked = false
    
    override fun toString(): String {
        return userObject?.toString() ?: ""
    }
}
