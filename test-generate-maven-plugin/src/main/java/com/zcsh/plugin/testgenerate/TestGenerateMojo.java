package com.zcsh.plugin.testgenerate;

import com.zcsh.plugin.core.annotations.Testable;
import com.zcsh.plugin.core.enums.ModifierKindEnum;
import com.zcsh.plugin.testgenerate.exception.JavaSourceException;
import com.zcsh.plugin.testgenerate.exception.TestMethodException;
import com.zcsh.plugin.testgenerate.kit.JavaSourceKit;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import spoon.Launcher;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.support.JavaOutputProcessor;
import spoon.support.reflect.code.CtBlockImpl;
import spoon.support.reflect.declaration.CtClassImpl;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link TestGenerateMojo}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @unknown 2020/5/8
 */
@Mojo(defaultPhase = LifecyclePhase.INSTALL, name = "test-generate")
public class TestGenerateMojo extends AbstractMojo {


    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    public File sourcePath;

    @Parameter(name = "generateTestSourcePath", defaultValue = "${project.build.directory}/test-source")
    public File generateTestSourcePath;

    /**
     * 扫描到的源代码
     */
    private Map<String, CtType> sourceMap;

    /**
     * 扫描到的测试源代码
     */
    private Map<String, CtType> testSourceMap;

    /**
     * 扫描到的需要测试的源文件
     */
    private Map<String, CtType> needTestSource = new HashMap();

    /**
     * 本次执行需要更新的源文件
     */
    private Set<CtType> updateTestSource = new HashSet<>();

    private Launcher testSourceLauncher = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("[test-generateMojo] start");

        prepare();

        generateNeedTestSource();

        fillNotExistTestSource();

        generateTestMethod();

        executeUpdate();

    }


    public void prepare() {
        // 扫描需要生成测试类的目录
        Launcher sourceLauncher = JavaSourceKit.createLauncher(sourcePath.getAbsolutePath(), null);
        sourceMap = JavaSourceKit.scanJavaSourceFile(sourceLauncher.getModel());

        // 扫描生成测试类的目录
        testSourceLauncher = JavaSourceKit.createLauncher(generateTestSourcePath.getAbsolutePath(), generateTestSourcePath.getAbsolutePath());
        testSourceMap = JavaSourceKit.scanJavaSourceFile(testSourceLauncher.getModel());
        getLog().debug("[准备] 扫描到源文件数量: " + sourceMap.size() + " , 扫描到测试类的源文件数量:" + testSourceMap.size());
    }

    /**
     * 创建需要生成测试类的源文件
     *
     * @return
     */
    public Map<String, CtType> generateNeedTestSource() {

        sourceMap.forEach((k, v) -> {
            if (v.hasAnnotation(Testable.class)) {
                needTestSource.put(k, v);
            }
        });
        return needTestSource;
    }

    /**
     * 填充不存在的测试类,将需要生成的测试类的类,但是测试类不存在,则创建并填充到 testSourceMap
     */
    private void fillNotExistTestSource() {
        for (String qualifiedName : needTestSource.keySet()) {
            if (findCorrespondingTestSource(qualifiedName, testSourceMap) == null) {
                CtType ctType = needTestSource.get(qualifiedName);
                CtType generateTestSource = new CtClassImpl();
                generateTestSource.setSimpleName(ctType.getSimpleName() + JavaSourceKit.GENERATE_TEST_SOURCE_SUFFIX);
                generateTestSource.setParent(ctType.getParent());
                generateTestSource.setVisibility(ModifierKind.PUBLIC);
                generateTestSource.setFactory(testSourceLauncher.getFactory());
                testSourceLauncher.getFactory().Package().getOrCreate(generateTestSource.getPackage().getQualifiedName()).addType(generateTestSource);
                testSourceMap.put(generateTestSource.getQualifiedName(), generateTestSource);
                CtComment ctComment = JavaSourceKit.generateComment("@see {@link " + generateTestSource.getQualifiedName() + "}", CtComment.CommentType.JAVADOC);
                generateTestSource.addComment(ctComment);
                // 将新添加的测试类存入本次更新的源文件集合中
                updateTestSource.add(generateTestSource);
                getLog().info("[创建新测试文件] 测试文件全类名:" + generateTestSource.getQualifiedName());
            }
        }
    }

    /**
     * 将需要生成测试方法的源文件, 在测试类中生成
     */
    public void generateTestMethod() {
        for (String qualifiedName : needTestSource.keySet()) {
            CtType ctTypeSource = needTestSource.get(qualifiedName);

            // 找到与之对应的测试类
            CtType correspondingTestSource = findCorrespondingTestSource(qualifiedName, testSourceMap);

            Testable annotation = ctTypeSource.getAnnotation(Testable.class);
            ModifierKindEnum[] modifierKindEnums = annotation.methodModifierTestable();
            if (modifierKindEnums == null || modifierKindEnums.length == 0) {
                throw new JavaSourceException("@Testable 未找到 methodModifierTestable 属性: " + qualifiedName);
            }

            // 查找到本次需要生成的测试方法
            Set<CtMethod> needGenerateTestMethod = findNeedGenerateTestMethod(ctTypeSource, correspondingTestSource);

            // 将测试方法添加到测试类中
            for (CtMethod ctMethod : needGenerateTestMethod) {
                CtMethod testMethod = doGenerateTestMethod(ctTypeSource, ctMethod);
                correspondingTestSource.addMethod(testMethod);
            }

            // 将新添加的测试类存入本次更新的源文件集合中
            if (!needGenerateTestMethod.isEmpty()) {
                updateTestSource.add(correspondingTestSource);
            }
        }
    }

    /**
     * 生成指定源文件方法的测试方法
     *
     * @param ctType   需要生成测试方法的源文件
     * @param ctMethod 需要生成测试方法的方法
     * @return 返回生成好的测试方法
     */
    public CtMethod doGenerateTestMethod(CtType ctType, CtMethod ctMethod) {
        // 生成测试方法的示例
        // /**
        // * @see {@link ctType#ctMethod()}
        // */
        // @Test
        // public void ctMethod() {
        // }

        CtMethod testMethod = JavaSourceKit.generateSimpleMethod(ModifierKind.PUBLIC, null, ctMethod.getSimpleName());
        // 方法的 javaDoc
        testMethod.setDocComment(JavaSourceKit.generateMethodReferenceComment(ctType, ctMethod));
        // 空方法体
        testMethod.setBody(new CtBlockImpl<>());
        // 添加单元测试的注解
        testMethod.addAnnotation(JavaSourceKit.generateAnnotation(JavaSourceKit.ADD_TEST_ANNOTATION));

        return testMethod;
    }

    /**
     * 查找一个 java 源文件与之对应的测试 java 文件
     *
     * @param qualifiedName 需要查找的java 源文件的限定名
     * @param testSourceMap 查找的集合
     * @return 返回与对应的测试源文件
     */
    private CtType findCorrespondingTestSource(String qualifiedName, Map<String, CtType> testSourceMap) {
        CtType ctType = testSourceMap.get(qualifiedName);
        if (ctType == null) {
            ctType = testSourceMap.get(qualifiedName + JavaSourceKit.GENERATE_TEST_SOURCE_SUFFIX);
        }
        return ctType;
    }

    /**
     * 查找需要生成测试方法的方法集合
     *
     * @param ctType     需要生成测试类的源文件
     * @param testCtType 与之对应的测试类
     * @return 待生成的测试方法
     */
    private Set<CtMethod> findNeedGenerateTestMethod(CtType ctType, CtType testCtType) {
        Testable annotation = ctType.getAnnotation(Testable.class);
        ModifierKindEnum[] modifierKindEnums = annotation.methodModifierTestable();
        if (modifierKindEnums.length == 0) {
            throw new JavaSourceException("@Testable 未找到 methodModifierTestable 属性: " + ctType.getQualifiedName());
        }

        // 筛选出要生成测试方法的方法修饰符
        Set<String> modifierKindSet = Stream.of(modifierKindEnums)
                .map(ModifierKindEnum::toString)
                .collect(Collectors.toSet());

        // 得到待生成测试方法的方法集合
        return ((Set<CtMethod>) ctType.getMethods()).stream()
                .filter(item -> modifierKindSet.contains(item.getVisibility().toString()))
                .filter(item -> !JavaSourceKit.isExistMethodName(testCtType, item.getSimpleName()))
                .collect(Collectors.toSet());
    }

    /**
     * 执行更新,将本次需要更新的测试类进行更新
     */
    public void executeUpdate() {
        JavaOutputProcessor javaOutputProcessor = new JavaOutputProcessor(testSourceLauncher.getEnvironment().createPrettyPrinterAutoImport());
        javaOutputProcessor.setFactory(testSourceLauncher.getFactory());

        for (CtType ctType : updateTestSource) {
            javaOutputProcessor.process(ctType);
            getLog().info("[更新测试文件] " + ctType.getQualifiedName());
        }
    }
}