package one.edee.oss.proxycian.cache;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class can be used as caching key for accessing constructors in particular class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ConstructorCacheKey {
	private static final Class<?>[] EMPTY_ARGS = new Class[0];
	@Getter private final Class<?> clazz;
	@Getter private final Class<?>[] argumentTypes;
	private final int hashCode;

	public ConstructorCacheKey(Class<?> clazz) {
		this.clazz = clazz;
		this.argumentTypes = EMPTY_ARGS;
		this.hashCode = clazz.hashCode();
	}

	public ConstructorCacheKey(Class<?> clazz, Class<?>[] argumentTypes) {
		this.clazz = clazz;
		this.argumentTypes = argumentTypes;
		int hashCode = Objects.hash(clazz);
		hashCode = 31 * hashCode + Arrays.hashCode(argumentTypes);
		this.hashCode = hashCode;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (((ConstructorCacheKey) o).hashCode != this.hashCode) return false;
		ConstructorCacheKey that = (ConstructorCacheKey) o;
		return clazz.equals(that.clazz) && Arrays.equals(argumentTypes, that.argumentTypes);
	}
}
