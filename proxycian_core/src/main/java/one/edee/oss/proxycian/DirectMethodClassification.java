package one.edee.oss.proxycian;

import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Method classification wraps detection and handling logic for method calls upon proxy.
 * This implementation returns {@link CurriedMethodContextInvocationHandler} that wrap both method context and method
 * implementation in single object. DirectMethodClassification might also return different implementations and reuse
 * logic for method context creation for multiple implementation paths. This variant of {@link MethodClassification}
 * is suitable for complex method signature matching logic with multiple different outputs.
 */
@RequiredArgsConstructor
public class DirectMethodClassification<PROXY, PROXY_STATE> implements MethodClassification<PROXY, PROXY_STATE> {

	/**
	 * This description describes the method classification for the sake of developer orientation.
	 * Tt has no other purpose than to make situation clearer for the reader in debugging.
	 **/
	private final String description;

	/**
	 * This factory function creates method context (ie. parsed data from method name, annotations and so on)
	 **/
	private final BiFunction<Method, PROXY_STATE, CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE>> invocationHandlerFactory;

	@Nullable
	@Override
	public CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE> createCurriedMethodContextInvocationHandler(@Nonnull Method classificationMethod, @Nonnull PROXY_STATE proxyState) {
		return invocationHandlerFactory.apply(classificationMethod, proxyState);
	}

	@Override
	public String toString() {
		return description;
	}

}
