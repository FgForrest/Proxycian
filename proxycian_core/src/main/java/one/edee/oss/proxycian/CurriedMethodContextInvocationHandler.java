package one.edee.oss.proxycian;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> {

	/**
	 * This method is variant of {@link MethodInvocationHandler#invoke(Object, Method, Object[], Object, Object, Callable)}  method,
	 * but except methodContext parameter which will be curried and remembered in the internal state of this lambda.
	 *
	 * @param proxy             reference to the proxy instance
	 * @param method            reference to the proxy method (or super method if supported)
	 * @param args              arguments of method invocations
	 * @param proxyState        references to the state object unique for each proxy instance
	 * @param invokeSuper       allows to optionally execute method on superclass
	 * @return result of the invocation
	 * @throws InvocationTargetException propagated exception if thrown in invocation
	 */
	Object invoke(PROXY proxy, Method method, Object[] args, PROXY_STATE proxyState, Callable<Object> invokeSuper) throws InvocationTargetException;

}
