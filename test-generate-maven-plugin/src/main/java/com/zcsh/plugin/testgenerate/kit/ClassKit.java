package com.zcsh.plugin.testgenerate.kit;
import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
/**
 * {@link ClassKit}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @unknown 2020/5/8
 */
public class ClassKit {
    /**
     * 筛选出一个 class 集合中,带指定注解的 class 集合
     *
     * @param classes
     * 		带筛选 class
     * @param annotationClass
     * 		筛选出带指定的注解
     * @return 返回带 annotationClass 的 class
     */
    @Transient
    public static List<Class> filterClassWithAnnotation(List<Class> classes, Class<? extends Annotation> annotationClass) {
        return Objects.isNull(classes) ? Collections.emptyList() : classes.stream().filter(( item) -> item.isAnnotationPresent(annotationClass)).collect(Collectors.toList());
    }
}