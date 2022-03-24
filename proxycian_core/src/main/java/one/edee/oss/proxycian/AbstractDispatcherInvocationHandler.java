package one.edee.oss.proxycian;

import lombok.RequiredArgsConstructor;
import one.edee.oss.proxycian.cache.ClassMethodCacheKey;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.StandardJavaMethods;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract class AbstractDispatcherInvocationHandler<T> implements DispatcherInvocationHandler {
	/* proxyState object unique to each proxy instance */
	protected final T proxyState;
	/* contains objects that need to be part of the caching key */
	protected final Object[] cacheKey;
	/* ordered list of method classifications - ie atomic features of the proxy */
	protected final List<MethodClassification<?, ?>> methodClassifications = new LinkedList<>();

    protected AbstractDispatcherInvocationHandler(T proxyState, MethodClassification<?, ?>... methodClassifications) {
		this.proxyState = proxyState;
		// firstly add all standard Java Object features
		this.methodClassifications.add(StandardJavaMethods.hashCodeMethodInvoker());
		this.methodClassifications.add(StandardJavaMethods.equalsMethodInvoker());
		this.methodClassifications.add(StandardJavaMethods.toStringMethodInvoker());
		// implementation specific standard Java Object features
	    addImplementationSpecificInvokers(this.methodClassifications);
		// then add infrastructural ProxyStateAccessor handling
		this.methodClassifications.add(ProxyStateAccessor.getProxyStateMethodInvoker());
		// finally add all method classifications developer wants
		Collections.addAll(this.methodClassifications, methodClassifications);
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
		final List<CurriedMethodContextInvocationHandler<U, T>> matchingHandlers = methodClassifications.stream()
			//create curried invocation handler (invocation handler curried with method state)
			.map(methodClassification -> ((MethodClassification<U, T>) methodClassification).createCurriedMethodContextInvocationHandler(method, proxyState))
			//filter out empty results - NULL results means no match
			.filter(Objects::nonNull)
			// collect all
			.collect(Collectors.toList());

		if (matchingHandlers.isEmpty()) {
			//return missing invocation handler throwing exception
			return StandardJavaMethods.missingImplementationInvoker();
		} else {
			final CurriedMethodContextInvocationHandler<U, T> topMatchingHandler = matchingHandlers.get(0);
			if (matchingHandlers.size() == 1) {
				return topMatchingHandler;
			} else {
				final ContinuationInvokeSuperFactory<U, T> invokeSuperFactory = new ContinuationInvokeSuperFactory<>(matchingHandlers);
				return (proxy, interceptedMethod, args, proxyState, invokeSuper) ->
					topMatchingHandler.invoke(
						proxy, interceptedMethod, args, proxyState,
						invokeSuperFactory.fabricate(proxy, interceptedMethod, args, proxyState, invokeSuper)
					);
			}
		}
    }

	protected ClassMethodCacheKey createCacheKey(@Nonnull Class<?> aClass, @Nonnull Class<?> proxyStateClazz, @Nonnull Method method) {
		return new ClassMethodCacheKey(aClass, proxyStateClazz, method, cacheKey);
	}

	@RequiredArgsConstructor
	private static class ContinuationInvokeSuperFactory<PROXY, PROXY_STATE> {
		private final List<CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE>> nestedClassifications;

		public Callable<Object> fabricate(PROXY proxy, Method method, Object[] args, PROXY_STATE proxyState, Callable<Object> invokeSuper) {
			Callable<Object> nestedInvokeSuper = invokeSuper;
			for (int i = nestedClassifications.size() - 1; i > 0; i--) {
				final CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> methodHandler = nestedClassifications.get(i);
				final Callable<Object> finalNestedInvokeSuper = nestedInvokeSuper;
				nestedInvokeSuper = () -> methodHandler.invoke(proxy, method, args, proxyState, finalNestedInvokeSuper);
			}
			return nestedInvokeSuper;
		}

	}

}
