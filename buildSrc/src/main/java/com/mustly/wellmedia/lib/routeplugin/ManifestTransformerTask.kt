package com.mustly.wellmedia.lib.routeplugin

import org.dom4j.Attribute
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import javax.inject.Inject
import javax.xml.parsers.SAXParser

// 定义 Manifest 编辑任务，然后将任务添加到路由插件中
abstract class ManifestTransformerTask @Inject constructor() : DefaultTask() {

    // 该 task 要做的事
    @TaskAction
    fun taskAction() {
        val input = getMergedManifest().get().asFile
        val output = getUpdatedManifest().get().asFile

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

    @InputFile
    abstract fun getMergedManifest(): RegularFileProperty

    @OutputFile
    abstract fun getUpdatedManifest(): RegularFileProperty

    private fun Document.findFirstAppNode(): Element? {
        return rootElement.element("application").elements()?.run {
            if (isNullOrEmpty()) {
                null
            } else {
                get(0) as? Element
            }
        }
    }
}