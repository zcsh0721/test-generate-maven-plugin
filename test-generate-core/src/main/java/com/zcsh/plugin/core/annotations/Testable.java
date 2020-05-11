package com.zcsh.plugin.core.annotations;

import com.zcsh.plugin.core.enums.ModifierKindEnum;

import java.lang.annotation.*;

/**
 * {@link Testable}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @date 2020/5/10
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Testable {

    /**
     * 可测试的方法修饰符,默认只会生成方法为 public 的修饰符的方法;
     * 注意:
     * 1.同样的方法名只会生成一个测试方法
     * 2.与 Object 中包含的方法名相同的,不会生成测试方法
     * 3.构造函数不会生成测试方法
     */
    ModifierKindEnum[] methodModifierTestable() default ModifierKindEnum.PUBLIC;

}
