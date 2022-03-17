package one.edee.oss.proxycian.javassist.trait.delegate;

import lombok.Data;
import one.edee.oss.proxycian.JavassistProxyGenerator;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.delegate.DelegateCallsAdvice;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies contract of {@link DelegateCallsAdvice}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DelegateCallsAdviceTest {

	@Test
	public void JavassistRecipeGenerator_DelegateCalls() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				DelegateCallsAdvice.getInstance(SomePojo.class)
			),
			new SomePojo()
		);

		assertTrue(theInstance instanceof PojoInterface);
		final PojoInterface proxy = (PojoInterface) theInstance;

		proxy.setFirstName("Jan");
		proxy.setLastName("Novotný");
		assertEquals("Jan Novotný", proxy.getFullName());
	}

	@Test
	public void JavassistRecipeGenerator_DelegateCallsOnSubProperty() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				DelegateCallsAdvice.getInstance(SomePojo.class, o -> ((BroaderState)o).getSomePojo())
			),
			new BroaderState()
		);

		assertTrue(theInstance instanceof PojoInterface);
		final PojoInterface proxy = (PojoInterface) theInstance;

		proxy.setFirstName("Jan");
		proxy.setLastName("Novotný");
		assertEquals("Jan Novotný", proxy.getFullName());
	}

	@Data
	public static class SomePojo implements Serializable, PojoInterface {
		private static final long serialVersionUID = -3190496823269251991L;
		private String firstName;
		private String lastName;

		public String getFullName() {
			return firstName + " " + lastName;
		}

	}

	@Data
	public static class BroaderState implements Serializable {
		private static final long serialVersionUID = 3427985599976732264L;
		private final SomePojo somePojo = new SomePojo();

	}

	public interface PojoInterface {

		String getFullName();

		String getFirstName();

		String getLastName();

		void setFirstName(String firstName);

		void setLastName(String lastName);

	}

}