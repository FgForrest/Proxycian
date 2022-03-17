package one.edee.oss.proxycian.recipe;

import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.PredicateMethodClassification;

import java.io.Serializable;
import java.util.List;

/**
 * Advice provides implementation logic for dynamic proxies. Advices MUST not keep any data connected with proxy instance.
 * All proxy related data are passed to the {@link PredicateMethodClassification} in proxy state object. Advices are recommended
 * to be singletons.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface Advice<S> extends Serializable {

	/**
	 * Returns the interface that must be implemented by proxy state in order this service could work.
	 */
	Class<S> getRequestedStateContract();

	/**
	 * Returns set of method classifications that can match and process methods of certain type.
	 */
	List<MethodClassification<?, S>> getMethodClassification();

}
