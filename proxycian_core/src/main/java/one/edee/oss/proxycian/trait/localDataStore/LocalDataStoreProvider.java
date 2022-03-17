package one.edee.oss.proxycian.trait.localDataStore;

import java.io.Serializable;
import java.util.Map;

/**
 * This interface must be implemented by the proxy state object in order {@link LocalDataStoreAdvice} can work.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface LocalDataStoreProvider {

	/**
	 * Returns existing or creates new memory Map data store for data written by {@link LocalDataStore}.
	 */
	Map<String, Serializable> getOrCreateLocalDataStore();

	/**
	 * Returns existing memory Map data store for data written by {@link LocalDataStore}. Might return null, if there
	 * are no data written yet.
	 */
	Map<String, Serializable> getLocalDataStoreIfPresent();

}
