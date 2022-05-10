package one.edee.oss.proxycian.bytebuddy;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.reflect.Method;

/**
 *
 * @author Milos Samek, FG Forrest a.s. (c) 2021
 */
@SuppressWarnings("unused")
@Log4j2
public class StrategyProvider {
    @SneakyThrows
    public ClassLoadingStrategy<ClassLoader> getStrategy(Class<?> targetClass) {
        log.debug("StrategyProvider.getStrategy is evaluated via JVM version 11");
        final Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
        final Object lookup = methodHandles.getMethod("lookup").invoke(null);
        final Method privateLookupIn = methodHandles.getMethod("privateLookupIn", Class.class, Class.forName("java.lang.invoke.MethodHandles$Lookup"));
        final Object privateLookup = privateLookupIn.invoke(null, targetClass, lookup);
        return ClassLoadingStrategy.UsingLookup.of(privateLookup);
    }
}
