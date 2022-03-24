package one.edee.oss.proxycian;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface MethodInvocationHandler<PROXY, METHOD_CONTEXT, PROXY_STATE> {

	/**
	 * This method mimics {@link java.lang.reflect.InvocationHandler} interface but accepts memoized information
	 * of proxyState and methodContext.
	 *
	 * @param proxy reference to the proxy instance
	 * @param method reference to the proxy method (or super method if supported)
	 * @param args arguments of method invocations
	 * @param methodContext memoized method context that contains parsed information from method name, args, params etc.
	 * @param proxyState references to the state object unique for each proxy instance
	 * @param invokeSuper allows to optionally execute method on superclass
	 * @return the result of the method invocation
	 * @throws InvocationTargetException in case the call of the method fails
	 */
    Object invoke(PROXY proxy, Method method, Object[] args, METHOD_CONTEXT methodContext, PROXY_STATE proxyState, Callable<Object> invokeSuper) throws InvocationTargetException;

}
