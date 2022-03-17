package one.edee.oss.proxycian.trait.localDataStore;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Proxies with this unimplemented interface will get local data store support by {@link LocalDataStore}.
 * Advice must be added to the recipe in order to enable this functionality.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
public interface LocalDataStore {

	/**
	 * Returns names of all data stored in the {@link LocalDataStore} instance.
	 * @return
	 */
	Collection<String> getLocalDataNames();

	/**
	 * Returns data stored in local store under the passed name.
	 * @param name
	 * @param <T>
	 * @return
	 */
	<T extends Serializable> T getLocalData(String name);

	/**
	 * Returns data stored in local store under automatically computed name from the class name.
	 * @param dataType
	 * @param <T>
	 * @return
	 */
	<T extends Serializable> T getLocalData(Class<T> dataType);

	/**
	 * Stores data to local store under the passed name.
	 * @param name
	 * @param value
	 * @param <T>
	 */
	<T extends Serializable> void setLocalData(String name, T value);

	/**
	 * Stores data to local store under automatically computed name from the class name.
	 * @param value
	 * @param <T>
	 */
	<T extends Serializable> void setLocalData(T value);

	/**
	 * Removes data stored in local store under the passed name.
	 * @param name
	 * @param <T>
	 * @return
	 */
	<T extends Serializable> T removeLocalData(String name);

	/**
	 * Removes data stored in local store under the passed name.
	 * @param dataType
	 * @param <T>
	 * @return
	 */
	<T extends Serializable> T removeLocalData(Class<T> dataType);

	/**
	 * Clears all local data store.
	 * @return
	 */
	void clearLocalData();

	/**
	 * Returns data stored in local store under the passed name. If there is none, new data are computed and stored
	 * to local store by passed lambda function.
	 *
	 * @param name
	 * @param dataSupplier
	 * @param <T>
	 */
	<T extends Serializable> T computeLocalDataIfAbsent(String name, Supplier<T> dataSupplier);

	/**
	 * Returns data stored in local store under automatically computed name from the class name. If there is none,
	 * new data are computed and stored to local store by passed lambda function.
	 *
	 * Beware: proper data type cannot be resolved when dataSupplier is a lambda function - cache will not be ever used
	 * in such case. Please use {@link #computeLocalDataIfAbsent(String, Supplier)} in case you want to use lambda
	 * expression.
	 *
	 * @param dataSupplier
	 * @param <T>
	 */
	<T extends Serializable> T computeLocalDataIfAbsent(Supplier<T> dataSupplier);

}
