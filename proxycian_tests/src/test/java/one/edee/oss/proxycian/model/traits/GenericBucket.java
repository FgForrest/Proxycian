package one.edee.oss.proxycian.model.traits;

import lombok.Getter;
import one.edee.oss.proxycian.ProxyStateWithConstructorArgs;
import one.edee.oss.proxycian.trait.beanMemoryStore.BeanMemoryStore;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStoreProvider;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

@SuppressWarnings("ALL")
public class GenericBucket implements ProxyStateWithConstructorArgs, LocalDataStoreProvider, BeanMemoryStore, Cloneable {
	private static final long serialVersionUID = 4135508091866018653L;
	@Getter private final Class<?>[] constructorTypes;
	@Getter private final Object[] constructorArgs;
	private Map<String, Serializable> data;

	public GenericBucket() {
		this.constructorTypes = new Class[0];
		this.constructorArgs = new Object[0];
	}

	public GenericBucket(Class<?>[] constructorTypes, Object[] constructorArgs) {
		this.constructorTypes = constructorTypes;
		this.constructorArgs = constructorArgs;
	}

	private GenericBucket(Map<String, Serializable> data) {
		this();
		this.data = data;
	}

	public GenericBucket(Class<?>[] constructorTypes, Object[] constructorArgs, Map<String, Serializable> data) {
		this(constructorTypes, constructorArgs);
		this.data = data;
	}

	public Serializable get(String propertyName) {
		return ofNullable(getLocalDataStoreIfPresent())
			.map(it -> it.get(propertyName))
			.orElse(null);
	}

	public void set(String propertyName, Serializable propertyValue) {
		getOrCreateLocalDataStore().put(propertyName, propertyValue);
	}

	@Override
	public Map<String, Serializable> getOrCreateLocalDataStore() {
		if (data == null) {
			this.data = new LinkedHashMap<>(16);
		}
		return data;
	}

	@Override
	public Map<String, Serializable> getLocalDataStoreIfPresent() {
		return data;
	}

	@Override
	public <T extends Serializable> void putValueToMemoryStore(String name, T value) {
		getOrCreateLocalDataStore().put(name, value);
	}

	@Override
	public <T extends Serializable> T getValueFromMemoryStore(String name) {
		return (T) ofNullable(getLocalDataStoreIfPresent()).map(it -> it.get(name)).orElse(null);
	}

	@Override
	public <T extends Serializable> void addValueToCollectionInMemoryStore(String name, T value) {
		final List<T> dataStore = (List<T>) getOrCreateLocalDataStore().computeIfAbsent(name, s -> new LinkedList<>());
		dataStore.add(value);
	}

	@Override
	public <T extends Serializable> boolean removeValueFromCollectionInMemoryStore(String name, T value) {
		final List<T> collection = (List<T>) ofNullable(getLocalDataStoreIfPresent()).map(it -> it.get(name)).orElse(null);
		return collection == null ? false : collection.remove(value);
	}

	@Override
	public int hashCode() {
		return data.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		GenericBucket that = (GenericBucket)o;
		return data.equals(that.data);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new GenericBucket(this.data);
	}

	@Override
	public String toString() {
		return data == null ? "NO_DATA" : data.toString();
	}
}
