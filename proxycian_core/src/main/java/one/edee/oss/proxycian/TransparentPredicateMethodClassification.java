package one.edee.oss.proxycian;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Method classification is the same as {@link PredicateMethodClassification} but is marked with
 * {@link TransparentMethodClassification} that allows checking additional {@link MethodClassification} in an examination
 * chain.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class TransparentPredicateMethodClassification<PROXY, METHOD_CONTEXT, PROXY_STATE> extends PredicateMethodClassification<PROXY, METHOD_CONTEXT, PROXY_STATE> implements TransparentMethodClassification {

	public TransparentPredicateMethodClassification(String description, BiPredicate<Method, PROXY_STATE> methodMatcher, BiFunction<Method, PROXY_STATE, METHOD_CONTEXT> methodContextFactory, MethodInvocationHandler<PROXY, METHOD_CONTEXT, PROXY_STATE> invocationHandler) {
		super(description, methodMatcher, methodContextFactory, invocationHandler);
	}

}
