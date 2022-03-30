package one.edee.oss.proxycian.javassist;

import lombok.Data;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyClassWithInnerStateTest.ParentWithCustomParametrizedConstructor;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyClassWithInnerStateTest.SpecializedImplementation;
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

public class JavassistClassWithInnerStateTest extends AbstractJavassistProxycianTest {

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_Created() {
		final ParentWithParametrizedConstructor theInstance = JavassistProxyGenerator.instantiate(
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
	public void JavassistProxyGenerator_Proxy_CreatedWithSpecializedConstructorType() {
		final SpecializedImplementation theImplementation = new SpecializedImplementation();
		final ParentWithCustomParametrizedConstructor theInstance = JavassistProxyGenerator.instantiate(
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
	public void JavassistProxyGenerator_Proxy_SerializedAndDeserialized() throws IOException, ClassNotFoundException {
		final Class<?>[] constructorTypes = {String.class, int.class};
		final Object[] constructorArgs = {"Me, myself and I", 42};
		final ParentWithParametrizedConstructor theInstance = JavassistProxyGenerator.instantiateSerializable(
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
	public void JavassistProxyGenerator_Proxy_CreatedWithOnInstantiationCallback() {
		final ParentWithParametrizedConstructor theInstance = JavassistProxyGenerator.instantiate(
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
		private static final long serialVersionUID = -31195579519063303L;
		private final String stringField;
		private final int intField;
		private boolean initialized;

	}

}
