package com.zcsh.plugin.core.enums;
import java.util.Locale;

/**
 * {@link ModifierKindEnum}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @unknown 2020/5/9
 */
public enum ModifierKindEnum {

    PUBLIC,
    PROTECTED,
    PRIVATE,
    NULL;
    private String lowercase = null;

    @Override
    public String toString() {
        if (lowercase == null) {
            lowercase = name().toLowerCase(Locale.US);
        }
        return lowercase;
    }
}