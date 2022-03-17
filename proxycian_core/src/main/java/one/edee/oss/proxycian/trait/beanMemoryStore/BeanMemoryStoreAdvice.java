package one.edee.oss.proxycian.trait.beanMemoryStore;

import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.recipe.Advice;
import one.edee.oss.proxycian.trait.localDataStore.LocalDataStore;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;

/**
 * This advice captures all getter/setter calls to the proxy {@link LocalDataStore} as simple key/values in a map object.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class BeanMemoryStoreAdvice implements Advice<BeanMemoryStore> {
	private static final long serialVersionUID = 4100044042153442374L;
	/**
	 * This instance matches all Java bean methods regardless of being implemented or not.
	 */
	public static final BeanMemoryStoreAdvice ALL_METHOD_INSTANCE = new BeanMemoryStoreAdvice(method -> true);
	/**
	 * This instance matches only abstract - non implemented Java bean methods.
	 */
	public static final BeanMemoryStoreAdvice ABSTRACT_METHOD_INSTANCE = new BeanMemoryStoreAdvice(method -> Modifier.isAbstract(method.getModifiers()));
	private static final String SET = "set";
	private static final String GET = "get";
	private static final String IS = "is";
	private static final String ADD = "add";
	private static final String REMOVE = "remove";
	private final Predicate<Method> basePredicate;

	private BeanMemoryStoreAdvice(Predicate<Method> basePredicate) {
		this.basePredicate = basePredicate;
	}

	@Override
	public Class<BeanMemoryStore> getRequestedStateContract() {
		return BeanMemoryStore.class;
	}

	@Override
	public List<MethodClassification<?, BeanMemoryStore>> getMethodClassification() {
		return Arrays.asList(
			new PredicateMethodClassification<>(
				/* description */   "JavaBean setter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(SET) && void.class.equals(method.getReturnType()) && method.getParameterCount() == 1,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(SET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					proxyState.putValueToMemoryStore(methodContext, (Serializable) args[0]);
					return null;
				}
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean short getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(GET) && short.class.equals(method.getReturnType()) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(GET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getValueFromMemoryStore(methodContext)).orElse((short) 0)
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean byte getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(GET) && byte.class.equals(method.getReturnType()) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(GET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getValueFromMemoryStore(methodContext)).orElse((byte) 0)
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean int getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(GET) && int.class.equals(method.getReturnType()) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(GET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getValueFromMemoryStore(methodContext)).orElse(0)
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean long getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(GET) && long.class.equals(method.getReturnType()) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(GET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getValueFromMemoryStore(methodContext)).orElse((long) 0)
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean float getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(GET) && float.class.equals(method.getReturnType()) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(GET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getValueFromMemoryStore(methodContext)).orElse((float) 0)
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean double getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(GET) && double.class.equals(method.getReturnType()) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(GET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getValueFromMemoryStore(methodContext)).orElse((double) 0)
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean object getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(GET) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(GET.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				proxyState.getValueFromMemoryStore(methodContext)
			),
			new PredicateMethodClassification<>(
				/* description */   "JavaBean boolean getter",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(IS) && method.getParameterCount() == 0,
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(IS.length())),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				ofNullable(proxyState.getValueFromMemoryStore(methodContext)).orElse(false)
			),
			new PredicateMethodClassification<>(
				/* description */   "add to list returning void",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(ADD) && method.getParameterCount() == 1 && method.getReturnType().equals(void.class),
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(ADD.length())) + "s",
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					proxyState.addValueToCollectionInMemoryStore(methodContext, (Serializable) args[0]);
					return null;
				}
			),
			new PredicateMethodClassification<>(
				/* description */   "remove from list returning boolean",
				/* matcher */       (method, proxyState) -> basePredicate.test(method) && method.getName().startsWith(REMOVE) && method.getParameterCount() == 1 && method.getReturnType().equals(boolean.class),
				/* methodContext */ (method, proxyState) -> StringUtils.uncapitalize(method.getName().substring(REMOVE.length())) + "s",
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
				proxyState.removeValueFromCollectionInMemoryStore(methodContext, (Serializable) args[0])
			)
		);
	}

}
