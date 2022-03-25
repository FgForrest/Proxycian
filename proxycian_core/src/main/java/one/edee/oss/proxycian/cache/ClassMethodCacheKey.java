package one.edee.oss.proxycian.cache;

import lombok.Getter;
import one.edee.oss.proxycian.PredicateMethodClassification;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class should be used as caching key for {@link PredicateMethodClassification} instances.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ClassMethodCacheKey {
	@Getter private final Class<?> clazz;
	@Getter private final Object proxyStateCacheKey;
	@Getter private final Method method;
	private final Object[] cacheKey;
	private final int hash;

	public ClassMethodCacheKey(@Nonnull Class<?> clazz, @Nonnull Object proxyStateCacheKey, @Nonnull Method method, @Nonnull Object[] cacheKey) {
		this.clazz = clazz;
		this.proxyStateCacheKey = proxyStateCacheKey;
		this.method = method;
		this.cacheKey = cacheKey;
		this.hash = Objects.hash(clazz, proxyStateCacheKey, method, Arrays.hashCode(cacheKey));
	}

	@Override
	public int hashCode() {
		return this.hash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (((ClassMethodCacheKey) o).hash != this.hash) return false;
		ClassMethodCacheKey that = (ClassMethodCacheKey) o;
		return Objects.equals(clazz, that.clazz) &&
			Objects.equals(proxyStateCacheKey, that.proxyStateCacheKey) &&
			Objects.equals(method, that.method) &&
			Arrays.equals(cacheKey, that.cacheKey);
	}
}
