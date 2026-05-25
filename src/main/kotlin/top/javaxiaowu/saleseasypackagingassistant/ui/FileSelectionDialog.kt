package top.javaxiaowu.saleseasypackagingassistant.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.CheckboxTreeListener
import top.javaxiaowu.saleseasypackagingassistant.utils.ZipPackageUtil
import top.javaxiaowu.saleseasypackagingassistant.utils.ScriptTriggerXmlGenerator
import javax.swing.*
import java.io.File
import java.nio.file.Paths

class FileSelectionDialog(val project: Project, private val initialSelection: List<VirtualFile>? = null) : DialogWrapper(project, true) {
    private var tree: CheckboxTree? = null
    private var rootNode: CheckedTreeNode? = null
    private var selectedFiles = mutableSetOf<VirtualFile>()
    private val checkedNodes = mutableSetOf<CheckedTreeNode>()

    init {
        title = "Select Files to Package"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // 尝试多种方式获取项目根目录
        var projectRoot = project.baseDir
        println("DEBUG: project.baseDir = $projectRoot")
        
        if (projectRoot == null) {
            projectRoot = project.projectFile?.parent
            println("DEBUG: project.projectFile?.parent = $projectRoot")
        }
        
        if (projectRoot == null) {
            // 尝试从 project.basePath 获取
            val basePath = project.basePath
            println("DEBUG: project.basePath = $basePath")
            if (basePath != null) {
                val vfsManager = com.intellij.openapi.vfs.VirtualFileManager.getInstance()
                projectRoot = vfsManager.findFileByNioPath(java.nio.file.Paths.get(basePath))
                println("DEBUG: VirtualFileManager.findFileByNioPath result = $projectRoot")
            }
        }
        
        println("DEBUG: Final projectRoot = $projectRoot")
        
        if (projectRoot == null) {
            val errorPanel = JPanel()
            errorPanel.layout = BoxLayout(errorPanel, BoxLayout.Y_AXIS)
            val errorLabel = JLabel("Error: Cannot find project root directory")
            errorLabel.foreground = java.awt.Color.RED
            errorPanel.add(errorLabel)
            return errorPanel
        }
        
        try {
            // 创建树
            rootNode = CheckedTreeNode(projectRoot)
            rootNode?.isChecked = false  // 默认不选中
            tree = CheckboxTree(FileTreeNodeRenderer(), rootNode!!)  // 使用 !! 确保不为 null
            
            // 监听复选框状态变化
            val currentTree = tree
            if (currentTree != null) {
                currentTree.addCheckboxTreeListener(object : CheckboxTreeListener {
                    override fun nodeStateChanged(node: CheckedTreeNode) {
                        if (node.isChecked) {
                            checkedNodes.add(node)
                        } else {
                            checkedNodes.remove(node)
                        }
                    }
                })
            }
            
            // 构建树结构
            val currentRoot = rootNode
            if (currentRoot != null) {
                buildTreeStructure(currentRoot, projectRoot)
                println("DEBUG: Tree structure built, rootNode has ${currentRoot.childCount} children")
                // 强制刷新树的模型
                (tree?.model as? javax.swing.tree.DefaultTreeModel)?.reload(currentRoot)
                println("DEBUG: Tree model reloaded")
                // 不需要自动展开根节点，让用户手动选择
                
                // 如果有初始选择,自动勾选
                if (!initialSelection.isNullOrEmpty()) {
                    println("DEBUG: Initial selection: ${initialSelection.map { it.path }}")
                    println("DEBUG: Total nodes to check...")
                    selectInitialFiles(currentRoot, initialSelection)
                    println("DEBUG: After selectInitialFiles, checkedNodes size = ${checkedNodes.size}")
                    // CheckboxTree.setNodeState 会自动处理 UI 更新,不需要手动 reload
                }
            }
            
            val scrollPane = JBScrollPane(tree)
            scrollPane.preferredSize = java.awt.Dimension(400, 500)
            panel.add(scrollPane)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorLabel = JLabel("Error: ${e.message}")
            errorLabel.foreground = java.awt.Color.RED
            panel.add(errorLabel)
        }
        
        return panel
    }

    private fun buildTreeStructure(parentNode: CheckedTreeNode, virtualFile: VirtualFile) {
        if (!virtualFile.isDirectory) return
        
        try {
            println("DEBUG: Processing directory: ${virtualFile.path}")
            val children = virtualFile.children
            println("DEBUG: Found ${children.size} children in ${virtualFile.name}")
            
            val sortedChildren = children.sortedBy { !it.isDirectory }.toMutableList()
            for (child in sortedChildren) {
                println("DEBUG: Processing child: ${child.name} (isDirectory: ${child.isDirectory})")
                
                // 跳过 .idea, .gradle, build 等目录
                if (child.isDirectory && (child.name.startsWith(".") || child.name == "build")) {
                    println("DEBUG: Skipping ${child.name}")
                    continue
                }
                
                val childNode = CheckedTreeNode(child)
                childNode.isChecked = false  // 默认不选中
                parentNode.add(childNode)
                println("DEBUG: Added node for ${child.name}, parentNode now has ${parentNode.childCount} children")
                
                if (child.isDirectory) {
                    buildTreeStructure(childNode, child)
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in buildTreeStructure: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun doOKAction() {
        // 收集已选中的文件
        selectedFiles.clear()
        for (node in checkedNodes) {
            val file = node.userObject as? VirtualFile
            if (file != null) {
                addFileAndChildren(file, selectedFiles)
            }
        }
        
        if (selectedFiles.isEmpty()) {
            Messages.showWarningDialog("Please select at least one file", "No Selection")
            return
        }

        // 检查是否需要创建 scriptTrigger.xml
        val selectedFileNames = selectedFiles.filter { !it.isDirectory }.map { it.name }
        val needsScriptTrigger = selectedFileNames.any { fileName ->
            ScriptTriggerXmlGenerator.shouldCreateScriptTrigger(fileName)
        }
        val hasScriptTriggerXml = selectedFileNames.contains("scriptTrigger.xml")
        
        var scriptTriggerConfigs: List<ScriptTriggerConfig>? = null
        
        if (needsScriptTrigger && !hasScriptTriggerXml) {
            // 找出需要配置的文件（仅文件）
            val filesToConfigure = selectedFileNames.filter { fileName ->
                ScriptTriggerXmlGenerator.shouldCreateScriptTrigger(fileName) && fileName != "scriptTrigger.xml"
            }
            
            val configs = mutableListOf<ScriptTriggerConfig>()
            for (fileToConfig in filesToConfigure) {
                // 找到对应的 VirtualFile，用于获取完整路径
                val virtualFile = selectedFiles.find { it.name == fileToConfig && !it.isDirectory }
                val dialog = ScriptTriggerConfigDialog(project, fileToConfig, virtualFile, project.baseDir)
                if (dialog.showAndGet()) {
                    dialog.config?.let { configs.add(it) }
                } else {
                    // 用户取消了配置
                    return
                }
            }
            
            if (configs.isNotEmpty()) {
                scriptTriggerConfigs = configs
            }
        }

        // 使用文件保存对话框
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        val saveFolder = com.intellij.openapi.fileChooser.FileChooser.chooseFile(descriptor, project, null)
        
        if (saveFolder != null) {
            // 弹出输入对话框让用户输入文件名
            var fileName = Messages.showInputDialog(
                project,
                "Enter ZIP file name (without .zip extension):",
                "Package Name",
                Messages.getQuestionIcon(),
                "package",
                null
            )
            
            if (fileName != null) {
                fileName = fileName.trim()
                if (fileName.isEmpty()) {
                    fileName = "package"
                }
                
                try {
                    val baseDir = project.baseDir
                    if (baseDir == null) {
                        Messages.showErrorDialog("Cannot determine project root directory", "Error")
                    } else {
                        val zipFileName = if (fileName.endsWith(".zip")) fileName else "$fileName.zip"
                        val zipFile = File(saveFolder.path, zipFileName)
                        val projectRootPath = project.basePath ?: baseDir.path
                        ZipPackageUtil.createZip(selectedFiles.toList(), zipFile, baseDir, scriptTriggerConfigs, projectRootPath)
                        Messages.showInfoMessage("Package created successfully: ${zipFile.absolutePath}", "Success")
                    }
                } catch (e: Exception) {
                    Messages.showErrorDialog("Error creating package: ${e.message}", "Error")
                }
            }
        }
        
        super.doOKAction()
    }
    
    private fun collectSelectedFiles(node: CheckedTreeNode) {
        if (node.isChecked) {
            val file = node.userObject as? VirtualFile
            if (file != null) {
                selectedFiles.add(file)
            }
        }
        
        // 递归检查子节点
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                collectSelectedFiles(child)
            }
        }
    }
    
    private fun addFileAndChildren(file: VirtualFile, result: MutableSet<VirtualFile>) {
        result.add(file)
        if (file.isDirectory) {
            file.children.forEach { child ->
                addFileAndChildren(child, result)
            }
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tree ?: JPanel()
    }
    
    private fun selectInitialFiles(parentNode: CheckedTreeNode, filesToSelect: List<VirtualFile>) {
        // 递归遍历树节点,找到对应的文件并勾选
        val parentFile = parentNode.userObject as? VirtualFile
        if (parentFile != null) {
            // 标准化路径以便比较
            val parentPath = parentFile.path.replace("\\", "/")
            println("DEBUG: Checking parentPath = $parentPath")
            
            for (selectFile in filesToSelect) {
                val selectPath = selectFile.path.replace("\\", "/")
                println("DEBUG: Against selectPath = $selectPath")
                
                // 精确匹配路径
                if (parentPath == selectPath) {
                    println("DEBUG: MATCH FOUND! Setting ${parentFile.name} as checked")
                    // 使用 CheckboxTree 的 setNodeState 方法来设置节点状态
                    tree?.setNodeState(parentNode, true)
                    checkedNodes.add(parentNode)
                    println("DEBUG: Node ${parentFile.name} state set via CheckboxTree")
                    
                    // 自动展开该节点及其所有父节点
                    expandNodePath(parentNode)
                    println("DEBUG: Node path expanded for ${parentFile.name}")
                    break
                }
            }
        }
        
        // 递归检查子节点
        for (i in 0 until parentNode.childCount) {
            val child = parentNode.getChildAt(i) as? CheckedTreeNode ?: continue
            selectInitialFiles(child, filesToSelect)
        }
    }
    
    private fun expandNodePath(node: CheckedTreeNode) {
        // 展开从根节点到当前节点的所有父节点
        val path = mutableListOf<CheckedTreeNode>()
        var current: CheckedTreeNode? = node
        
        // 收集从当前节点到根节点的路径
        while (current != null) {
            path.add(0, current)
            current = current.parent as? CheckedTreeNode
        }
        
        // 展开路径中的每个节点
        for (pathNode in path) {
            val treePath = javax.swing.tree.TreePath(pathNode.path)
            tree?.expandPath(treePath)
        }
    }
}
