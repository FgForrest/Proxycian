package one.edee.oss.proxycian.bytebuddy;

import one.edee.oss.proxycian.model.traits.GenericBucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ByteBuddyDiamondTest extends AbstractByteBuddyProxycianTest {

	@Test
	public void ByteBuddyProxyRecipeGenerator_DiamondProblemTest() {
		final Object instance = ByteBuddyProxyGenerator.instantiate(
			// create invocation handler delegating calls to "classifications" - ie atomic features of the proxy
			new ByteBuddyDispatcherInvocationHandler<>(
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
