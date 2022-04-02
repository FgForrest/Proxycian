package one.edee.oss.proxycian.javassist;

import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test verifying {@link JavassistProxyGenerator} for diamond interface composition with default methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class JavassistMethodAnnotationTest extends AbstractJavassistProxycianTest {

	@Test
	public void JavassistProxyRecipeGenerator_MethodAnnotationTest() throws NoSuchMethodException {
		final Object instance = JavassistProxyGenerator.instantiate(
			// create invocation handler delegating calls to "classifications" - ie atomic features of the proxy
			new JavassistDispatcherInvocationHandler<>(
				// proxy state
				new GenericBucket(),
				new PredicateMethodClassification<>(
					"Catch all",
					(method, proxyState) -> true,
					MethodClassification.noContext(),
					(proxy, method, args, methodContext, proxyState, invokeSuper) -> {
						assertTrue(method.isAnnotationPresent(CustomMethodAnnotation.class));
						try {
							return invokeSuper.call();
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						}
					}
				)
			),
			// interfaces to implement
			TestClass.class
		);
		assertEquals("whatever", ((TestClass)instance).getCode());
	}

	public abstract static class TestClass {

		@CustomMethodAnnotation
		public String getCode() {
			return "whatever";
		}

	}

	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	public @interface CustomMethodAnnotation {
	}

}
