package one.edee.oss.proxycian.bytebuddy;

import one.edee.oss.proxycian.ByteBuddyProxyGenerator;
import org.junit.jupiter.api.BeforeEach;

/**
 * This test parent flushes the implementation cache.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public abstract class AbstractByteBuddyProxycianTest {

	@BeforeEach
	void setUp() {
		ByteBuddyProxyGenerator.clearClassCache();
		ByteBuddyProxyGenerator.clearMethodClassificationCache();
	}

}
