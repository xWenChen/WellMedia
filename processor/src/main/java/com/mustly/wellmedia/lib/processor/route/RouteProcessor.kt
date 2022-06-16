package com.mustly.wellmedia.lib.processor.route

import com.google.auto.service.AutoService
import com.mustly.wellmedia.lib.annotation.Constants
import com.mustly.wellmedia.lib.annotation.Route
import com.mustly.wellmedia.lib.annotation.Utils
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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

        moduleName = Utils.parseModuleName(processingEnv?.options?.get(Constants.OPTION_MODULE_NAME))
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

    private fun generateRouteTable(typeElements: List<TypeElement>) {
        // 生成方法的参数类型：Map<String, KClass<*>>
        // 生成方法的参数类型：Map<String, String>
        val mapTypeName = HashMap::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            String::class.asClassName()
            /*KClass::class.asClassName().parameterizedBy(
                WildcardTypeName.producerOf(Any::class)
            )*/
        )

        // 生成 map 参数
        val mapParamSpec = ParameterSpec.builder("map", mapTypeName).build()

        // map 参数添加到 register 方法中
        // override public fun register(map: Map<String, KClass<*>>)
        val funcRegister = FunSpec.builder(Constants.METHOD_REGISTER)
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PUBLIC)
            .addParameter(mapParamSpec)
            .returns(UNIT)

        // 缓存，防止重复的路由
        val pathRecorder = hashMapOf<String, String>()

        // 根据注解生成方法体
        // qualifiedName 全限定名
        typeElements.forEach {
            val route = it.getAnnotation(Route::class.java)
            val path = route.url
            // 路由重复
            if (pathRecorder.containsKey(path)) {
                throw RuntimeException("Duplicate route path: ${path}[${it.qualifiedName}, ${pathRecorder[path]}]")
            }
            funcRegister.addStatement("map[%S] = %S", path, it.asClassName().canonicalName)
            pathRecorder[path] = it.qualifiedName.toString()
        }

        // 方法内容添加到类中
        processingEnv?.elementUtils?.getTypeElement(
            Constants.ROUTE_REGISTER_INTERFACE_NAME
        )?.let { superInterfaceType ->
            TypeSpec.classBuilder(Utils.getRegisterClassName(moduleName))
                .addSuperinterface(superInterfaceType.asClassName())
                .addModifiers(KModifier.PUBLIC)
                .addFunction(funcRegister.build())
                .addKdoc(Constants.CLASS_DOC)
                .build()
        }?.also { type ->
            try {
                // 类写入文件
                processingEnv?.filer?.apply {
                    FileSpec.get(
                        Constants.GENERATED_ROUTE_REGISTER_PATH,
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

        if (Modifier.ABSTRACT in (element?.modifiers ?: emptySet())) {
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