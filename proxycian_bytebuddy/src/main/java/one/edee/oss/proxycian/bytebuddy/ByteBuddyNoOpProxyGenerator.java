package one.edee.oss.proxycian.bytebuddy;


import one.edee.oss.proxycian.DispatcherInvocationHandler;

public interface ByteBuddyNoOpProxyGenerator {
    DispatcherInvocationHandler NULL_INVOCATION_HANDLER = new DispatcherInvocationHandler() { };

    static <T> T instantiate(Class<T> contract) {
        return ByteBuddyProxyGenerator.instantiate(
                NULL_INVOCATION_HANDLER,
                contract
        );
    }

}
