package one.edee.oss.proxycian.bytebuddy;

import lombok.Data;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import one.edee.oss.proxycian.recipe.Advice;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStore;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStoreAdvice;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ByteBuddyIssuesTest extends AbstractByteBuddyProxycianTest {

	@Test
	public void ByteBuddyProxyRecipeGenerator_Issue7_DefaultPreferredOverSuper() {
		final TestSuperclass theInstance = ByteBuddyProxyGenerator.instantiate(
			new ProxyRecipe(
				new Class[] { TestSuperclass.class, InterfaceWithDefaultImplementation.class },
				new Advice[] { LocalDataStoreAdvice.INSTANCE }
			),
			new GenericBucket()
		);

		assertTrue(theInstance instanceof LocalDataStore);
		assertEquals("Hello there", theInstance.sayHello());
	}

	public void newMethod() {
		System.out.println("YES");
	}

	@Data
	public static class TestSuperclass implements Serializable {

		public String sayHello() {
			return "Hello there";
		}
	}

	public interface InterfaceWithDefaultImplementation {

		default String sayHello() {
			return "Hello";
		}

	}

}
