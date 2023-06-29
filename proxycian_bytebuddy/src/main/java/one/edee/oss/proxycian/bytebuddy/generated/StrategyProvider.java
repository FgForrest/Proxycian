package one.edee.oss.proxycian.bytebuddy.generated;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator;

/**
 * Strategy provider provides an strategy for {@link ByteBuddyProxyGenerator#getProxyClass(Class[], ClassLoader) by specific Java version}
 * ClassLoadingStrategy is INJECTION as JVM is running on JDK8, this is a default behaviour. Other strategies are evaluated when running JDK11 / JDK17 with MultiRelease Build.
 *
 * @author Milos Samek, FG Forrest a.s. (c) 2021
 */
@SuppressWarnings("unused")
public class StrategyProvider {
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> targetClass) {
        return ClassLoadingStrategy.Default.INJECTION;
    }
}
