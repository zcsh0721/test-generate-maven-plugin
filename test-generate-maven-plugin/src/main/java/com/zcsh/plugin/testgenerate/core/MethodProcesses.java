package com.zcsh.plugin.testgenerate.core;

import com.zcsh.plugin.core.enums.ModifierKindEnum;
import spoon.reflect.declaration.CtMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * {@link MethodProcesses}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @unknown 2020/5/9
 */
public class MethodProcesses {
    private static Set<String> internalMethodNameSet = new HashSet<>();

    static {
        for (Method method : Object.class.getMethods()) {
            internalMethodNameSet.add(method.getName());
        }
    }

    /**
     * 过滤掉方法集合中带指定注解的方法
     *
     * @param methods
     * 		待过滤方法集合
     * @param excludeAnnotation
     * 		需要排除的方法集合
     * @return 返回不带注解集合元素的方法
     */
    public static Set<CtMethod> filterMethodByExcludeAnnotation(Set<CtMethod> methods, Class<? extends Annotation> excludeAnnotation) {
        if (excludeAnnotation == null) {
            return methods;
        }
        return methods.stream().filter(( item) -> !item.hasAnnotation(excludeAnnotation)).collect(Collectors.toSet());
    }

    /**
     * 查找方法集合中指定修饰符的方法
     *
     * @param methods
     * 		待过滤方法集合
     * @param includeModifierList
     * 		修饰符集合
     * @return 返回指定修饰符集合的方法
     */
    public static Set<CtMethod> findMethodWithModifier(Set<CtMethod> methods, List<ModifierKindEnum> includeModifierList) {
        if (Objects.isNull(includeModifierList) || includeModifierList.isEmpty()) {
            return methods;
        }
        Set<String> includeModifierSet = includeModifierList.stream().map(ModifierKindEnum::toString).collect(Collectors.toSet());
        return methods.stream().filter(( item) -> includeModifierSet.contains(item.getVisibility().toString())).collect(Collectors.toSet());
    }

    /**
     * 过滤集合中与 internalMethodNameSet 元素相同的方法名
     *
     * @param methods
     * 		带过滤方法集合
     * @return 返回过滤后的方法集合
     */
    public static Set<CtMethod> filterInternalMethod(Set<CtMethod> methods) {
        // 过滤构造器,因为CtMethod 中不会有构造函数,所以可以忽略
        // 过滤 internalMethodNameSet 方法名
        return methods.stream().filter(( item) -> !internalMethodNameSet.contains(item.getSimpleName())).collect(Collectors.toSet());
    }
}