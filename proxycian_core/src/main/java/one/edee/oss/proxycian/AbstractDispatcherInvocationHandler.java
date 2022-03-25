package one.edee.oss.proxycian;

import lombok.RequiredArgsConstructor;
import one.edee.oss.proxycian.cache.ClassMethodCacheKey;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.StandardJavaMethods;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

public abstract class AbstractDispatcherInvocationHandler<T> implements DispatcherInvocationHandler {
	/* proxyState object unique to each proxy instance */
	protected final T proxyState;
	/* contains objects that need to be part of the caching key */
	protected final Object[] cacheKey;
	/* ordered list of method classifications - ie atomic features of the proxy */
	protected final List<MethodClassification<?, ?>> methodClassifications = new LinkedList<>();

    protected AbstractDispatcherInvocationHandler(T proxyState, MethodClassification<?, ?>... methodClassifications) {
		this.proxyState = proxyState;
	    // first add infrastructural ProxyStateAccessor handling
	    this.methodClassifications.add(ProxyStateAccessor.getProxyStateMethodInvoker());
	    // then add all method classifications developer wants
	    Collections.addAll(this.methodClassifications, methodClassifications);
		// then add all standard Java Object features
		this.methodClassifications.add(StandardJavaMethods.hashCodeMethodInvoker());
		this.methodClassifications.add(StandardJavaMethods.equalsMethodInvoker());
		this.methodClassifications.add(StandardJavaMethods.toStringMethodInvoker());
		// then implementation specific standard Java Object features - eg serialization
	    addImplementationSpecificInvokers(this.methodClassifications);
		// finally call super method handler
	    this.methodClassifications.add(StandardJavaMethods.realMethodInvoker());
		// now compute the cache key
	    this.cacheKey = this.methodClassifications
		    .stream()
		    .filter(it -> it instanceof CacheKeyAffectingMethodClassification)
		    .map(it -> ((CacheKeyAffectingMethodClassification)it).getCacheKey())
		    .filter(Objects::nonNull)
		    .toArray(Object[]::new);
    }

	protected AbstractDispatcherInvocationHandler(T proxyState, Collection<MethodClassification<?, ?>> methodClassifications) {
		this.proxyState = proxyState;
		this.methodClassifications.addAll(methodClassifications);
		// now compute the cache key
		this.cacheKey = this.methodClassifications
			.stream()
			.filter(it -> it instanceof CacheKeyAffectingMethodClassification)
			.map(it -> ((CacheKeyAffectingMethodClassification)it).getCacheKey())
			.filter(Objects::nonNull)
			.toArray(Object[]::new);
	}

	protected abstract void addImplementationSpecificInvokers(List<MethodClassification<?, ?>> methodClassifications);

	@SuppressWarnings("unchecked")
	protected <U> CurriedMethodContextInvocationHandler<U, T> getCurriedMethodContextInvocationHandler(Method method) {
		final List<CurriedMethodContextInvocationHandler<U, T>> matchingHandlers = new LinkedList<>();
		for (MethodClassification<?, ?> methodClassification : methodClassifications) {
			//create curried invocation handler (invocation handler curried with method state)
			final CurriedMethodContextInvocationHandler<U, T> curriedMethodInvocationHandler = ((MethodClassification<U, T>) methodClassification).createCurriedMethodContextInvocationHandler(method, proxyState);
			//filter out empty results - NULL results means no match
			if (curriedMethodInvocationHandler != null) {
				matchingHandlers.add(curriedMethodInvocationHandler);
				// matching method classifier is by default greedy - discarding all other classifiers in chain
				// but this can be changed by @Continuing annotation
				if (!(methodClassification instanceof TransparentMethodClassification)) {
					break;
				}
			}
		}

		if (matchingHandlers.isEmpty()) {
			//return missing invocation handler throwing exception
			return StandardJavaMethods.missingImplementationInvoker();
		} else if (matchingHandlers.size() == 1) {
			return matchingHandlers.get(0);
		} else {
			return fabricateComposedMethodInvocationHandler(matchingHandlers);
		}
    }

	protected ClassMethodCacheKey createCacheKey(@Nonnull Class<?> aClass, @Nonnull Class<?> proxyStateClazz, @Nonnull Method method) {
		return new ClassMethodCacheKey(aClass, proxyStateClazz, method, cacheKey);
	}

	public <PROXY, PROXY_STATE> CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> fabricateComposedMethodInvocationHandler(List<CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE>> nestedClassifications) {
		CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> translatedInvoker = new TailMethodInvocationHandler<>(
			nestedClassifications.get(nestedClassifications.size() - 1)
		);
		for (int i = nestedClassifications.size() - 2; i >= 0; i--) {
			final CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> methodHandler = nestedClassifications.get(i);
			translatedInvoker = new DelegatingMethodInvocationHandler<>(methodHandler, translatedInvoker);
		}
		return translatedInvoker;
	}

	@RequiredArgsConstructor
	private static class TailMethodInvocationHandler<PROXY, PROXY_STATE> implements CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> {
		private final CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> delegate;

		@Override
		public Object invoke(PROXY proxy, Method method, Object[] args, PROXY_STATE proxy_state, Callable<Object> invokeSuper) throws InvocationTargetException {
			return delegate.invoke(proxy, method, args, proxy_state, invokeSuper);
		}

		@Override
		public String toString() {
			return delegate.toString();
		}
	}

	@RequiredArgsConstructor
	private static class DelegatingMethodInvocationHandler<PROXY, PROXY_STATE> implements CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> {
		private final CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> delegate;
		private final CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> continuation;

		@Override
		public Object invoke(PROXY proxy, Method method, Object[] args, PROXY_STATE proxy_state, Callable<Object> invokeSuper) throws InvocationTargetException {
			return delegate.invoke(proxy, method, args, proxy_state, () -> continuation.invoke(proxy, method, args, proxy_state, invokeSuper));
		}

		@Override
		public String toString() {
			return delegate.toString();
		}
	}

}
