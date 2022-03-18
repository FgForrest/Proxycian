package one.edee.oss.proxycian.bytebuddy.trait.beanMemoryStore;

import lombok.Data;
import one.edee.oss.proxycian.bytebuddy.AbstractByteBuddyProxycianTest;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyProxyGenerator;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import one.edee.oss.proxycian.recipe.Advice;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.beanMemoryStore.BeanMemoryStoreAdvice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies contract of {@link BeanMemoryStoreAdvice}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class BeanMemoryStoreAdviceTest extends AbstractByteBuddyProxycianTest {

	@Test
	public void shouldProxyJavaBeanInterface() {
		final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				new Class[] {SomeJavaBeanIface.class},
				new Advice[] {BeanMemoryStoreAdvice.ABSTRACT_METHOD_INSTANCE}
			),
			new GenericBucket()
		);

		assertTrue(theInstance instanceof SomeJavaBeanIface);
		final SomeJavaBeanIface proxy = (SomeJavaBeanIface) theInstance;

		proxy.setAge((byte) 43);
		proxy.setHeight((short) 183);
		proxy.setWeight(82);
		proxy.setIq(143); // :)
		proxy.setName("Jan Novotný");
		assertEquals((byte) 43, proxy.getAge());
		assertEquals((short) 183, proxy.getHeight());
		assertEquals(82, proxy.getWeight());
		assertEquals(143L, proxy.getIq());

		assertEquals("Jan Novotný (43)", proxy.print());
	}

	@Test
	public void shouldCloneContractWork() {
		final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				new Class[] {SomeJavaBeanIface.class},
				new Advice[] {BeanMemoryStoreAdvice.ABSTRACT_METHOD_INSTANCE}
			),
			new GenericBucket()
		);

		final SomeJavaBeanIface proxy = (SomeJavaBeanIface) theInstance;

		proxy.setAge((byte) 43);
		proxy.setHeight((short) 183);
		proxy.setWeight(82);
		proxy.setIq(143); // :)
		proxy.setName("Jan Novotný");

		final SomeJavaBeanIface clonedProxy = (SomeJavaBeanIface) proxy.clone();

		assertNotSame(proxy, clonedProxy);
		assertNotSame(((ProxyStateAccessor)proxy).getProxyState(), ((ProxyStateAccessor)clonedProxy).getProxyState());
		assertEquals((byte) 43, clonedProxy.getAge());
		assertEquals((short) 183, clonedProxy.getHeight());
		assertEquals(82, clonedProxy.getWeight());
		assertEquals(143L, clonedProxy.getIq());

		assertEquals("Jan Novotný (43)", clonedProxy.print());
	}

	@Test
	public void shouldProxyJavaBeanAbstractClass() {
		final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				new Class[] {SomeJavaBean.class},
				new Advice[] {BeanMemoryStoreAdvice.ALL_METHOD_INSTANCE}
			),
			new GenericBucket()
		);

		assertTrue(theInstance instanceof SomeJavaBeanIface);
		final SomeJavaBeanIface proxy = (SomeJavaBeanIface) theInstance;

		proxy.setAge((byte) 43);
		proxy.setHeight((short) 183);
		proxy.setWeight(82);
		proxy.setIq(143); // :)
		proxy.setName("Jan Novotný");
		assertEquals((byte) 43, proxy.getAge());
		assertEquals((short) 183, proxy.getHeight());
		assertEquals(82, proxy.getWeight());
		assertEquals(143L, proxy.getIq());
		assertEquals("Jan Novotný (43)", proxy.print());
	}

	@Test
	public void shouldProxyJavaBeanWithMultipleItems() {
		final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				new Class[] {JavaBeanWithMultipleItems.class},
				new Advice[] {BeanMemoryStoreAdvice.ALL_METHOD_INSTANCE}
			),
			new GenericBucket()
		);

		assertTrue(theInstance instanceof JavaBeanWithMultipleItems);
		final JavaBeanWithMultipleItems proxy = (JavaBeanWithMultipleItems) theInstance;

		proxy.addItem("A");
		proxy.addItem("B");
		proxy.addItem("C");

		assertArrayEquals(new String[] {"A", "B", "C"}, proxy.getItems().toArray(new String[0]));

		proxy.removeItem("B");

		assertArrayEquals(new String[] {"A", "C"}, proxy.getItems().toArray(new String[0]));
	}

	public interface SomeJavaBeanIface extends Cloneable {

		boolean isLiving();
		void setLiving(boolean living);

		byte getAge();
		void setAge(byte age);

		short getHeight();
		void setHeight(short height);

		int getWeight();
		void setWeight(int weight);

		long getIq();
		void setIq(long iq);

		String getName();
		void setName(String name);

		Object clone();

		default String print() {
			return getName() + " (" + getAge() + ")";
		}

	}

	@Data
	public static class SomeJavaBean implements SomeJavaBeanIface {
		private boolean living;
		private byte age;
		private short height;
		private int weight;
		private long iq;
		private String name;

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public interface JavaBeanWithMultipleItems {

		List<String> getItems();

		void setItems(List<String> items);

		void addItem(String item);

		void removeItem(String item);

	}

}
