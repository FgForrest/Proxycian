package one.edee.oss.proxycian;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Method classification is the same as {@link DirectMethodClassification} but is marked with
 * {@link TransparentMethodClassification} that allows checking additional {@link MethodClassification} in an examination
 * chain.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class TransparentDirectMethodClassification<PROXY, PROXY_STATE> extends DirectMethodClassification<PROXY, PROXY_STATE> implements TransparentMethodClassification {

	public TransparentDirectMethodClassification(String description, BiFunction<Method, PROXY_STATE, CurriedMethodContextInvocationHandler<PROXY, PROXY_STATE>> invocationHandlerFactory) {
		super(description, invocationHandlerFactory);
	}

}
