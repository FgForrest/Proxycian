package one.edee.oss.proxycian.trait.groovy;

import groovy.lang.MetaClass;

/**
 * This interface must be implemented by the proxy state object in order {@link GroovyObjectAdvice} can work.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface GroovyStateProvider {

	/**
	 * Returns Groovy base class.
	 */
	Class<?> getBaseClass();

	/**
	 * Returns Groovy metaclass if it was previously set.
	 */
	MetaClass getMetaClass();

	/**
	 * Stores Groovy metaclass.
	 */
	void setMetaClass(MetaClass metaClass);
}
