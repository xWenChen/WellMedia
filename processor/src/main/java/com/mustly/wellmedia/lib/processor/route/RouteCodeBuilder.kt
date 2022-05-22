package com.mustly.wellmedia.lib.processor.route

import com.squareup.kotlinpoet.*
import java.io.File

class RouteCodeBuilder(
    private val kaptKotlinGeneratedDir: String, //
    private val fileName: String, //
    private val code: CodeBlock, //
) {
    private val routerAnnotationInit = ClassName("com.mustly.wellmedia.lib.commonlib.route", "RouterAnnotationInit")
    private val routerAnnotationHandler = ClassName("com.mustly.wellmedia.lib.commonlib.route", "DefaultRouteAnnotationHandler")
    private val generatedPackage = "com.mustly.mediacommon.route.generated"

    fun buildFile() = FileSpec.builder(generatedPackage, fileName) // 定义一个包名 generatedPackage，名称为 fileName 的文件
        .addInitClass() // 向文件中添加一个名为 fileName 的类
        .build()
        .writeTo(File(kaptKotlinGeneratedDir)) // 将生成的文件写入 kaptKotlinGeneratedDir 文件夹

    private fun FileSpec.Builder.addInitClass() = apply { // 1
        addType(TypeSpec.classBuilder(fileName)  // 向文件中添加一个名为 fileName 的类
            .addSuperinterface(routerAnnotationInit) // 此 类实现了 routerAnnotationInit 接口
            .addInitMethod(code) // 3
            .build()
        )
    }

    private fun TypeSpec.Builder.addInitMethod(code: CodeBlock) = apply { // 1
        addFunction(FunSpec.builder("init") // 向类中添加一个名为 init 的方法
            .addModifiers(KModifier.OVERRIDE) // 该方法重写了一个抽象方法
            .addParameter("handler", routerAnnotationHandler) // 将参数添加到函数定义中，此方法覆盖的 init 方法有一个参数：handler
            .returns(UNIT) // 返回一个 UNIT
            .addCode(code) // 将代码块添加到方法体中
            .build()
        )
    }
}