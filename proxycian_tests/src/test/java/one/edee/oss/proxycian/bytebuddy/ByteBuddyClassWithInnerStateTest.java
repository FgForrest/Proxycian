package one.edee.oss.proxycian.bytebuddy;

import lombok.Data;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import one.edee.oss.proxycian.recipe.Advice;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStore;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStoreAdvice;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ByteBuddyClassWithInnerStateTest extends AbstractByteBuddyProxycianTest {

	@Test
	public void ByteBuddyProxyRecipeGenerator_Proxy_Created() {
		final ParentWithParametrizedConstructor theInstance = ByteBuddyProxyGenerator.instantiate(
			new ProxyRecipe(
				new Class[] { ParentWithParametrizedConstructor.class },
				new Advice[] { LocalDataStoreAdvice.INSTANCE }
			),
			new GenericBucket(),
			new Class[] { String.class, int.class },
			new Object[] { "Me, myself and I", 42 }
		);

		assertTrue(theInstance instanceof LocalDataStore);
		assertEquals("Me, myself and I", theInstance.getStringField());
		assertEquals(42, theInstance.getIntField());
	}

	@Test
	public void ByteBuddyProxyRecipeGenerator_Proxy_CreatedWithSpecializedConstructorType() {
		final SpecializedImplementation theImplementation = new SpecializedImplementation();
		final ParentWithCustomParametrizedConstructor theInstance = ByteBuddyProxyGenerator.instantiate(
			new ProxyRecipe(
				new Class[] { ParentWithCustomParametrizedConstructor.class },
				new Advice[] { LocalDataStoreAdvice.INSTANCE }
			),
			new GenericBucket(),
			new Class[] { SpecializedImplementation.class },
			new Object[] {theImplementation}
		);

		assertTrue(theInstance instanceof LocalDataStore);
		assertSame(theImplementation, theInstance.getSomeField());
	}

	@Test
	public void ByteBuddyProxyRecipeGenerator_Proxy_SerializedAndDeserialized() throws IOException, ClassNotFoundException {
		final Class<?>[] constructorTypes = {String.class, int.class};
		final Object[] constructorArgs = {"Me, myself and I", 42};
		final ParentWithParametrizedConstructor theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				new Class[] { ParentWithParametrizedConstructor.class },
				new Advice[] { LocalDataStoreAdvice.INSTANCE }
			),
			new GenericBucket(constructorTypes, constructorArgs),
			constructorTypes, constructorArgs
		);

		theInstance.setInitialized(true);

		final byte[] serializedObject;
		try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024); final ObjectOutputStream os = new ObjectOutputStream(bos)) {
			os.writeObject(theInstance);
			os.flush();
			serializedObject = bos.toByteArray();
		}

		final ParentWithParametrizedConstructor deserializedInstance;
		try (final ByteArrayInputStream bis = new ByteArrayInputStream(serializedObject); final ObjectInputStream is = new ObjectInputStream(bis)) {
			deserializedInstance = (ParentWithParametrizedConstructor) is.readObject();
		}

		assertTrue(deserializedInstance instanceof LocalDataStore);
		assertEquals("Me, myself and I", deserializedInstance.getStringField());
		assertEquals(42, deserializedInstance.getIntField());
		assertTrue(deserializedInstance.isInitialized());
	}

	@Test
	public void ByteBuddyProxyRecipeGenerator_Proxy_CreatedWithOnInstantiationCallback() {
		final ParentWithParametrizedConstructor theInstance = ByteBuddyProxyGenerator.instantiate(
			new ProxyRecipe(
				new Class[]{ParentWithParametrizedConstructor.class},
				new Advice[]{LocalDataStoreAdvice.INSTANCE},
				(proxy, proxyState) -> {
					assertNotNull(proxyState);
					((ParentWithParametrizedConstructor)proxy).setInitialized(true);
				}
			),
			new GenericBucket(),
			new Class[] { String.class, int.class },
			new Object[] { "Me, myself and I", 42 }
		);

		assertTrue(theInstance instanceof LocalDataStore);
		assertEquals("Me, myself and I", theInstance.getStringField());
		assertEquals(42, theInstance.getIntField());
		assertTrue(theInstance.isInitialized());
	}

	@Data
	public static class ParentWithParametrizedConstructor implements Serializable {
		private static final long serialVersionUID = 2613107154512824175L;
		private final String stringField;
		private final int intField;
		private boolean initialized;

	}

	@Data
	public static class ParentWithCustomParametrizedConstructor implements Serializable {
		private static final long serialVersionUID = -26457059608558160L;
		private final SpecializedInterface someField;

	}

	public interface SpecializedInterface {

	}

	public static class SpecializedImplementation implements SpecializedInterface {

	}

}
