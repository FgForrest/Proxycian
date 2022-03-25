package one.edee.oss.proxycian.bytebuddy;


import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.DefaultCall;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import one.edee.oss.proxycian.AbstractDispatcherInvocationHandler;
import one.edee.oss.proxycian.CurriedMethodContextInvocationHandler;
import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.cache.ClassMethodCacheKey;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.StandardJavaMethods;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator.INVOCATION_HANDLER_FIELD;
import static one.edee.oss.proxycian.util.ReflectionUtils.findMethodHandle;

public class ByteBuddyDispatcherInvocationHandler<T> extends AbstractDispatcherInvocationHandler<T> {
	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

	public ByteBuddyDispatcherInvocationHandler(T proxyState, MethodClassification<?, ?>... methodClassifications) {
		super(proxyState, methodClassifications);
	}

	private ByteBuddyDispatcherInvocationHandler(T proxyState, Collection<MethodClassification<?, ?>> methodClassifications) {
		super(proxyState, methodClassifications);
	}

	/**
	 * This method gets bound and invoked by the ByteBuddy.
	 */
	@RuntimeType
	public static Object interceptMethodCall(
		@This Object proxy,
		@Origin Method method,
		@FieldValue(INVOCATION_HANDLER_FIELD) ByteBuddyDispatcherInvocationHandler<?> handler,
		@SuperCall(nullIfImpossible = true, serializableProxy = true, fallbackToDefault = false) Callable<Object> superMethod,
		@DefaultCall(nullIfImpossible = true, serializableProxy = true) Callable<Object> defaultMethod,
		@AllArguments Object[] args
	) throws Throwable {
		if (handler == null) {
			return superMethod.call();
		} else {
			return handler.interceptMethodCall(proxy, method, superMethod, defaultMethod, args);
		}
	}

	public Object interceptMethodCall(
		Object proxy,
		Method method,
		Callable<Object> superMethod,
		Callable<Object> defaultMethod,
		Object[] args
	) throws Throwable {
		final ClassMethodCacheKey cacheKey = this.createCacheKey(proxy.getClass(), proxyState, method);
		// issue https://github.com/raphw/byte-buddy/issues/1177
		final Callable<Object> superCallable;
		if (defaultMethod == null && method.isDefault()) {
			final MethodHandle methodHandle = ByteBuddyProxyGenerator.DEFAULT_METHOD_CACHE.computeIfAbsent(cacheKey, ck -> findMethodHandle(ck.getMethod()));
			superCallable = () -> {
				try {
					return methodHandle.bindTo(proxy).invokeWithArguments(args);
				} catch (Throwable e) {
					throw new InvocationTargetException(e);
				}
			};
		} else {
			superCallable = defaultMethod != null ? defaultMethod : superMethod;
		}

		// COMPUTE IF ABSENT = GET FROM MAP, IF MISSING OR INVALID -> COMPUTE, STORE AND RETURN RESULT OF LAMBDA
		@SuppressWarnings("rawtypes") CurriedMethodContextInvocationHandler invocationHandler = ByteBuddyProxyGenerator.CLASSIFICATION_CACHE.get(cacheKey);
		if (invocationHandler == null) {
			invocationHandler = this.getCurriedMethodContextInvocationHandler(method);
			ByteBuddyProxyGenerator.CLASSIFICATION_CACHE.put(cacheKey, invocationHandler);
		}
		// INVOKE CURRIED LAMBDA
		//noinspection unchecked
		return invocationHandler.invoke(
			proxy, method, args, proxyState, superCallable
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addImplementationSpecificInvokers(List<MethodClassification<?, ?>> methodClassifications) {
		methodClassifications.add(StandardJavaMethods.cloneMethodInvoker(objectToClone -> {
			try {
				final Field field = objectToClone.getClass().getDeclaredField(INVOCATION_HANDLER_FIELD);
				field.setAccessible(true);
				final ByteBuddyDispatcherInvocationHandler<T> originalDispatcher = (ByteBuddyDispatcherInvocationHandler<T>) field.get(objectToClone);

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
				return ByteBuddyProxyGenerator.instantiate(
					new ByteBuddyDispatcherInvocationHandler<>(
						clonedState,
						originalDispatcher.methodClassifications
					),
					interfaces.toArray(EMPTY_CLASS_ARRAY)
				);
			} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
				throw new CloneNotSupportedException("Cannot clone the proxy instance due to: " + e.getMessage());
			}
		}));
	}

}
