package one.edee.oss.proxycian.bytebuddy;

import one.edee.oss.proxycian.AbstractDispatcherInvocationHandler;
import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.StandardJavaMethods;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator.INVOCATION_HANDLER_FIELD;

/**
 * Abstract implementation to allow different ByteBuddy approaches to be used based on JDK version.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2023
 */
public abstract class AbstractByteBuddyDispatcherInvocationHandler<T, S extends AbstractByteBuddyDispatcherInvocationHandler<T, S>> extends AbstractDispatcherInvocationHandler<T> {
    protected static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    public AbstractByteBuddyDispatcherInvocationHandler(T proxyState, Collection<MethodClassification<?, ?>> methodClassifications) {
        super(proxyState, methodClassifications);
    }

    public AbstractByteBuddyDispatcherInvocationHandler(T proxyState, MethodClassification<?, ?>... methodClassifications) {
        super(proxyState, methodClassifications);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addImplementationSpecificInvokers(List<MethodClassification<?, ?>> methodClassifications) {
        methodClassifications.add(StandardJavaMethods.cloneMethodInvoker(objectToClone -> {
            try {
                final Field field = objectToClone.getClass().getDeclaredField(INVOCATION_HANDLER_FIELD);
                field.setAccessible(true);
                final S originalDispatcher = (S) field.get(objectToClone);

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
                    if (!ProxyStateAccessor.class.equals(anInterface)) {
                        interfaces.add(anInterface);
                    }
                }
                return makeClone(originalDispatcher, clonedState, interfaces);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                     NoSuchFieldException e) {
                throw new CloneNotSupportedException("Cannot clone the proxy instance due to: " + e.getMessage());
            }
        }));
    }

    protected abstract ProxyStateAccessor makeClone(
            S originalDispatcher,
            T clonedState,
            List<Class<?>> interfaces
    );
}
