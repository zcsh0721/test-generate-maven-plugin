package com.zcsh.plugin.testgenerate.kit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codehaus.plexus.util.DirectoryScanner;
/**
 * {@link ClassPathKit}
 *
 * @author <a href="mailto:carey.zhou@yunlsp.com">Carey.zhou</a>
 * @version ${project.version}
 * @unknown 2020/5/8
 */
public class ClassPathKit {
    /**
     * 获取一个 classPath 下面的所有 class
     *
     * @param classPath
     * 		需要查询的 classPath
     * @return 返回 class 集合; 元素为 class 的全类名
     */
    public static List<String> findClassAbsolutePathList(String classPath) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(classPath);
        directoryScanner.setIncludes(new String[]{ "**/*.class" });
        directoryScanner.scan();
        return Stream.of(directoryScanner.getIncludedFiles()).map(( item) -> item.replace("/", ".").replace("\\", ".").substring(0, item.lastIndexOf("."))).collect(Collectors.toList());
    }
}