package one.edee.oss.proxycian.trait.beanMemoryStore;

import java.io.Serializable;

/**
 * Proxies with this unimplemented interface will get local data store support by {@link BeanMemoryStore}.
 * Advice must be added to the recipe in order to enable this functionality.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
public interface BeanMemoryStore {

	/**
	 * Stores data to memory store under the passed name.
	 */
	<T extends Serializable> void putValueToMemoryStore(String name, T value);

	/**
	 * Returns data stored in memory store under the passed name.
	 */
	<T extends Serializable> T getValueFromMemoryStore(String name);

	/**
	 * Adds new item to the collection stored in memory under the passed name.
	 */
	<T extends Serializable> boolean addValueToCollectionInMemoryStore(String name, T value);

	/**
	 * Removes item from the collection stored in memory under the passed name and returns TRUE if the item was
	 * successfully removed, FALSE otherwise.
	 */
	<T extends Serializable> boolean removeValueFromCollectionInMemoryStore(String name, T value);
}
