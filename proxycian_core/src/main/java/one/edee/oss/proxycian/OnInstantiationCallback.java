package one.edee.oss.proxycian;

import java.io.Serializable;

/**
 * Client may provide implementation of this interface to the proxy instantiation method call if it requires to intercept
 * the proxy just after being instantiated but before method handler is set. Implementation of this interface cannot get
 * advantage of {@link MethodInvocationHandler} because they aren't initialized yet but can access the proxy instance
 * itself and call non-intercepted methods on it.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface OnInstantiationCallback extends Serializable {

	/**
	 * Default implementation does nothing.
	 */
	OnInstantiationCallback DEFAULT = (proxy, proxyState) -> { /* do nothing */ };

	/**
	 * Post-process proxy instance just after it was instantiated. No {@link MethodInvocationHandler} interception will
	 * be executed in this phase because the proxy is not yet fully initialized.
	 */
	void proxyCreated(Object proxy, Object proxyState);

}
