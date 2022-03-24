package one.edee.oss.proxycian.trait.localDataStore;

import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.recipe.IntroductionAdvice;
import one.edee.oss.proxycian.util.ReflectionUtils;
import one.edee.oss.proxycian.utils.GenericsUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static one.edee.oss.proxycian.MethodClassification.noContext;

/**
 * Backs logic of {@link LocalDataStore}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2015
 */
public class LocalDataStoreAdvice implements IntroductionAdvice<LocalDataStoreProvider> {
	private static final long serialVersionUID = 8044624463306298114L;
	public static LocalDataStoreAdvice INSTANCE = new LocalDataStoreAdvice();

	private LocalDataStoreAdvice() {}

	@Override
	public List<Class<?>> getInterfacesToImplement() {
		return Collections.singletonList(
			LocalDataStore.class
		);
	}

	@Override
	public Class<LocalDataStoreProvider> getRequestedStateContract() {
		return LocalDataStoreProvider.class;
	}

	@Override
	public List<MethodClassification<?, LocalDataStoreProvider>> getMethodClassification() {
		return Arrays.asList(
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.getLocalData(String)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "getLocalData", String.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getLocalDataStoreIfPresent()).map(it -> it.get((String) args[0])).orElse(null)
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.getLocalData(Class)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "getLocalData", Class.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getLocalDataStoreIfPresent()).map(it -> it.get(computeName((Class<?>) args[0]))).orElse(null)
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.setLocalData(Serializable)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "setLocalData", Serializable.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					final Serializable value = (Serializable) args[0];
					// nothing is stored when value is null
					if (value != null) {
						final String valueName = computeName(value.getClass());
						proxyState.getOrCreateLocalDataStore().put(valueName, value);
					}
					return null;
				}
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.setLocalData(String,Serializable)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "setLocalData", String.class, Serializable.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					final String valueName = (String) args[0];
					final Serializable value = (Serializable) args[1];
					// nothing is stored when value is null
					if (value != null) {
						proxyState.getOrCreateLocalDataStore().put(valueName, value);
					}
					return null;
				}
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.computeLocalDataIfAbsent(Supplier)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "computeLocalDataIfAbsent", Supplier.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					try {
						final Supplier<?> valueSupplier = (Supplier<?>) args[0];
						final Method getter = valueSupplier.getClass().getMethod("get");
						final Class<?> methodReturnType = GenericsUtils.getMethodReturnType(valueSupplier.getClass(), getter);
						final String valueName = computeName(methodReturnType);
						final Map<String, Serializable> memoryStore = proxyState.getOrCreateLocalDataStore();
						Serializable value = memoryStore.get(valueName);
						if (value == null) {
							final Serializable newValue = (Serializable) valueSupplier.get();
							memoryStore.put(valueName, newValue);
							value = newValue;
						} else if (!methodReturnType.equals(value.getClass())) {
							throw new IllegalArgumentException(
								"Guessed type " + methodReturnType.getName() + " doesn't match cached type " + value.getClass().getName() + ". " +
									"If you're calling with lambda function proper type cannot be guessed from generics. Use computeLocalDataIfAbsent(String, Supplier) method instead!"
							);
						}
						return value;
					} catch (NoSuchMethodException e) {
						throw new InvocationTargetException(e);
					}
				}
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.computeLocalDataIfAbsent(String, Supplier)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "computeLocalDataIfAbsent", String.class, Supplier.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					final String valueName = (String) args[0];
					@SuppressWarnings("rawtypes") final Supplier<?> valueSupplier = (Supplier<?>) args[1];
					final Map<String, Serializable> memoryStore = proxyState.getOrCreateLocalDataStore();
					Serializable value = memoryStore.get(valueName);
					if (value == null) {
						final Serializable newValue = (Serializable) valueSupplier.get();
						memoryStore.put(valueName, newValue);
						value = newValue;
					}
					return value;
				}
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.removeLocalData(String)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "removeLocalData", String.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
					ofNullable(proxyState.getLocalDataStoreIfPresent()).map(it -> it.remove((String) args[0])).orElse(null)
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.removeLocalData(Class)",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "removeLocalData", Class.class),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
					ofNullable(proxyState.getLocalDataStoreIfPresent()).map(it -> it.remove(computeName((Class) args[0]))).orElse(null)
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.getLocalDataNames()",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "getLocalDataNames"),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
					ofNullable(proxyState.getLocalDataStoreIfPresent()).map(Map::keySet).orElse(null)
			),
			new PredicateMethodClassification<>(
				/* description */   "LocalDataStore.clearLocalData()",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "clearLocalData"),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					ofNullable(proxyState.getLocalDataStoreIfPresent()).ifPresent(Map::clear);
					return null;
				}
			)
		);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || !(obj == null || getClass() != obj.getClass());
	}

	private String computeName(Class<?> aClass) {
		return aClass.getSimpleName();
	}
}
