package one.edee.oss.proxycian.bytebuddy.generated;

import lombok.extern.log4j.Log4j2;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator;

/**
 * Stragegy provider provides an strategy for {@link ByteBuddyProxyGenerator#getProxyClass(Class[], ClassLoader) by specific Java version}
 * @author Milos Samek, FG Forrest a.s. (c) 2021
 */
@SuppressWarnings("unused")
@Log4j2
public class StrategyProvider {
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> targetClass) {
        log.debug("ClassLoadingStrategy is INJECTION as JVM is running on JDK8, this is a default behaviour. Other strategies are evaluated when running JDK11 / JDK17 with MultiRelease Build.");
        return ClassLoadingStrategy.Default.INJECTION;
    }
}
