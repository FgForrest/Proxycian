package one.edee.oss.proxycian.javassist;

import one.edee.oss.proxycian.model.composite.CustomizedPerson;
import one.edee.oss.proxycian.model.traits.GenericBucketProxyGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavassistSerializationTest extends AbstractJavassistProxycianTest {

	@Test
	public void JavassistGenerator_ProxySerialization_DeserializedCopyEqualsOriginal() throws Exception {
		final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");

        final ByteArrayOutputStream serializedProxy = new ByteArrayOutputStream();
        try (ObjectOutputStream serializationStream = new ObjectOutputStream(serializedProxy)) {
            serializationStream.writeObject(person);
        }

        final CustomizedPerson deserializedPerson;
        final ByteArrayInputStream proxyForDeserialization = new ByteArrayInputStream(serializedProxy.toByteArray());
        try (ObjectInputStream deserializationStream = new ObjectInputStream(proxyForDeserialization)) {
            deserializedPerson = (CustomizedPerson) deserializationStream.readObject();
        }

        assertEquals(person, deserializedPerson);
    }

	private static CustomizedPerson createTestPersonProxy(String firstName, String lastName) {
		final CustomizedPerson person = GenericBucketProxyGenerator.instantiateJavassistProxy(CustomizedPerson.class);
		person.setFirstName(firstName);
		person.setLastName(lastName);
		person.setBirthDate(LocalDate.of(1978, 5, 5));
		return person;
	}

}