package one.edee.oss.proxycian.javassist;

import lombok.Data;
import one.edee.oss.proxycian.JavassistProxyGenerator;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import one.edee.oss.proxycian.recipe.Advice;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStore;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStoreAdvice;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavassistProxyRecipeTest extends AbstractJavassistProxycianTest {

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_Created() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				LocalDataStoreAdvice.INSTANCE
			),
			new GenericBucket()
		);

		assertTrue(theInstance instanceof LocalDataStore);
	}

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_CreatedWithOnInstantiationCallback() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				BaseClass.class, new Class[0],
				new Advice[]{LocalDataStoreAdvice.INSTANCE},
				(proxy, proxyState) -> {
					assertNotNull(proxyState);
					((BaseClass)proxy).setInitialized(true);
				}
			),
			new GenericBucket()
		);

		assertTrue(theInstance instanceof LocalDataStore);
		assertTrue(((BaseClass)theInstance).isInitialized());
	}

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_GetSetContractWorks() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				LocalDataStoreAdvice.INSTANCE
			),
			new GenericBucket()
		);

		final LocalDataStore lds = (LocalDataStore) theInstance;
		lds.setLocalData("a", 1);
		lds.setLocalData(new BigDecimal("56.0"));
		assertEquals(Integer.valueOf(1), lds.getLocalData("a"));
		assertEquals(new BigDecimal("56.0"), lds.getLocalData(BigDecimal.class));
	}

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_GetSetSupplierContractWorks() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				LocalDataStoreAdvice.INSTANCE
			),
			new GenericBucket()
		);

		final LocalDataStore lds = (LocalDataStore) theInstance;
		lds.computeLocalDataIfAbsent("a", () -> 1);
		// we need this anonymous class to pass generics data that are not available for lambda methods
		lds.computeLocalDataIfAbsent(new Supplier<BigDecimal>() {
			@Override
			public BigDecimal get() {
				return new BigDecimal("56.0");
			}
		});
		assertEquals(Integer.valueOf(1), lds.getLocalData("a"));
		assertEquals(new BigDecimal("56.0"), lds.getLocalData(BigDecimal.class));
	}

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_RemoveContractWorks() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				LocalDataStoreAdvice.INSTANCE
			),
			new GenericBucket()
		);

		final LocalDataStore lds = (LocalDataStore) theInstance;
		lds.setLocalData("a", 1);
		lds.setLocalData(new BigDecimal("56.0"));

		assertEquals(Integer.valueOf(1), lds.getLocalData("a"));
		assertEquals(new BigDecimal("56.0"), lds.getLocalData(BigDecimal.class));

		lds.removeLocalData(BigDecimal.class);
		assertNull(lds.getLocalData(BigDecimal.class));
		assertNotNull(lds.getLocalData("a"));

		lds.removeLocalData("a");
		assertNull(lds.getLocalData("a"));
	}

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_ClearAllContractWorks() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				LocalDataStoreAdvice.INSTANCE
			),
			new GenericBucket()
		);

		final LocalDataStore lds = (LocalDataStore) theInstance;
		lds.setLocalData("a", 1);
		lds.setLocalData(new BigDecimal("56.0"));

		assertEquals(Integer.valueOf(1), lds.getLocalData("a"));
		assertEquals(new BigDecimal("56.0"), lds.getLocalData(BigDecimal.class));

		lds.clearLocalData();
		assertNull(lds.getLocalData(BigDecimal.class));
		assertNull(lds.getLocalData("a"));
	}

	@Test
	public void JavassistProxyRecipeGenerator_Proxy_GetLocalNamesContractWorks() {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				LocalDataStoreAdvice.INSTANCE
			),
			new GenericBucket()
		);

		final LocalDataStore lds = (LocalDataStore) theInstance;
		lds.setLocalData("a", 1);
		lds.setLocalData(new BigDecimal("56.0"));

		final Collection<String> localDataNames = lds.getLocalDataNames();
		assertEquals(2, localDataNames.size());
		final Iterator<String> it = localDataNames.iterator();
		assertEquals("a", it.next());
		assertEquals("BigDecimal", it.next());
	}

	@Test
	public void JavassistProxyRecipeGenerator_ProxySerialization_DeserializedCopyEqualsOriginal() throws Exception {
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				LocalDataStoreAdvice.INSTANCE
			),
			new GenericBucket()
		);

		final LocalDataStore lds = (LocalDataStore) theInstance;
		lds.setLocalData("a", 1);
		lds.setLocalData(new BigDecimal("56.0"));

		final ByteArrayOutputStream serializedProxy = new ByteArrayOutputStream();
		try (ObjectOutputStream serializationStream = new ObjectOutputStream(serializedProxy)) {
			serializationStream.writeObject(theInstance);
		}

		final LocalDataStore deserializedLds;
		final ByteArrayInputStream proxyForDeserialization = new ByteArrayInputStream(serializedProxy.toByteArray());
		try (ObjectInputStream deserializationStream = new ObjectInputStream(proxyForDeserialization)) {
			deserializedLds = (LocalDataStore) deserializationStream.readObject();
		}

		assertEquals(lds, deserializedLds);
		assertEquals(Integer.valueOf(1), lds.getLocalData("a"));
		assertEquals(new BigDecimal("56.0"), lds.getLocalData(BigDecimal.class));
	}

	@Data
	public static abstract class BaseClass {
		private boolean initialized;
	}

}
