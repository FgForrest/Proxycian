package one.edee.oss.proxycian.javassist;

import javassist.util.proxy.MethodHandler;


public interface JavassistNoOpProxyGenerator {
    MethodHandler NULL_METHOD_HANDLER = (self, thisMethod, proceed, args) -> null;

    static <T> T instantiate(Class<T> contract) {
        return JavassistProxyGenerator.instantiate(
                NULL_METHOD_HANDLER,
                contract
        );
    }

}
