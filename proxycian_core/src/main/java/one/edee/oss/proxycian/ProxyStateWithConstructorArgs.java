package one.edee.oss.proxycian;

import java.io.Serializable;

/**
 * This interface must be implemented by proxy state object that is used for serializable proxy with non-default
 * constructor. The data are required to create instance of deserialized object.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface ProxyStateWithConstructorArgs extends Serializable {

	/**
	 * Returns types of the constructor to be used for deserialization.
	 */
	Class<?>[] getConstructorTypes();

	/**
	 * Returns objects that should be used for instantiation during deserialization.
	 */
	Object[] getConstructorArgs();

}
