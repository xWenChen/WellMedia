package com.mustly.wellmedia.lib.processor.route

import com.google.auto.service.AutoService
import com.mustly.wellmedia.lib.annotation.Route
import com.squareup.kotlinpoet.CodeBlock
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class RouteProcessor : AbstractProcessor() {
    companion object {
        // 注解处理创建的源代码文件位于 kapt/kotlin/generated
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    private val logger by lazy { ProcessorLogger(processingEnv) }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)

        if (processingEnv == null) {
            return
        }
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        if (annotations.isNullOrEmpty()) {
            // 我们自定义的注解处理器不再执行
            return false
        }
        // 检查处理器是否能够找到必要的文件夹并将文件写入其中
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: return false

        // 收集所有路由的信息，统一起来
        val codeBuilder = CodeBlock.Builder()

        // 加一个随机的UUID，防止多个模块之间生成的文件重复
        val fileName = "AnnotationInit" + "_" + UUID.randomUUID().toString().replace("-", "")

        // element 是用 Route 注解标注的元素，如 Activity 或 Fragment
        roundEnv?.getElementsAnnotatedWith(Route::class.java)?.forEach { element ->
            if (!validateRoute(element)) {
                return false
            }

            val annotation = element.getAnnotation(Route::class.java)

            val url = annotation.url
            // Activity 或 Fragment 名
            val className = element.simpleName.toString()
            // 生成目标 Activity 或 Fragment 的全限定名称，例如：com.mustly.wellmedia.MainActivity
            val packageName = processingEnv.elementUtils.getPackageOf(element).toString()
            val target = "$packageName.$className"
            // 插入代码语句，注册路由
            codeBuilder.addStatement("handler.register(%S, %S)", url, target)
        }

        RouteCodeBuilder(kaptKotlinGeneratedDir, fileName, codeBuilder.build()).buildFile()
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Route::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    // 检查 @Route 注解是否 OK
    private fun validateRoute(element: Element): Boolean {
        val activity = processingEnv.elementUtils.getTypeElement("android.app.Activity").asType()
        val fragment = processingEnv.elementUtils.getTypeElement("android.app.Fragment").asType()
        val androidXFragment = processingEnv.elementUtils.getTypeElement("androidx.fragment.app.Fragment").asType()
        (element as? TypeElement)?.let {
            // 检测 Activity
            if (!processingEnv.typeUtils.isSubtype(it.asType(), activity)
                && !processingEnv.typeUtils.isSubtype(it.asType(), fragment)
                && !processingEnv.typeUtils.isSubtype(it.asType(), androidXFragment)
            ) {
                logger.e("Route 只能标注 Activity 和 Fragment", element)
                return false
            }

            if (Modifier.ABSTRACT in it.modifiers) {
                logger.e("Route 标注的元素不可以是抽象类", element)
                return false
            }

            return true
        } ?: return false
    }
}