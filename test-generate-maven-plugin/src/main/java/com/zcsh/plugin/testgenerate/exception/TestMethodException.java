package com.zcsh.plugin.testgenerate.exception;
/**
 * {@link TestMethodException}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @unknown 2020/5/8
 */
public class TestMethodException extends RuntimeException {
    public TestMethodException(String message) {
        super(message);
    }
}