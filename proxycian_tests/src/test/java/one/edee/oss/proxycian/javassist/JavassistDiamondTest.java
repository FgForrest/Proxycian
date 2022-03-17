package one.edee.oss.proxycian.javassist;

import one.edee.oss.proxycian.JavassistDispatcherInvocationHandler;
import one.edee.oss.proxycian.JavassistProxyGenerator;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavassistDiamondTest extends AbstractJavassistProxycianTest {

	@Test
	public void ByteBuddyProxyRecipeGenerator_DiamondProblemTest() {
		final Object instance = JavassistProxyGenerator.instantiate(
			// create invocation handler delegating calls to "classifications" - ie atomic features of the proxy
			new JavassistDispatcherInvocationHandler<>(
				// proxy state
				new GenericBucket()
			),
			// interfaces to implement
			Trait1.class, Trait2.class
		);
		assertEquals("whatever", ((SomeSharedInterface)instance).getCode());
	}

	public interface Trait1 extends SomeSharedInterface {

	}

	public interface Trait2 extends SomeSharedInterface {

	}

	public interface SomeSharedInterface {

		default String getCode() {
			return "whatever";
		}

	}

}
