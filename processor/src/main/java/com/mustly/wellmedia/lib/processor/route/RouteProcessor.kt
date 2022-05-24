package com.mustly.wellmedia.lib.processor.route

import com.google.auto.service.AutoService
import com.mustly.wellmedia.lib.annotation.Route
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

@AutoService(Processor::class)
class RouteProcessor : AbstractProcessor() {
    companion object {
        // 注解处理创建的源代码文件位于 kapt/kotlin/generated
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    private var moduleName = Constants.DEFAULT_MODULE_NAME

    private val logger by lazy { ProcessorLogger(processingEnv) }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Route::class.java.canonicalName)
    }

    override fun getSupportedOptions(): MutableSet<String> {
        return mutableSetOf(Constants.OPTION_MODULE_NAME)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)

        parseModuleName()
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {

        val elements = roundEnv?.getElementsAnnotatedWith(Route::class.java)

        if (moduleName.isEmpty() || elements.isNullOrEmpty()) {
            // 我们自定义的注解处理器不再执行
            return true
        }

        // 合法的TypeElement集合
        val typeElements = elements.filter {
            it.kind.isClass && validateRoute(it)
        }.map {
            it as TypeElement
        }

        generateRouteTable(typeElements)

        return true
    }

    private fun parseModuleName() {
        var tempName = processingEnv?.options?.get(Constants.OPTION_MODULE_NAME)

        if (tempName.isNullOrEmpty()) {
            tempName = Constants.DEFAULT_MODULE_NAME
        }

        moduleName = tempName
            .replace(".", "_")
            .replace("-", "_")
            .replace(" ", "_")
            .replaceFirstChar {
                // 首字母大写
                if (it.isLowerCase()) {
                    it.titlecase(Locale.getDefault())
                } else {
                    it.toString()
                }
            }
    }

    private fun generateRouteTable(typeElements: List<TypeElement>) {
        // 生成方法的参数类型：Map<String, KClass<*>>
        val mapTypeName = Map::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            KClass::class.asClassName().parameterizedBy(
                WildcardTypeName.producerOf(Any::class)
            )
        )

        val mapParamSpec = ParameterSpec.builder("map", mapTypeName).build()

        // override public fun register(map: Map<String, KClass<*>>)
        val funcRegister = FunSpec.builder(Constants.METHOD_REGISTER)
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PUBLIC)
            .addParameter(mapParamSpec)
            .returns(UNIT)

        // 缓存，防止重复的路由
        val pathRecorder = hashMapOf<String, String>()

        // qualifiedName 全限定名
        typeElements.forEach {
            val route = it.getAnnotation(Route::class.java)
            val path = route.url
            // 路由重复
            if (pathRecorder.containsKey(path)) {
                throw RuntimeException("Duplicate route path: ${path}[${it.qualifiedName}, ${pathRecorder[path]}]")
            }
            funcRegister.addStatement("map[%S] = %T::class", path, it.asClassName())
            pathRecorder[path] = it.qualifiedName.toString()
        }

        processingEnv?.elementUtils?.getTypeElement(
            Constants.ROUTE_REGISTER_FULL_NAME
        )?.let { superInterfaceType ->
            TypeSpec.classBuilder("$moduleName${Constants.ROUTE_REGISTER}")
                .addSuperinterface(superInterfaceType.asClassName())
                .addModifiers(KModifier.PUBLIC)
                .addFunction(funcRegister.build())
                .addKdoc(Constants.CLASS_DOC)
                .build()
        }?.also { type ->
            try {
                processingEnv?.filer?.apply {
                    FileSpec.get(
                        Constants.ROUTE_HUB_PACKAGE_NAME,
                        type
                    ).writeTo(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 检查 @Route 注解是否 OK
    private fun validateRoute(element: Element?): Boolean {
        if (!isSubtype(element, Constants.ACTIVITY_FULL_NAME)
            && !isSubtype(element, Constants.FRAGMENT_FULL_NAME)
            && !isSubtype(element, Constants.FRAGMENT_X_FULL_NAME)
        ) {
            return false
        }

        if (Modifier.ABSTRACT in element?.modifiers ?: emptySet()) {
            return false
        }

        return true
    }

    private fun isSubtype(typeElement: Element?, type: String): Boolean {
        return processingEnv?.run {
            typeUtils.isSubtype(
                typeElement?.asType(),
                elementUtils.getTypeElement(type).asType()
            )
        } ?: false
    }
}