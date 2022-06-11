package com.mustly.wellmedia.lib.routeplugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.*
import com.android.repository.Revision
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.dsl.ScriptHandler

// 代码参考自 Router 框架
class RoutePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.hasPlugin(AppPlugin::class.java)                   // com.android.application
            && !project.hasPlugin(LibraryPlugin::class.java)           // com.android.library
            && !project.hasPlugin(TestPlugin::class.java)              // com.android.test
            && !project.hasPlugin(DynamicFeaturePlugin::class.java)    // com.android.dynamic-feature, added in 3.2
        ) {
            throw GradleException("android plugin required.")
        }

        val android: BaseExtension = project.extensions.getByName("android") as BaseExtension

        val options = mutableMapOf("moduleName" to project.name)

        // 为注解添加选项
        android.defaultConfig.javaCompileOptions.annotationProcessorOptions.arguments(options)
        android.productFlavors.forEach {
            it.javaCompileOptions.annotationProcessorOptions.arguments(options)
        }

        // 注册 task
        val appVersion = project.rootProject.configurations.getByName(ScriptHandler.CLASSPATH_CONFIGURATION)
            .resolvedConfiguration.firstLevelModuleDependencies.find {
                it.moduleGroup == "com.android.tools.build" && it.moduleName == "gradle"
            }?.moduleVersion ?: throw GradleException("project version is null. name = ${project.name}")

        val revision = Revision.parseRevision(appVersion, Revision.Precision.PREVIEW)

        when {
            revision.major > 4 -> {
                v4_2(project)
            }
            revision.major == 4 && revision.minor >= 2 -> {
                v4_2(project)
            }
            revision.major == 4 && revision.minor == 1 -> {
                v4_2(project)
            }
            else -> {
                v4_2(project)
            }
        }
    }

    /**
     * 当 gradle 版本导入选项使用 compileOnly 时，需要加上版本兼容。
     * 如：compileOnly "com.android.tools.build:gradle:$gradleVersion"
     * 和 compileOnly "com.android.tools.build:gradle-api:$gradleVersion"
     *
     * components 代表着 app/lib 等，代表 Android 构建结果的大类
     * variant 代表构建的一个不同的应用版本，例如 build type + product flavor
     * artifacts 代表着产物，每个任务执行后都可以有产物。如 文件产物的代表：FileCollection
     * */
    private fun  v4_2(project: Project) {
        // com.android.build.api.extension.AndroidComponentsExtension
        val androidComponents = project.extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>
        androidComponents.onVariants { variant -> // com.android.build.api.variant.Variant
            registerAndBindTask(project, variant)
        }
    }

    private fun registerAndBindTask(project: Project, variant: Variant) {
        // 项目中注册任务
        val taskProvider = project.tasks.register(
            "process${variant.name.localCapitalize()}RouteManifest",
            ManifestTransformerTask::class.java
        )
        // 将任务和具体的产入产出挂钩，方便转换
        variant.artifacts.use(taskProvider).wiredWithFiles(
            { it.getMergedManifest() }, { it.getUpdatedManifest() }
        ).toTransform(SingleArtifact.MERGED_MANIFEST)
    }

    private fun String.localCapitalize() = capitalize()
}

private fun Project.hasPlugin(pluginClz: Class<out Plugin<Project>>) = plugins.hasPlugin(pluginClz)
