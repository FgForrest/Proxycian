package one.edee.oss.proxycian.bytebuddy.trait.delegate;

import lombok.Data;
import one.edee.oss.proxycian.bytebuddy.AbstractByteBuddyProxycianTest;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator;
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
public class DelegateCallsAdviceTest extends AbstractByteBuddyProxycianTest {

	@Test
	public void ByteBuddyProxyRecipeGenerator_DelegateCalls() {
		final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				DelegateCallsAdvice.getInstance(NameImplementation.class)
			),
			new NameImplementation()
		);

		assertTrue(theInstance instanceof NameInterface);
		final NameInterface proxy = (NameInterface) theInstance;

		proxy.setFirstName("Jan");
		proxy.setLastName("Novotný");
		assertEquals("Jan Novotný", proxy.getFullName());
	}

	@Test
	public void ByteBuddyProxyRecipeGenerator_DelegateCallsOnSubProperty() {
		final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				DelegateCallsAdvice.getInstance(NameInterface.class, o -> ((CompositionState)o).getNameHolder()),
				DelegateCallsAdvice.getInstance(AgeInterface.class, o -> ((CompositionState)o).getAgeHolder()),
				DelegateCallsAdvice.getInstance(PersonInterface.class)
			),
			new CompositionState()
		);

		assertTrue(theInstance instanceof NameInterface);
		final NameInterface nameProxyContract = (NameInterface) theInstance;
		nameProxyContract.setFirstName("Jan");
		nameProxyContract.setLastName("Novotný");

		assertTrue(theInstance instanceof AgeInterface);
		final AgeInterface ageProxyContract = (AgeInterface) theInstance;
		ageProxyContract.setAge(43);

		assertTrue(theInstance instanceof PersonInterface);
		assertEquals("Jan Novotný of age 43", ((PersonInterface)theInstance).getPersonDescription());
	}

	public interface NameInterface {

		String getFullName();

		String getFirstName();
		String getLastName();

		void setFirstName(String firstName);
		void setLastName(String lastName);

	}

	@Data
	public static class NameImplementation implements Serializable, NameInterface {
		private static final long serialVersionUID = -3190496823269251991L;
		private String firstName;
		private String lastName;

		public String getFullName() {
			return firstName + " " + lastName;
		}

	}

	public interface AgeInterface {

		int getAge();
		void setAge(int ageInYears);

	}

	@Data
	public static class AgeImplementation implements Serializable, AgeInterface {
		private static final long serialVersionUID = -2520784598151746890L;
		private int age;
	}

	public interface PersonInterface {

		String getPersonDescription();

	}

	@Data
	public static class CompositionState implements Serializable, PersonInterface {
		private static final long serialVersionUID = 3427985599976732264L;
		private final NameImplementation nameHolder = new NameImplementation();
		private final AgeImplementation ageHolder = new AgeImplementation();

		@Override
		public String getPersonDescription() {
			return nameHolder.getFullName() + " of age " + ageHolder.getAge();
		}
	}

}