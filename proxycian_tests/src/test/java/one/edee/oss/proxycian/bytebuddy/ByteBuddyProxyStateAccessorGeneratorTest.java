package one.edee.oss.proxycian.bytebuddy;

import one.edee.oss.proxycian.model.composite.CustomizedPerson;
import one.edee.oss.proxycian.model.traits.GenericBucketProxyGenerator;
import one.edee.oss.proxycian.utils.ClockAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ByteBuddyProxyStateAccessorGeneratorTest extends AbstractByteBuddyProxycianTest {

	@BeforeEach
	void setUp() {
		ClockAccessor.getInstance().setCurrentClock(Clock.fixed(LocalDate.of(2017, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
	}

	@AfterEach
	public void tearDown() {
		ClockAccessor.getInstance().setSystemDateTimeClock();
	}

	@Test
	public void ByteBuddyProxyGenerator_Proxy_Created() {
		CustomizedPerson person = GenericBucketProxyGenerator.instantiateByteBuddyProxy(CustomizedPerson.class);
		assertNotNull(person);
	}

	@Test
	public void ByteBuddyProxyGenerator_Proxy_GetPropertyReturnsSetValue() {
        final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");

		assertEquals("Jan", person.getFirstName());
		assertEquals("Novotný", person.getLastName());
		assertEquals(LocalDate.of(1978, 5, 5), person.getBirthDate());
	}

    @Test
	public void ByteBuddyProxyGenerator_Proxy_DefaultMethodComputesAge() {
        final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");

		assertEquals(38, person.getAge());
	}

	@Test
	public void ByteBuddyProxyGenerator_Proxy_GetPropertiesReturnsPopulatedMap() {
        final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");

		final Map<String, Object> props = person.getProperties();
		assertEquals(3, props.size());
		assertTrue(props.containsKey("firstName"));
		assertTrue(props.containsKey("lastName"));
		assertTrue(props.containsKey("birthDate"));
	}

	@Test
	public void ByteBuddyProxyGenerator_Proxy_PropertiesCanBeSetIntoMap() {
		final CustomizedPerson person = GenericBucketProxyGenerator.instantiateByteBuddyProxy(CustomizedPerson.class);
		final Map<String, Object> props = person.getProperties();
		props.put("firstName", "Jan");
		props.put("lastName", "Novotný");
		props.put("birthDate", LocalDate.of(1978, 5, 5));

		assertEquals("Jan", person.getFirstName());
		assertEquals("Novotný", person.getLastName());
		assertEquals(LocalDate.of(1978, 5, 5), person.getBirthDate());
	}

	@Test
	public void ByteBuddyProxyGenerator_Proxy_ToStringReturnsContentsOfTheMap() {
        final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");

		assertEquals("{firstName=Jan, lastName=Novotný, birthDate=1978-05-05}", person.toString());
	}

    @Test
    public void ByteBuddyProxyGenerator_Proxy_HashCodeContractRespected() {
        final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");
        final CustomizedPerson samePerson = createTestPersonProxy("Jan", "Novotný");

        assertNotSame(person, samePerson);
        assertEquals(person.hashCode(), samePerson.hashCode());
    }

    @Test
    public void ByteBuddyProxyGenerator_Proxy_EqualsContractRespected() {
        final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");
        final CustomizedPerson samePerson = createTestPersonProxy("Jan", "Novotný");
        final CustomizedPerson differentPerson = createTestPersonProxy("Petr", "Novák");

        assertNotSame(person, samePerson);
        assertEquals(person, samePerson);
        assertNotEquals(person, differentPerson);
    }

	@Test
	public void ByteBuddyProxyGenerator_Proxy_NonhandledMethodThrowsException() {
		assertThrows(UnsupportedOperationException.class, () -> {
			final CustomizedPerson person = createTestPersonProxy("Jan", "Novotný");
			person.doWork();
		});
	}

    private static CustomizedPerson createTestPersonProxy(String firstName, String lastName) {
        final CustomizedPerson person = GenericBucketProxyGenerator.instantiateByteBuddyProxy(CustomizedPerson.class);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setBirthDate(LocalDate.of(1978, 5, 5));
        return person;
    }

}
