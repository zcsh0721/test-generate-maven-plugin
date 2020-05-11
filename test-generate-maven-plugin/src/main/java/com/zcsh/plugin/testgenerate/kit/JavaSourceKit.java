package com.zcsh.plugin.testgenerate.kit;

import org.apache.commons.lang3.StringUtils;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtCommentImpl;
import spoon.support.reflect.declaration.CtAnnotationImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtPackageReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

import java.util.*;

/**
 * {@link JavaSourceKit}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @unknown 2020/5/9
 */
public class JavaSourceKit {
    public static final String VOID = "void";

    public static final String ADD_TEST_ANNOTATION = "org.junit.Test";

    public static final String GENERATE_TEST_SOURCE_SUFFIX = "Test";

    /**
     * 创建 Launcher
     *
     * @param scanPath         扫描java 文件的目录
     * @param sourceOutputPath 修改完源代码输出的目录
     * @return 返回 Launcher
     */
    public static Launcher createLauncher(String scanPath, String sourceOutputPath) {
        if (StringUtils.isEmpty(sourceOutputPath)) {
            sourceOutputPath = scanPath;
        }
        Launcher launcher = new Launcher();
        launcher.addInputResource(scanPath);
        launcher.setSourceOutputDirectory(sourceOutputPath);
        launcher.buildModel();
        return launcher;
    }

    /**
     * 查询指定 model 下的所有 java 文件
     *
     * @param model
     * @return map: 类的全类名 -> java 文件
     */
    public static Map<String, CtType> scanJavaSourceFile(CtModel model) {
        HashMap<String, CtType> returnMap = new HashMap<>();
        for (CtPackage allPackage : model.getAllPackages()) {
            if (!allPackage.getTypes().isEmpty()) {
                for (CtType ctType : allPackage.getTypes()) {
                    returnMap.put(ctType.getQualifiedName(), ctType);
                }
            }
        }
        return returnMap;
    }

    /**
     * 生成一个简单方法 ; example: public void test();
     *
     * @param modifierKind             方法的修饰符; 如 public
     * @param returnValueQualifiedName 方法返回类的限定名,如果为 null or void ,则返回 void
     * @param methodName               方法名
     * @return 返回一个简单的方法
     */
    public static CtMethod generateSimpleMethod(ModifierKind modifierKind, String returnValueQualifiedName, String methodName) {
        CtMethod ctMethod = new CtMethodImpl<>();
        // 返回值
        ctMethod.setType(generateTypeReference(returnValueQualifiedName));
        // 修饰符
        ctMethod.setVisibility(modifierKind);
        // 方法名
        ctMethod.setSimpleName(methodName);
        // 添加 javaDoc 的注释处理
        addJavaDocCommentConfig(ctMethod);
        return ctMethod;
    }

    /**
     * 生成注释
     *
     * @param comment     注释内容
     * @param commentType 注释类型
     * @return 返回生成的注释, 可添加到 ctMethod 的 Body 中
     */
    public static CtComment generateComment(String comment, CtComment.CommentType commentType) {
        CtCommentImpl ctComment = new CtCommentImpl();
        ctComment.setCommentType(commentType);
        ctComment.setContent(comment);
        return ctComment;
    }

    /**
     * 生成方法引用的注释
     *
     * @param ctType   java 文件
     * @param ctMethod 需要生成描述的方法
     * @return 返回方法引用注释; example: @see TestGenerateMojo#test(ConcurrentHashMap,HashMap)
     */
    public static String generateMethodReferenceComment(CtType ctType, CtMethod ctMethod) {
        StringBuilder comment = new StringBuilder().append("@see {@link ").append(ctType.getQualifiedName()).append("#").append(ctMethod.getSimpleName()).append("(");
        for (CtParameter parameter : ((List<CtParameter>) (ctMethod.getParameters()))) {
            comment.append(parameter.getType().getQualifiedName()).append(",");
        }
        if (!ctMethod.getParameters().isEmpty()) {
            comment.deleteCharAt(comment.lastIndexOf(","));
        }
        return comment.append(")}").toString();
    }

    /**
     * 生成类型引用
     *
     * @param qualifiedName 类的限定名,如果为 null or  void ,则返回 void
     * @return 返回一个全限定名的类型引用
     */
    public static CtTypeReference generateTypeReference(String qualifiedName) {
        CtTypeReference ctTypeReference = new CtTypeReferenceImpl();
        if (StringUtils.isEmpty(qualifiedName) || Objects.equals(qualifiedName, VOID)) {
            ctTypeReference.setSimpleName(VOID);
        } else {
            ctTypeReference.setSimpleName(qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1));
            CtPackageReference ctPackageReference = new CtPackageReferenceImpl();
            ctPackageReference.setSimpleName(qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
            ctTypeReference.setPackage(ctPackageReference);
        }
        return ctTypeReference;
    }

    /**
     * 判断指定 java 文件是否存在指定方法名的方法
     *
     * @param ctType     需判断的java 文件
     * @param methodName 查找的方法名
     * @return 存在 true , 不存在  false
     */
    public static Boolean isExistMethodName(CtType ctType, String methodName) {
        return ((Set<CtMethod<?>>) (ctType.getMethods())).stream().anyMatch((item) -> Objects.equals(item.getSimpleName(), methodName));
    }


    /**
     * 生成一个注解
     *
     * @param qualifiedName 注解的限定名
     * @return 返回生成的注解
     */
    public static CtAnnotation generateAnnotation(String qualifiedName) {
        CtAnnotation ctAnnotation = new CtAnnotationImpl();
        ctAnnotation.setAnnotationType(generateTypeReference(qualifiedName));
        return ctAnnotation;
    }

    /**
     * 给方法添加 javaDoc 注解配置,如果有的话则不添加
     *
     * @param ctMethod 需要添加的方法
     */
    private static void addJavaDocCommentConfig(CtMethod ctMethod) {
        boolean isExistJavaDoc = ctMethod.getComments().stream().anyMatch((item) -> Objects.equals(item.getCommentType(), CtComment.CommentType.JAVADOC));
        if (!isExistJavaDoc) {
            CtCommentImpl javaDocComment = new CtCommentImpl();
            javaDocComment.setCommentType(CtComment.CommentType.JAVADOC);
            ctMethod.addComment(javaDocComment);
        }
    }

}