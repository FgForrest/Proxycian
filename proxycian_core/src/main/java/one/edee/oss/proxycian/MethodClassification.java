package one.edee.oss.proxycian;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Method classification defines an interface for selecting the {@link MethodInvocationHandler} that will react to
 * the particular method call on the proxy.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface MethodClassification<PROXY, PROXY_STATE> {

	/**
	 * No operation lambda creating void methodContext.
	 **/
	static <PROXY_STATE> BiFunction<Method, PROXY_STATE, Void> noContext() {
		return new NoContextFunction<>();
	}

	/**
	 * Creates lambda function that wraps method context along with execution logic.
	 * It gets advantage of currying execution lambda with method context.
	 *
	 * @param classificationMethod method that matched
	 * @param proxyState           backing proxy state object
	 * @return lambda with method context baked in, so that only proxy, method and args are necessary to invoke logic
	 */
	@Nullable
	CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> createCurriedMethodContextInvocationHandler(@Nonnull Method classificationMethod, @Nonnull PROXY_STATE proxyState);

	/**
	 * Shortcut method context when we don't need any context.
	 */
	class NoContextFunction<THE_PROXY_STATE> implements BiFunction<Method, THE_PROXY_STATE, Void> {

		@Override
		public Void apply(Method method, THE_PROXY_STATE proxyState) {
			return null;
		}

		@Override
		public String toString() {
			return "noContext";
		}
	}
}
