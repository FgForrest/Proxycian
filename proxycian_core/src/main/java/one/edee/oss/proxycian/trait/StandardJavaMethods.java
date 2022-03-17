package one.edee.oss.proxycian.trait;


import one.edee.oss.proxycian.CurriedMethodContextInvocationHandler;
import one.edee.oss.proxycian.PredicateMethodClassification;

import java.lang.reflect.Modifier;

import static one.edee.oss.proxycian.MethodClassification.noContext;
import static one.edee.oss.proxycian.util.ReflectionUtils.isMethodDeclaredOn;
import static one.edee.oss.proxycian.util.ReflectionUtils.isNonPublicMethodDeclaredOn;

public interface StandardJavaMethods {

	/** METHOD CONTRACT: catch all real (not abstract) methods and delegate calls to them **/
    static PredicateMethodClassification<ProxyStateAccessor, Void, Object> realMethodInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "delegate to default / super implementation",
        /* matcher */       (method, proxyState) -> !Modifier.isAbstract(method.getModifiers()) || method.isDefault(),
        /* methodContext */ noContext(),
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> invokeSuper.call()
        );
    }

	/** METHOD CONTRACT: String toString() **/
    static PredicateMethodClassification<ProxyStateAccessor, Void, Object> toStringMethodInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "Object.toString()",
        /* matcher */       (method, proxyState) -> isMethodDeclaredOn(method, Object.class, "toString"),
        /* methodContext */ noContext(),
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> proxyState.toString()
        );
    }

	/** METHOD CONTRACT: int hashCode() **/
    static PredicateMethodClassification<ProxyStateAccessor, Void, Object> hashCodeMethodInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "Object.hashCode()",
        /* matcher */       (method, proxyState) -> isMethodDeclaredOn(method, Object.class, "hashCode"),
        /* methodContext */ noContext(),
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> proxyState.hashCode()
        );
    }

	/** METHOD CONTRACT: boolean equals(Object o) **/
    static PredicateMethodClassification<ProxyStateAccessor, Void, Object> equalsMethodInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "Object.equals(Object)",
        /* matcher */       (method, proxyState) -> isMethodDeclaredOn(method, Object.class, "equals", Object.class),
        /* methodContext */ noContext(),
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
                                        args[0] != null &&
                                        proxy.getClass().equals(args[0].getClass()) &&
                                        proxyState.equals(((ProxyStateAccessor)args[0]).getProxyState())
        );
    }

	/** METHOD CONTRACT: Object clone() **/
	static PredicateMethodClassification<ProxyStateAccessor, Void, Object> cloneMethodInvoker(CloneLambda<ProxyStateAccessor> cloner) {
		return new PredicateMethodClassification<>(
			/* description */   "Object.clone(Object)",
			/* matcher */       (method, proxyState) -> isNonPublicMethodDeclaredOn(method, Object.class, "clone"),
			/* methodContext */ noContext(),
			/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> cloner.clone(proxy)
		);
	}

	/** METHOD CONTRACT: catch everything else and throw exception **/
	@SuppressWarnings("rawtypes")
	static CurriedMethodContextInvocationHandler missingImplementationInvoker() {
        return (proxy, method, args, proxyState, invokeSuper) -> {
            throw new UnsupportedOperationException(
                    "Method " + method.toGenericString() + " is not supported by this proxy!"
            );
        };
    }

	interface CloneLambda<T> {

		T clone(T t) throws CloneNotSupportedException;

	}

}
