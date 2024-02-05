package one.edee.oss.proxycian.bytebuddy;

import lombok.SneakyThrows;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.invoke.MethodHandles;

/**
 * ClassLoadingStrategy is va MethodHandles::lookup with fallback as JVM is running on JDK17, this is a behaviour called via MultiReleaseBuild. Other strategies are evaluated when running JDK8 / JDK11 with MultiRelease Build
 *
 * @author Milos Samek, FG Forrest a.s. (c) 2021
 */
@SuppressWarnings("unused")
public class StrategyProvider {
    @SneakyThrows
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> contextClass) {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(contextClass, MethodHandles.lookup());
        return ClassLoadingStrategy.UsingLookup.of(lookup);
    }
}
