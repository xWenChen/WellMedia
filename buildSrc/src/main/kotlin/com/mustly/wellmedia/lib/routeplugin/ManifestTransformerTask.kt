package com.mustly.wellmedia.lib.routeplugin

import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream

// 定义 Manifest 编辑任务，然后将任务添加到路由插件中
abstract class ManifestTransformerTask : DefaultTask() {
    @get:InputFile abstract val srcManifest: RegularFileProperty
    @get:OutputFile abstract val updatedManifest: RegularFileProperty

    // 该 task 要做的事
    @TaskAction
    fun taskAction() {
        val input = srcManifest.get().asFile
        val output = updatedManifest.get().asFile

        val document = SAXReader().read(input)

        // application 节点下添加属性
        document.findFirstAppNode()?.addElement("meta-data")?.apply {
            addAttribute("android:name", project.name)
            addAttribute("android:value", "com.mustly.wellmedia.lib.commonlib.route.moduleName")
        }

        // 修改后的 xml 内容，写入文件
        XMLWriter(FileOutputStream(output), OutputFormat.createPrettyPrint().apply {
            encoding = "UTF-8"
        }).apply {
            write(document)
            close()
        }
    }

    private fun Document.findFirstAppNode(): Element? {
        /*return rootElement.element("application").elements()?.run {
            if (isNullOrEmpty()) {
                null
            } else {
                // 在 application 下的第一个节点添加内容
                get(0) as? Element
            }
        }*/

        // 返回 application 节点
        return rootElement.element("application")
    }
}