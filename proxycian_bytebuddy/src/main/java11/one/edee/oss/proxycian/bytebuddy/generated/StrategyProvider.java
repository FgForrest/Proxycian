package one.edee.oss.proxycian.bytebuddy.generated;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.reflect.Method;

import java.lang.invoke.MethodHandles;

/**
 *
 * @author Milos Samek, FG Forrest a.s. (c) 2021
 */
@SuppressWarnings("unused")
@Log4j2
public class StrategyProvider {
    @SneakyThrows
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> targetClass) {
        log.debug("ClassLoadingStrategy is va MethodHandles::lookup with fallback as JVM is running on JDK11, this is a behaviour called via MultiReleaseBuild. Other strategies are evaluated when running JDK8 / JDK17 with MultiRelease Build");
        return ClassLoadingStrategy.UsingLookup.withFallback(MethodHandles::lookup);
    }
}
