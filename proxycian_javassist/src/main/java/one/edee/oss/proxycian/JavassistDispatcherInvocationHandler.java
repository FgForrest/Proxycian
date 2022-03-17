package one.edee.oss.proxycian;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import lombok.RequiredArgsConstructor;
import one.edee.oss.proxycian.cache.ClassMethodCacheKey;
import one.edee.oss.proxycian.exception.InvalidSuperMethodCallException;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.StandardJavaMethods;
import one.edee.oss.proxycian.util.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.Optional.ofNullable;

public class JavassistDispatcherInvocationHandler<T> extends AbstractDispatcherInvocationHandler<T> implements MethodHandler {
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    public JavassistDispatcherInvocationHandler(T proxyState, MethodClassification<?, ?>... methodClassifications) {
        super(proxyState, methodClassifications);
    }

    private JavassistDispatcherInvocationHandler(T proxyState, Collection<MethodClassification<?, ?>> methodClassifications) {
        super(proxyState, methodClassifications);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        final ClassMethodCacheKey cacheKey = this.createCacheKey(self.getClass(), proxyState.getClass(), thisMethod);

        // COMPUTE IF ABSENT = GET FROM MAP, IF MISSING OR INVALID -> COMPUTE, STORE AND RETURN RESULT OF LAMBDA
        @SuppressWarnings("rawtypes") CurriedMethodContextInvocationHandler invocationHandler = JavassistProxyGenerator.CLASSIFICATION_CACHE.get(cacheKey);
        if (invocationHandler == null) {
            invocationHandler = this.getCurriedMethodContextInvocationHandler(thisMethod);
            JavassistProxyGenerator.CLASSIFICATION_CACHE.put(cacheKey, invocationHandler);
        }
		// INVOKE CURRIED LAMBDA, PASS REFERENCE TO REAL METHOD IF AVAILABLE
		return invocationHandler.invoke(
            self, ofNullable(proceed).orElse(thisMethod), args, proxyState,
            new MethodCall(proceed, self, args)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addImplementationSpecificInvokers(List<MethodClassification<?, ?>> methodClassifications) {
        methodClassifications.add(StandardJavaMethods.cloneMethodInvoker(objectToClone -> {
            try {
                final JavassistDispatcherInvocationHandler<T> originalDispatcher = (JavassistDispatcherInvocationHandler<T>) ((ProxyObject)objectToClone).getHandler();

                final T originalProxyState = (T) objectToClone.getProxyState();
                final Method cloneMethod = originalProxyState.getClass().getDeclaredMethod("clone");
                cloneMethod.setAccessible(true);
                final T clonedState = (T) cloneMethod.invoke(originalProxyState);

                final List<Class<?>> interfaces = new LinkedList<>();
                final Class<?> superClass = objectToClone.getClass().getSuperclass();
                if (!Object.class.equals(superClass)) {
                    interfaces.add(superClass);
                }
                for (Class<?> anInterface : objectToClone.getClass().getInterfaces()) {
                    if (!ProxyStateAccessor.class.equals(anInterface) && !ProxyObject.class.equals(anInterface)) {
                        interfaces.add(anInterface);
                    }
                }

                return JavassistProxyGenerator.instantiate(
                    new JavassistDispatcherInvocationHandler<>(
                        clonedState,
                        originalDispatcher.methodClassifications
                    ),
                    interfaces.toArray(EMPTY_CLASS_ARRAY)
                );
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new CloneNotSupportedException("Cannot clone the proxy instance due to: " + e.getMessage());
            }
        }));
    }

    @RequiredArgsConstructor
    private static class MethodCall implements Callable<Object> {
        private final Method proceed;
        private final Object self;
        private final Object[] arguments;

        @Override
        public Object call() {
            if (proceed == null) {
                throw new UnsupportedOperationException("Calling super method is not allowed!");
            } else {
                try {
                    if (proceed.isDefault()) {
                        final MethodHandle methodHandle = ReflectionUtils.findMethodHandle(proceed);
                        return methodHandle.bindTo(self).invokeWithArguments(arguments);
                    } else {
                        return proceed.invoke(self, arguments);
                    }
                } catch(Throwable e) {
                    throw new InvalidSuperMethodCallException(e);
                }
            }
        }
    }
}
