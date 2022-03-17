package one.edee.oss.proxycian.recipe;

import javax.annotation.Nonnull;

/**
 * This interface allows proxy states to verify their compatibility with interface required by the {@link ProxyRecipe}
 * list of advices.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface SelfVerifiableState {

	/**
	 * Returns TRUE if the proxy state is compatible with passed interface.
	 */
	boolean verifyCompatibility(@Nonnull Object proxyState, @Nonnull Class<?> withRequestedInterface);

}
