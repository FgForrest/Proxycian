package one.edee.oss.proxycian;

/**
 * This interface must be implemented by all {@link PredicateMethodClassification} extensions that contains inner state. Inner
 * state then differentiates one {@link PredicateMethodClassification} from another and has to be taken into an account when
 * computing method call cache key.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface CacheKeyAffectingMethodClassification {

	/**
	 * Returns serializable cache key with proper equals and hash code implementation.
	 */
	Object getCacheKey();

}
