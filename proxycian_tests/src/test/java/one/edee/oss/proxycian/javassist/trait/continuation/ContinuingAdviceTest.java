package one.edee.oss.proxycian.javassist.trait.continuation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.TransparentPredicateMethodClassification;
import one.edee.oss.proxycian.bytebuddy.AbstractByteBuddyProxycianTest;
import one.edee.oss.proxycian.javassist.JavassistProxyGenerator;
import one.edee.oss.proxycian.model.traits.GenericBucket;
import one.edee.oss.proxycian.recipe.Advice;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStoreAdvice;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStoreProvider;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import static one.edee.oss.proxycian.MethodClassification.noContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies contract of advice continuation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ContinuingAdviceTest extends AbstractByteBuddyProxycianTest {

	@Test
	public void JavassistGenerator_shouldInvokeMultipleAdvices() {
		final GenericBucket stateBucket = new GenericBucket();
		final Object theInstance = JavassistProxyGenerator.instantiateSerializable(
			new ProxyRecipe(
				new Class[] {WorkerClass.class},
				new Advice[] {
					TimeMeasuringAdvice.INSTANCE,
					MemoizationAdvice.INSTANCE,
					LocalDataStoreAdvice.INSTANCE
				}
			),
			stateBucket
		);

		assertTrue(theInstance instanceof WorkerClass);
		final WorkerClass proxy = (WorkerClass) theInstance;

		final double result = proxy.computePi();
		assertEquals(3.1415936535907742d, result);

		final Long computationTime = (Long) stateBucket.get("_d5computePi");
		assertNotNull(computationTime);
		assertTrue(computationTime > 0L);

		final Double memoizedPi = (Double) stateBucket.get("_d5computePi_memoizedResult");
		assertEquals(3.1415936535907742d, memoizedPi);
	}

	public abstract static class WorkerClass {

		public double computePi() {
			double pi = 0;
			for (int i = 1; i < 1_000_000; i++) {
				pi += Math.pow(-1, i+1) / (2 * i - 1);
			}
			return 4 * pi;
		}

	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class TimeMeasuringAdvice implements Advice<LocalDataStoreProvider> {
		private static final long serialVersionUID = 2228087776061940393L;
		public static final TimeMeasuringAdvice INSTANCE = new TimeMeasuringAdvice();

		@Override
		public Class<LocalDataStoreProvider> getRequestedStateContract() {
			return LocalDataStoreProvider.class;
		}

		@Override
		public List<MethodClassification<?, LocalDataStoreProvider>> getMethodClassification() {
			return Collections.singletonList(
				new TransparentPredicateMethodClassification<>(
					/* description */   "All methods catch",
					/* matcher */       (method, proxyState) -> true,
					/* methodContext */ noContext(),
					/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					final long start = System.currentTimeMillis();
					try {
						return invokeSuper.call();
					} catch (InvocationTargetException e) {
						throw e;
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					} finally {
						proxyState.getOrCreateLocalDataStore().put(
							method.getName(), System.currentTimeMillis() - start
						);
					}
				}
				)
			);
		}
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class MemoizationAdvice implements Advice<LocalDataStoreProvider> {
		private static final long serialVersionUID = 2228087776061940393L;
		public static final MemoizationAdvice INSTANCE = new MemoizationAdvice();

		@Override
		public Class<LocalDataStoreProvider> getRequestedStateContract() {
			return LocalDataStoreProvider.class;
		}

		@Override
		public List<MethodClassification<?, LocalDataStoreProvider>> getMethodClassification() {
			return Collections.singletonList(
				new TransparentPredicateMethodClassification<>(
					/* description */   "All methods catch",
					/* matcher */       (method, proxyState) -> true,
					/* methodContext */ noContext(),
					/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					try {
						final Object result = invokeSuper.call();
						proxyState.getOrCreateLocalDataStore().put(
							method.getName() + "_memoizedResult", (Serializable) result
						);
						return result;
					} catch (InvocationTargetException e) {
						throw e;
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
				)
			);
		}
	}

}
