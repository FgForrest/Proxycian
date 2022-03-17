package one.edee.oss.proxycian.trait;

import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.util.ReflectionUtils;

import static one.edee.oss.proxycian.MethodClassification.noContext;

public interface ProxyStateAccessor {

	/**
	 * METHOD CONTRACT: Object getProxyState()
	 **/
	static PredicateMethodClassification<ProxyStateAccessor, Void, Object> getProxyStateMethodInvoker() {
		return new PredicateMethodClassification<>(
			/* description */   "ProxyStateAccessor.getProxyState()",
			/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, ProxyStateAccessor.class, "getProxyState"),
			/* methodContext */ noContext(),
			/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> proxyState
		);
	}

	/**
	 * Returns internal state of the proxy that is unique to each instance.
	 */
	Object getProxyState();

}
