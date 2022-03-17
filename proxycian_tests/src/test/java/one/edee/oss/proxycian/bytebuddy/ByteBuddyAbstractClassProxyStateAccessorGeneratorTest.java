package one.edee.oss.proxycian.bytebuddy;

import one.edee.oss.proxycian.model.composite.CustomizedPerson;
import one.edee.oss.proxycian.model.composite.CustomizedPersonAbstract;
import one.edee.oss.proxycian.model.traits.GenericBucketProxyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ByteBuddyAbstractClassProxyStateAccessorGeneratorTest {

	@Test
	public void ByteBuddyGenerator_ProxyAbstract_Created() {
		CustomizedPerson person = GenericBucketProxyGenerator.instantiateByteBuddyProxy(CustomizedPersonAbstract.class);
		assertNotNull(person);
	}

	@Test
	public void ByteBuddyGenerator_ProxyAbstract_InvokesRealMethodUsingAbstractOnes() {
		final CustomizedPersonAbstract person = createTestPersonProxy("Jan", "Novotný");
		assertEquals("Jan Novotný", person.getCompleteName());
	}

	private static CustomizedPersonAbstract createTestPersonProxy(String firstName, String lastName) {
		final CustomizedPersonAbstract person = GenericBucketProxyGenerator.instantiateByteBuddyProxy(CustomizedPersonAbstract.class);
		person.setFirstName(firstName);
		person.setLastName(lastName);
		return person;
	}

}