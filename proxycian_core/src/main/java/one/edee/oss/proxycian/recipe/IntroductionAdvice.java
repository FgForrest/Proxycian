package one.edee.oss.proxycian.recipe;

import one.edee.oss.proxycian.PredicateMethodClassification;

import java.util.List;

/**
 * Introduction advice is a logic related to specific interface or set of interfaces. It provides listing of the interfaces
 * it handles logic for and also should provide complete list of {@link PredicateMethodClassification} that fill in the logic
 * connected with particular interfaces.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface IntroductionAdvice<S> extends Advice<S> {

	/**
	 * Returns list of interfaces that should the proxy implement when this advice is added.
	 */
	List<Class<?>> getInterfacesToImplement();

}
