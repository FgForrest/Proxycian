package one.edee.oss.proxycian.trait.delegate;

import one.edee.oss.proxycian.CacheKeyAffectingMethodClassification;
import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.recipe.IntroductionAdvice;
import one.edee.oss.proxycian.recipe.SelfVerifiableState;
import one.edee.oss.proxycian.util.ReflectionUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * This advice delegates calls of methods defined on passed interface (class) to the method on state object if the state
 * object defines method with exactly same signature.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DelegateCallsAdvice<T> implements IntroductionAdvice<T>, SelfVerifiableState {
	private static final long serialVersionUID = 8341706887626479107L;
	private final Class<T> iface;
	private final Function<Object, Object> delegateAccessor;
	private final boolean considerVerified;

	private DelegateCallsAdvice(Class<T> iface) {
		this.iface = iface;
		this.delegateAccessor = null;
		this.considerVerified = false;
	}

	public DelegateCallsAdvice(Class<T> iface, Function<Object, Object> delegateAccessor) {
		this.iface = iface;
		this.delegateAccessor = delegateAccessor;
		this.considerVerified = false;
	}

	private DelegateCallsAdvice(Class<T> iface, boolean considerVerified) {
		this.iface = iface;
		this.delegateAccessor = null;
		this.considerVerified = considerVerified;
	}

	public DelegateCallsAdvice(Class<T> iface, Function<Object, Object> delegateAccessor, boolean considerVerified) {
		this.iface = iface;
		this.delegateAccessor = delegateAccessor;
		this.considerVerified = considerVerified;
	}

	public static <T> DelegateCallsAdvice<T> getInstance(Class<T> iface) {
		return new DelegateCallsAdvice<>(iface);
	}

	public static <T> DelegateCallsAdvice<T> getInstance(Class<T> iface, Function<Object, Object> delegateAccessor) {
		return new DelegateCallsAdvice<>(iface, delegateAccessor);
	}

	public static <T> DelegateCallsAdvice<T> getInstanceValidFor(Class<T> iface) {
		return new DelegateCallsAdvice<>(iface, true);
	}

	public static <T> DelegateCallsAdvice<T> getInstanceValidFor(Class<T> iface, Function<Object, Object> delegateAccessor) {
		return new DelegateCallsAdvice<>(iface, delegateAccessor, true);
	}

	@Override
	public boolean verifyCompatibility(@Nonnull Object proxyState, @Nonnull Class<?> withRequestedInterface) {
		return considerVerified || withRequestedInterface.isInstance(delegateAccessor == null ? proxyState : delegateAccessor.apply(proxyState));
	}

	@Override
	public Class<T> getRequestedStateContract() {
		return iface;
	}

	@Override
	public List<MethodClassification<?, T>> getMethodClassification() {
		return Collections.singletonList(
			new DelegatingMethodClassification<>(iface, delegateAccessor)
		);
	}

	@Override
	public List<Class<?>> getInterfacesToImplement() {
		if (iface.isInterface()) {
			return Collections.singletonList(iface);
		} else {
			return Arrays.asList(iface.getInterfaces());
		}
	}

	/**
	 * Special classification that carries {@link #delegateAccessor} around with it. This lambda must be part of the
	 * caching key so that multiple {@link DelegateCallsAdvice} can be used for different proxies.
	 *
	 * @param <U>
	 * @param <S>
	 */
	public static class DelegatingMethodClassification<U, S> extends PredicateMethodClassification<U, Method, S> implements CacheKeyAffectingMethodClassification {
		private final Function<Object, Object> delegateAccessor;

		public DelegatingMethodClassification(Class<S> iface, Function<Object, Object> delegateAccessor) {
			super(
				/* description */   "Delegate to " + (delegateAccessor == null ? "state" : "state property"),
				/* matcher */       (method, proxyState)-> {
					final Object targetState = delegateAccessor == null ? proxyState : delegateAccessor.apply(proxyState);
					return ReflectionUtils.isMatchingMethodPresentOn(method, iface) && ReflectionUtils.isMatchingMethodPresentOn(method, targetState.getClass());
				},
				/* methodContext */ (method, proxyState) -> {
					try {
						final Object targetState = delegateAccessor == null ? proxyState : delegateAccessor.apply(proxyState);
						return targetState.getClass().getMethod(method.getName(), method.getParameterTypes());
					} catch (NoSuchMethodException e) {
						throw new IllegalStateException("Method " + method.toGenericString() + " is unexpectedly not defined on " + iface.toString() + "!");
					}
				},
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					final Object targetState = delegateAccessor == null ? proxyState : delegateAccessor.apply(proxyState);
					return methodContext.invoke(targetState, args);
				}
			);
			this.delegateAccessor = delegateAccessor;
		}

		@Override
		public Object getCacheKey() {
			return delegateAccessor;
		}
	}

}
