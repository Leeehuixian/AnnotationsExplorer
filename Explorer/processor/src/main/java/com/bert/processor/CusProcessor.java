package com.bert.processor;

import com.bert.annotations.CusAnnotation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * @Author: bertking
 * @ProjectName: AnnotationsExplorer
 * @CreateAt: 2020/8/12 11:23 AM
 * @UpdateAt: 2020/8/12 11:23 AM
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 * @Description: 自定义的注解处理器
 */
@AutoService(Processor.class) // 如果不加此行代码，该注解处理器不会生效(JVM在编译时不会加载)
//@SupportedAnnotationTypes("com.bert.annotations.CusAnnotation")
public class CusProcessor extends AbstractProcessor {
    private Filer filer;
    // 设置使用JavaPoet还是JavaWriter来生成Java文件
    private boolean useJPoet ;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnv.getFiler();
    }

    /**
     * 核心方法:Processor在处理注解时所调用的方法。(我们一般在这里，根据自定义的注解类型，来生成Java代码)
     *
     * @param annotations      注解类型的集合
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        System.out.println("--------------");
        if (annotations.isEmpty()) {
            return false;
        }

        for (TypeElement annotation :
                annotations) {
            if (annotation.getQualifiedName().toString().equals(CusAnnotation.class.getCanonicalName())) {
                System.out.println("匹配成功");
                String packageName = processingEnv.getElementUtils().getPackageOf(annotation).getQualifiedName().toString();

                if(useJPoet){
                    useJPoet(packageName);
                }else {
                    useJavaFileObj(packageName);
                }
            }
        }

        return true;
    }

    /**
     * 获取Processor支持的注解类型
     * 此方法也可以通过注解：@SupportedAnnotationTypes("com.bert.annotations.CusAnnotation"),多个注解用","隔开
     *
     * @return 支持的注解集合
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(CusAnnotation.class.getCanonicalName());
    }

    /**
     * 获取支持的版本号
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 使用JPoet来生成Java源文件(ButterKnife采用这种方式)
     *
     * @param packageName 包名
     * @return
     */
    public void useJPoet(String packageName) {
        // 创建变量
        FieldSpec ageField = FieldSpec.builder(int.class, "age", Modifier.PRIVATE).build();
        // 创建方法
        MethodSpec getAgeMethod = MethodSpec.methodBuilder("getAge").addModifiers(Modifier.PRIVATE).returns(void.class).build();
        // 创建类
        TypeSpec autoClass = TypeSpec.classBuilder("AutoClass")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("This class is generated by EventBus, do not edit.")
                .addField(ageField)
                .addMethod(getAgeMethod)
                .build();

        // 创建Java文件
        JavaFile javaFile = JavaFile.builder(packageName, autoClass).build();

        // 将文档写入
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 使用JavaWriter来读写，容易出错(EventBus就是采用这种方式实现的)
     * @param packageName
     */
    public void useJavaFileObj(String packageName) {
        Writer writer = null;
        String fileName = packageName+".AutoClazz";

        try {
            JavaFileObject sourceFile = filer.createSourceFile(fileName);
            // 如果考虑线程安全问题，可以考虑使用BufferedWriter
            writer = sourceFile.openWriter();
            StringBuilder builder = new StringBuilder()
                    .append("package com.bert.annotations;\n")
                    .append("/** This class is generated by EventBus, do not edit. */\n")
                    .append("public final class AutoClazz {\n")
                    .append("private int age;")
                    .append("\n")
                    .append("private void getAge() {}\n")
                    .append("}\n");

            writer.write(builder.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not write source for....");
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




}
