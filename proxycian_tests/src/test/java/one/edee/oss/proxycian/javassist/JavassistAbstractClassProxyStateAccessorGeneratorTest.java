package one.edee.oss.proxycian.javassist;

import one.edee.oss.proxycian.model.composite.CustomizedPerson;
import one.edee.oss.proxycian.model.composite.CustomizedPersonAbstract;
import one.edee.oss.proxycian.model.traits.GenericBucketProxyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JavassistAbstractClassProxyStateAccessorGeneratorTest extends AbstractJavassistProxycianTest {

	@Test
	public void JavassistGenerator_ProxyAbstract_Created() {
		CustomizedPerson person = GenericBucketProxyGenerator.instantiateJavassistProxy(CustomizedPersonAbstract.class);
		assertNotNull(person);
	}

	@Test
	public void JavassistGenerator_ProxyAbstract_InvokesRealMethodUsingAbstractOnes() {
		final CustomizedPersonAbstract person = createTestPersonProxy("Jan", "Novotný");
		assertEquals("Jan Novotný", person.getCompleteName());
	}

	private static CustomizedPersonAbstract createTestPersonProxy(String firstName, String lastName) {
		final CustomizedPersonAbstract person = GenericBucketProxyGenerator.instantiateJavassistProxy(CustomizedPersonAbstract.class);
		person.setFirstName(firstName);
		person.setLastName(lastName);
		return person;
	}

}