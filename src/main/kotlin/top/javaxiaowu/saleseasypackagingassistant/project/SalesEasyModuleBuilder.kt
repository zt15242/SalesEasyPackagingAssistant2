package top.javaxiaowu.saleseasypackagingassistant.project

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * SalesEasy 项目模块构建器
 * 用于创建自定义的项目模板
 */
class SalesEasyModuleBuilder : ModuleBuilder() {
    
    // OAuth 配置信息
    var userName: String = ""
    var password: String = ""
    var securityCode: String = ""
    var clientId: String = ""
    var clientSecret: String = ""
    var domain: String = ""
    
    // GroupId 和 ArtifactId
    var groupId: String = "other.xsy"
    var artifactId: String = "jinpan"
    
    override fun getModuleType(): ModuleType<*> {
        return SalesEasyModuleType.INSTANCE
    }
    
    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: com.intellij.openapi.Disposable): ModuleWizardStep {
        // 返回自定义配置步骤
        return SalesEasyConfigStep(this)
    }

    
    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val contentEntry = doAddContentEntry(modifiableRootModel) ?: return
        val contentRoot = contentEntry.file ?: return
        
        // 创建 pom.xml (Maven 项目配置)
        createPomXml(contentRoot)
        
        // 创建 .gitignore
        createGitignore(contentRoot)
        
        // 创建项目结构
        createProjectStructure(contentRoot)
        
        // 创建配置文件
        createOAuthConfigFile(contentRoot)
        
        // 创建 scriptTrigger.xml
        createScriptTriggerXml(contentRoot)
        
        // 刷新文件系统以确保可以找到新目录
        contentRoot.refresh(false, true)
        
        // 标记 Source Roots
        val srcMainJava = contentRoot.findFileByRelativePath("src/main/java")
        if (srcMainJava != null) {
            contentEntry.addSourceFolder(srcMainJava, false)
        }
        
        val srcMainResources = contentRoot.findFileByRelativePath("src/main/resources")
        if (srcMainResources != null) {
             try {
                 contentEntry.addSourceFolder(srcMainResources, false) 
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }
        
        // Auto-add as Maven Project
        val pomFile = contentRoot.findChild("pom.xml")
        if (pomFile != null) {
            val project = modifiableRootModel.project
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                try {
                    // 使用完整类名以避免导入问题，或者你可以添加导入
                    val mavenManager = org.jetbrains.idea.maven.project.MavenProjectsManager.getInstance(project)
                    if (!mavenManager.isManagedFile(pomFile)) {
                        mavenManager.addManagedFiles(listOf(pomFile))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * 创建 pom.xml 文件
     */
    private fun createPomXml(contentRoot: VirtualFile) {
        try {
            val pomFile = File(contentRoot.path, "pom.xml")
            
            // 使用 HTTPS schema
            val content = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>$groupId</groupId>
    <artifactId>$artifactId</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>$artifactId</name>
    <description>SalesEasy Project - $artifactId</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Add your dependencies here -->
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>src/main/java</directory>
                <includes>
                    <include>**/*.xml</include>
                    <include>**/*.properties</include>
                </includes>
                <filtering>false</filtering>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"""
            
            pomFile.writeText(content)
            println("Created pom.xml: ${pomFile.absolutePath}")
            
            // 刷新文件系统
            contentRoot.refresh(false, true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    
    /**
     * 创建项目目录结构
     */
    private fun createProjectStructure(contentRoot: VirtualFile) {
        try {
            val basePath = contentRoot.path
            
            // 创建 src/main/java 目录
            val srcMainJava = File(basePath, "src/main/java")
            srcMainJava.mkdirs()
            
            // 创建 src/main/resources 目录
            val srcMainResources = File(basePath, "src/main/resources")
            srcMainResources.mkdirs()
            
            // 创建包路径: other/xsy/jinpan
            val packagePath = groupId.replace(".", "/") + "/" + artifactId
            val basePackageDir = File(srcMainJava, packagePath)
            basePackageDir.mkdirs()
            
            // 创建子目录
            val subDirs = listOf("event", "trigger", "service", "futuretask", "util", "html")
            for (subDir in subDirs) {
                val dir = File(basePackageDir, subDir)
                dir.mkdirs()
                println("Created directory: ${dir.absolutePath}")
            }
            
            // 刷新文件系统
            contentRoot.refresh(false, true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 创建 oauthConfig.properties 文件
     */
    private fun createOAuthConfigFile(contentRoot: VirtualFile) {
        try {
            // 生成到项目根目录，而不是 resources 目录
            val configFile = File(contentRoot.path, "oauthConfig.properties")
            
            val content = buildString {
                appendLine("# OAuth Configuration")
                appendLine("# Generated by SalesEasy Packaging Assistant")
                appendLine()
                appendLine("userName=$userName")
                appendLine("password=$password")
                appendLine("securityCode=$securityCode")
                appendLine("clientId=$clientId")
                appendLine("clientSecret=$clientSecret")
                appendLine("domain=$domain")
            }
            
            configFile.writeText(content)
            println("Created oauthConfig.properties: ${configFile.absolutePath}")
            
            // 刷新文件系统
            contentRoot.refresh(false, true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 创建 scriptTrigger.xml 文件
     */
    private fun createScriptTriggerXml(contentRoot: VirtualFile) {
        try {
            val srcMainJava = File(contentRoot.path, "src/main/java")
            srcMainJava.mkdirs()
            
            val xmlFile = File(srcMainJava, "scriptTrigger.xml")
            
            val content = """<?xml version="1.0" encoding="utf-8"?>
<configs>
    <config>

    </config>
</configs>
"""
            
            xmlFile.writeText(content)
            println("Created scriptTrigger.xml: ${xmlFile.absolutePath}")
            
            // 刷新文件系统
            contentRoot.refresh(false, true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun getBuilderId(): String {
        return "saleseasy.module.builder"
    }
    
    override fun getPresentableName(): String {
        return "SalesEasy Project"
    }
    
    override fun getDescription(): String {
        return "Create a SalesEasy project with OAuth configuration and standard directory structure"
    }
    
    override fun getGroupName(): String {
        return "SalesEasy"
    }
    
    /**
     * 创建 .gitignore 文件
     */
    private fun createGitignore(contentRoot: VirtualFile) {
        try {
            val gitignoreFile = File(contentRoot.path, ".gitignore")
            
            val content = """# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# IntelliJ IDEA
.idea/
*.iws
*.iml
*.ipr

# Eclipse
.classpath
.project
.settings/

# NetBeans
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/

# VS Code
.vscode/

# macOS
.DS_Store

# Windows
Thumbs.db
ehthumbs.db
Desktop.ini

# Log files
*.log

# Temporary files
*.tmp
*.bak
*.swp
*~

# OAuth Configuration (sensitive)
# Uncomment if you don't want to commit OAuth credentials
# oauthConfig.properties
"""
            
            gitignoreFile.writeText(content)
            println("Created .gitignore: ${gitignoreFile.absolutePath}")
            
            // 刷新文件系统
            contentRoot.refresh(false, true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
