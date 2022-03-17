package one.edee.oss.proxycian;

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
	protected <U> CurriedMethodContextInvocationHandler<T, U> getCurriedMethodContextInvocationHandler(Method method) {
        return methodClassifications.stream()
                //create curried invocation handler (invocation handler curried with method state)
                .map(methodClassification -> ((MethodClassification<U, T>)methodClassification).createCurriedMethodContextInvocationHandler(method, proxyState))
	            //filter out empty results - NULL results means no match
	            .filter(Objects::nonNull)
				//return first matching curried method context
                .findFirst()
                //return missing invocation handler throwing exception
                .orElse(StandardJavaMethods.missingImplementationInvoker());
    }

	protected ClassMethodCacheKey createCacheKey(@Nonnull Class<?> aClass, @Nonnull Class<?> proxyStateClazz, @Nonnull Method method) {
		return new ClassMethodCacheKey(aClass, proxyStateClazz, method, cacheKey);
	}
}
