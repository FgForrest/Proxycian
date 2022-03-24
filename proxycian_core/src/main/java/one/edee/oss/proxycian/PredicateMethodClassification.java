package one.edee.oss.proxycian;

import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Method classification wraps detection and handling logic for method calls upon proxy.
 * Predicate {@link #methodMatcher} allows identifying whether particular method call should be intercepted, transformation
 * lambda {@link #methodContextFactory} will extract information from the call site (usually method signature) and pass
 * it to the {@link #invocationHandler} that handles the call using the information from the {@link #methodContextFactory}.
 *
 * Predicate and result of transformation lambda are cached and thus only {@link #invocationHandler} is executed for
 * second and additional calls of the same method on the proxy.
 */
@RequiredArgsConstructor
public class PredicateMethodClassification<PROXY, METHOD_CONTEXT, PROXY_STATE> implements MethodClassification<PROXY, PROXY_STATE> {

	/**
	 * This description describes the method classification for the sake of developer orientation.
	 * Tt has no other purpose than to make situation clearer for the reader in debugging.
	 **/
	private final String description;

	/**
	 * This predicate checks method and returns true only if this classification should be applied on method
	 **/
	private final BiPredicate<Method, PROXY_STATE> methodMatcher;

	/**
	 * This factory function creates method context (ie. parsed data from method name, annotations and so on)
	 **/
	private final BiFunction<Method, PROXY_STATE, METHOD_CONTEXT> methodContextFactory;

	/**
	 * This returns something like invocation handler but with extended method arguments:
	 *
	 * Object invoke(PROXY proxy, Method method, Object[] args, METHOD_CONTEXT methodContext, PROXY_STATE proxyState) throws Throwable;
	 *
	 * This method handler will be called by DispatcherInvocationHandler for each method execution on proxy
	 */
	private final MethodInvocationHandler<PROXY, METHOD_CONTEXT, PROXY_STATE> invocationHandler;

	@Nullable
	@Override
	public CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> createCurriedMethodContextInvocationHandler(@Nonnull Method classificationMethod, @Nonnull PROXY_STATE proxyState) {
		if (methodMatcher.test(classificationMethod, proxyState)) {
			final METHOD_CONTEXT methodContext = methodContextFactory.apply(classificationMethod, proxyState);
			return new MethodClassificationCurriedMethodContextInvocationHandler<>(
				description, methodContext, invocationHandler
			);
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return description;
	}

	/**
	 * Curried {@link MethodInvocationHandler} that carries method context and description along.
	 */
	protected static class MethodClassificationCurriedMethodContextInvocationHandler<THE_PROXY, THE_METHOD_CONTEXT, THE_PROXY_STATE> implements CurriedMethodContextInvocationHandler<THE_PROXY, THE_PROXY_STATE> {
		private final String description;
		private final THE_METHOD_CONTEXT methodContext;
		private final MethodInvocationHandler<THE_PROXY, THE_METHOD_CONTEXT, THE_PROXY_STATE> invocationHandler;

		public MethodClassificationCurriedMethodContextInvocationHandler(String description, THE_METHOD_CONTEXT methodContext, MethodInvocationHandler<THE_PROXY, THE_METHOD_CONTEXT, THE_PROXY_STATE> invocationHandler) {
			this.description = description;
			this.methodContext = methodContext;
			this.invocationHandler = invocationHandler;
		}

		@Override
		public Object invoke(THE_PROXY proxy, Method executionMethod, Object[] args, THE_PROXY_STATE proxyState, Callable<Object> invokeSuper) throws InvocationTargetException {
			return invocationHandler.invoke(
				proxy, executionMethod, args, methodContext, proxyState, invokeSuper
			);
		}

		@Override
		public String toString() {
			return description + (methodContext == null ? "" : ": " + methodContext);
		}
	}

}
