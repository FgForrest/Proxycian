package one.edee.oss.proxycian.javassist;

import org.junit.jupiter.api.BeforeEach;

/**
 * This test parent flushes the implementation cache.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public abstract class AbstractJavassistProxycianTest {

	@BeforeEach
	void setUp() {
		JavassistProxyGenerator.clearClassCache();
		JavassistProxyGenerator.clearMethodClassificationCache();
	}

}
