package one.edee.oss.proxycian.bytebuddy;

import lombok.extern.log4j.Log4j2;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

/**
 * Stragegy provider provides an strategy for {@link ByteBuddyProxyGenerator#getProxyClass(Class[], ClassLoader) by specific Java version}
 * @author Milos Samek, FG Forrest a.s. (c) 2021
 */
@SuppressWarnings("unused")
@Log4j2
public class StrategyProvider {
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> targetClass) {
        log.debug("StrategyProvider.getStrategy is evaluated via JVM version 8");
        return ClassLoadingStrategy.Default.INJECTION;
    }
}
