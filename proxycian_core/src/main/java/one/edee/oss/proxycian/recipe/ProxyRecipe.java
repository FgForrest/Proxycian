package one.edee.oss.proxycian.recipe;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.OnInstantiationCallback;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.utils.ArrayUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import static java.util.Optional.ofNullable;

/**
 * Proxy recipe is a complete descriptor for the proxy class. It contains list of all interfaces the proxy class should
 * implement (or from which class it should extend) and also contains set of advices that fill in the logic that should
 * handle all logic connected with those interfaces.
 *
 * It's recommended to cache proxy recipes and keep only minimal amount of them.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode
@ToString
public class ProxyRecipe implements Serializable {
	private static final long serialVersionUID = -4034411993592819942L;
	private static final Class<?>[] EMPTY_CLASSES = new Class[0];

	@Getter private final OnInstantiationCallback instantiationCallback;
	@Getter private final Class<?>[] interfaces;
	@Getter private final Advice<?>[] advices;
	private transient MethodClassification<?,?>[] methodClassifications;
	private final Set<Class<?>> verifiedStateClasses = ConcurrentHashMap.newKeySet();

	/**
	 * Combines abstract class with interfaces together.
	 */
	private static Class<?>[] combine(Class<?> aClass, Class<?>[] interfaces) {
		final Class<?>[] combinedClasses = new Class[interfaces.length + 1];
		combinedClasses[0] = aClass;
		System.arraycopy(interfaces, 0, combinedClasses, 1, interfaces.length);
		return combinedClasses;
	}

	/**
	 * Creates new proxy recipe with passed set of advices. Resulting proxy implements only those interfaces that are
	 * derived from {@link IntroductionAdvice}.
	 */
	public ProxyRecipe(Advice<?>... advices) {
		this(EMPTY_CLASSES, advices);
	}

	/**
	 * Creates new proxy recipe with passed set of advices. Resulting proxy implements only those interfaces that are
	 * derived from {@link IntroductionAdvice}.
	 */
	public ProxyRecipe(Advice<?>[] advices, OnInstantiationCallback instantiationCallback) {
		this(EMPTY_CLASSES, advices, instantiationCallback);
	}

	/**
	 * Creates new proxy recipe that combined passed interfaces with interfaces introduced by {@link IntroductionAdvice}
	 * and registers all advices that fullfill the logic of the proxy.
	 */
	public ProxyRecipe(Class<?> abstractClass, Class<?>[] interfaces, Advice<?>[] advices) {
		this(combine(abstractClass, interfaces), advices);
	}

	/**
	 * Creates new proxy recipe that combined passed interfaces with interfaces introduced by {@link IntroductionAdvice}
	 * and registers all advices that fullfill the logic of the proxy.
	 */
	public ProxyRecipe(Class<?> abstractClass, Class<?>[] interfaces, Advice<?>[] advices, OnInstantiationCallback instantiationCallback) {
		this(combine(abstractClass, interfaces), advices, instantiationCallback);
	}

	/**
	 * Creates new proxy recipe that combined passed interfaces with interfaces introduced by {@link IntroductionAdvice}
	 * and registers all advices that fullfill the logic of the proxy.
	 */
	public ProxyRecipe(Class<?>[] interfaces, Advice<?>[] advices) {
		this(interfaces, advices, null);
	}

	/**
	 * Creates new proxy recipe that combined passed interfaces with interfaces introduced by {@link IntroductionAdvice}
	 * and registers all advices that fullfill the logic of the proxy.
	 */
	public ProxyRecipe(Class<?>[] interfaces, Advice<?>[] advices, OnInstantiationCallback instantiationCallback) {
		this.instantiationCallback = instantiationCallback;
		this.advices = advices;
		List<Class<?>> additionalInterfaces = null;
		for (Advice<?> advice : advices) {
			if (advice instanceof IntroductionAdvice) {
				if (additionalInterfaces == null) {
					additionalInterfaces = new LinkedList<>();
				}
				final List<Class<?>> interfacesToImplement = ((IntroductionAdvice) advice).getInterfacesToImplement();
				for (Class<?> interfaceToImplement : interfacesToImplement) {
					if (!additionalInterfaces.contains(interfaceToImplement)) {
						additionalInterfaces.add(interfaceToImplement);
					}
				}
			}
		}
		this.interfaces = additionalInterfaces == null ?
			interfaces :
			ArrayUtils.mergeArrays(interfaces, additionalInterfaces.toArray(EMPTY_CLASSES));
	}

	/**
	 * Method verifies whether proxy state is compliant with all advices used in this recipe.
	 */
	public void verifyProxyState(Object proxyState) {
		if (!verifiedStateClasses.contains(proxyState.getClass())) {
			for (Advice<?> advice : advices) {
				ofNullable(advice.getRequestedStateContract()).ifPresent(requestedInterface -> {
					final BiPredicate<Object, Class<?>> compatibilityChecker = advice instanceof SelfVerifiableState ?
						(pState, reqIface) -> ((SelfVerifiableState) advice).verifyCompatibility(proxyState, requestedInterface) :
						(pState, reqIface) -> requestedInterface.isInstance(pState);
					if (!compatibilityChecker.test(proxyState, requestedInterface)) {
						throw new IllegalArgumentException(
							"Proxy state " + proxyState.getClass() + " doesn't implement " + requestedInterface + "!"
						);
					}
				});
			}
			verifiedStateClasses.add(proxyState.getClass());
		}
	}

	/**
	 * Returns complete list of interfaces defined by this recipe and adds all interfaces from the parameters of the method.
	 */
	public Class<?>[] getInterfacesWith(Class<?>... baseClasses) {
		if (interfaces[0].isInterface()) {
			return ArrayUtils.mergeArrays(baseClasses, interfaces);
		} else {
			final Class<?>[] result = new Class[baseClasses.length + interfaces.length];
			result[0] = interfaces[0];
			System.arraycopy(baseClasses, 0, result, 1, baseClasses.length);
			System.arraycopy(interfaces, 1, result, 1 + baseClasses.length, interfaces.length - 1);
			return result;
		}
	}

	/**
	 * Returns complete list of method classifications defined by this recipe and adds all classifications from the parameters
	 * of the method.
	 */
	public MethodClassification<?, ?>[] getMethodClassificationsWith(PredicateMethodClassification<?, ?, ?>... baseMethodClassifications) {
		final MethodClassification<?, ?>[] methodClassifications = getMethodClassifications();
		final MethodClassification<?,?>[] combinedResult = new MethodClassification<?,?>[methodClassifications.length + baseMethodClassifications.length];
		System.arraycopy(methodClassifications, 0, combinedResult, 0, methodClassifications.length);
		System.arraycopy(baseMethodClassifications, 0, combinedResult, methodClassifications.length, baseMethodClassifications.length);
		return combinedResult;
	}

	/*
		PRIVATE METHODS
	 */

	private MethodClassification<?, ?>[] getMethodClassifications() {
		if (this.methodClassifications == null) {
			this.methodClassifications = Arrays.stream(this.advices)
				.flatMap(it -> it.getMethodClassification().stream())
				.toArray(MethodClassification[]::new);
		}
		return this.methodClassifications;
	}
}
