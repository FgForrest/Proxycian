package one.edee.oss.proxycian.bytebuddy.trait.localDataStore;

import one.edee.oss.proxycian.bytebuddy.AbstractByteBuddyProxycianTest;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import one.edee.oss.proxycian.recipe.Advice;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStore;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStoreAdvice;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies contract of {@link LocalDataStoreAdvice}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class LocalDataStoreAdviceTest extends AbstractByteBuddyProxycianTest {

	@Test
	public void ByteBuddyProxyRecipeGenerator_shouldProxyAnythingAndAddLocalDataStore() {
		final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				new Class[] {StatelessClass.class},
				new Advice[] {LocalDataStoreAdvice.INSTANCE}
			),
			new GenericBucket()
		);

		assertTrue(theInstance instanceof StatelessClass);
		final StatelessClass proxy = (StatelessClass) theInstance;

		proxy.rememberValue("myNameIs", "Bond, James Bond");
		assertEquals("Bond, James Bond", proxy.recallValue("myNameIs"));
	}

	public abstract static class StatelessClass implements LocalDataStore {

		public void rememberValue(String name, Serializable value) {
			setLocalData(name, value);
		}

		public <T> T recallValue(String name) {
			return getLocalData(name);
		}

	}

}
