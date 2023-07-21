package one.edee.oss.proxycian.bytebuddy;


import net.bytebuddy.implementation.bind.annotation.*;
import one.edee.oss.proxycian.CurriedMethodContextInvocationHandler;
import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.cache.ClassMethodCacheKey;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator.INVOCATION_HANDLER_FIELD;

public class ByteBuddyDispatcherInvocationHandler<T> extends AbstractByteBuddyDispatcherInvocationHandler<T, ByteBuddyDispatcherInvocationHandler<T>> {

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
		@Origin MethodHandles.Lookup lookup,
		@FieldValue(INVOCATION_HANDLER_FIELD) ByteBuddyDispatcherInvocationHandler<?> handler,
		@SuperCall(nullIfImpossible = true, serializableProxy = true, fallbackToDefault = false) Callable<Object> superMethod,
		@DefaultCall(nullIfImpossible = true, serializableProxy = true) Callable<Object> defaultMethod,
		@AllArguments Object[] args
	) throws Throwable {
		if (handler == null) {
			return superMethod.call();
		} else {
			return handler.interceptMethodCall(proxy, method, lookup, superMethod, defaultMethod, args);
		}
	}

	public Object interceptMethodCall(
		Object proxy,
		Method method,
		MethodHandles.Lookup lookup,
		Callable<Object> superMethod,
		Callable<Object> defaultMethod,
		Object[] args
	) throws Throwable {
		try {
			final ClassMethodCacheKey cacheKey = this.createCacheKey(proxy.getClass(), proxyState, method);

			final Callable<Object> superCallable;
			if (method.isDefault()) {
				if (defaultMethod == null) {
					superCallable = () -> {
						try {
							return MethodHandles.privateLookupIn(method.getDeclaringClass(), lookup)
								.findSpecial(
									method.getDeclaringClass(),
									method.getName(),
									MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
									method.getDeclaringClass()
								)
								.bindTo(proxy)
								.invokeWithArguments(args);
						} catch (Throwable e) {
							throw new InvocationTargetException(e);
						}
					};
				} else {
					superCallable = defaultMethod;
				}
			} else {
				superCallable = superMethod;
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
		} catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	@Override
	protected ProxyStateAccessor makeClone(ByteBuddyDispatcherInvocationHandler<T> originalDispatcher, T clonedState, List<Class<?>> interfaces) {
		return ByteBuddyProxyGenerator.instantiate(
				new ByteBuddyDispatcherInvocationHandler<>(
						clonedState,
						originalDispatcher.methodClassifications
				),
				interfaces.toArray(EMPTY_CLASS_ARRAY)
		);
	}
}
