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
import java.util.*

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
                v7(project)
            }
            revision.major == 4 && revision.minor >= 2 -> {
                v7(project)
            }
            revision.major == 4 && revision.minor == 1 -> {
                v7(project)
            }
            else -> {
                v7(project)
            }
        }
    }

    // components 代表着 app/lib 等，代表 Android 构建结果的大类
    // variant 代表构建的一个不同的应用版本，例如 build type + product flavor
    // artifacts 代表着产物，每个任务执行后都可以有产物。如 文件产物的代表：FileCollection
    private fun  v7(project: Project) {
        // com.android.build.api.extension.AndroidComponentsExtension
        val androidComponents = project.extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>

        androidComponents.onVariants { variant -> // com.android.build.api.variant.Variant
            registerAndBindTask(project, variant)
        }
    }

    /**
     * 当 gradle 版本导入选项使用 compileOnly 时，需要加上版本兼容。
     * 如：compileOnly "com.android.tools.build:gradle:$gradleVersion"
     * 和 compileOnly "com.android.tools.build:gradle-api:$gradleVersion"
     * */
    /*void v4_2(Project project) {
        def manifestClz = Class.forName('com.android.build.api.artifact.ArtifactType$MERGED_MANIFEST')
        def instanceField = manifestClz.getField('INSTANCE')
        def artifactInstance = instanceField.get(null)
        // com.android.build.api.extension.AndroidComponentsExtension
        def androidComponents = project.extensions.getByName('androidComponents')
        androidComponents.onVariants(androidComponents.selector().all(), { variant -> // com.android.build.api.variant.Variant
            TaskProvider<ManifestTransformerTask> taskProvider = project.tasks.register(
                    "process${variant.name.capitalize()}RouterManifest", ManifestTransformerTask.class, project)
            variant.artifacts.use(taskProvider).wiredWithFiles({ it.mergedManifest },
                { it.updatedManifest }).toTransform(*//*ArtifactType.MERGED_MANIFEST.INSTANCE*//* artifactInstance)
        })
    }

    void v4_1(Project project) {
        /// BaseAppModuleExtension/LibraryExtension
        def android = project.extensions.getByName('android')
        if (project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(DynamicFeaturePlugin)) {
            // Way1:
//            AppExtension app = android
//            app.applicationVariants.all { ApplicationVariantImpl variant -> // com.android.build.gradle.internal.api.ApplicationVariantImpl
//                variant.outputs.all { BaseVariantOutput output -> // com.android.build.gradle.api.BaseVariantOutput
//                    output.processManifestProvider.get().doLast { ProcessMultiApkApplicationManifest task ->
//                        File manifestOutputFile = task.multiApkManifestOutputDirectory.get().file(SdkConstants.ANDROID_MANIFEST_XML).asFile
//                        ManifestTransformer.transform(project, manifestOutputFile, manifestOutputFile)
//                    }
//                }
//            }

            // Way2: Only support app module
            def manifestClz = Class.forName('com.android.build.api.artifact.ArtifactType$MERGED_MANIFEST')
            def instanceField = manifestClz.getField('INSTANCE')
            def artifactInstance = instanceField.get(null)

            android.onVariantProperties { *//*VariantPropertiesImpl*//* variant ->
                // ApplicationVariantPropertiesImpl/LibraryVariantPropertiesImpl
                TaskProvider<ManifestTransformerTask> taskProvider = project.tasks.register(
                        "process${variant.name.capitalize()}RouterManifest", ManifestTransformerTask.class, project)
                // variant.artifacts: com.android.build.api.artifact.Artifacts
                variant.artifacts.use(taskProvider).wiredWithFiles({ it.mergedManifest },
                    { it.updatedManifest }).toTransform(*//*ArtifactType.MERGED_MANIFEST.INSTANCE*//* artifactInstance)
            }
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            LibraryExtension lib = android
                    lib.libraryVariants.all { LibraryVariantImpl variant ->
                        variant.outputs.all { *//*BaseVariantOutput*//* output -> // com.android.build.gradle.api.BaseVariantOutput
                            output.processManifestProvider.get().doLast { ProcessLibraryManifest task ->
                                File manifestOutputFile = task.manifestOutputFile.get().asFile
                                ManifestTransformer.transform(project, manifestOutputFile, manifestOutputFile)
                            }
                        }
                    }
        } else if (project.plugins.hasPlugin(TestPlugin)) {
            TestExtension test = android
                    test.applicationVariants.all { variant ->
                        it.outputs.all { *//*BaseVariantOutput*//* output -> // com.android.build.gradle.api.BaseVariantOutput
                            output.processManifestProvider.get().doLast { ProcessTestManifest task ->
                                File manifestOutputFile = task.packagedManifestOutputDirectory.get().file(SdkConstants.ANDROID_MANIFEST_XML).asFile
                                ManifestTransformer.transform(project, manifestOutputFile, manifestOutputFile)
                            }
                        }
                    }
        }
    }

    void v3(Project project) {
        def android = project.extensions.getByName('android')
        if (project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(DynamicFeaturePlugin)) {
            AppExtension app = android
                    app.applicationVariants.all { ApplicationVariantImpl variant -> // com.android.build.gradle.internal.api.ApplicationVariantImpl
                        variant.outputs.all { *//*BaseVariantOutput*//* output -> // com.android.build.gradle.api.BaseVariantOutput
                            output.processManifestProvider.get().doLast { ProcessApplicationManifest task ->
                                File manifestOutputFile = task.manifestOutputDirectory.get().file(SdkConstants.ANDROID_MANIFEST_XML).asFile
                                ManifestTransformer.transform(project, manifestOutputFile, manifestOutputFile)
                            }
                        }
                    }
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            LibraryExtension lib = android
                    lib.libraryVariants.all { LibraryVariantImpl variant ->
                        variant.outputs.all { *//*BaseVariantOutput*//* output -> // com.android.build.gradle.api.BaseVariantOutput
                            output.processManifestProvider.get().doLast { ProcessLibraryManifest task ->
                                File manifestOutputFile = task.manifestOutputFile.get().asFile
                                ManifestTransformer.transform(project, manifestOutputFile, manifestOutputFile)
                            }
                        }
                    }
        } else if (project.plugins.hasPlugin(TestPlugin)) {
            TestExtension test = android
                    test.applicationVariants.all { variant ->
                        it.outputs.all { *//*BaseVariantOutput*//* output -> // com.android.build.gradle.api.BaseVariantOutput
                            output.processManifestProvider.get().doLast { ProcessTestManifest task ->
//                        File manifestOutputFolder = Strings.isNullOrEmpty(task.apkData.getDirName())
//                                ? task.manifestOutputDirectory.get().asFile
//                                : task.manifestOutputDirectory.get().file(task.apkData.getDirName()).asFile
//                        File manifestOutputFile = new File(manifestOutputFolder, SdkConstants.ANDROID_MANIFEST_XML)
                                File manifestOutputFile = task.manifestOutputDirectory.get().file(SdkConstants.ANDROID_MANIFEST_XML).asFile
                                ManifestTransformer.transform(project, manifestOutputFile, manifestOutputFile)
                            }
                        }
                    }
        }
    }*/

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

    private fun String.localCapitalize() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
}

private fun Project.hasPlugin(pluginClz: Class<out Plugin<Project>>) = plugins.hasPlugin(pluginClz)
