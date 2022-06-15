package one.edee.oss.proxycian.javassist;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import one.edee.oss.proxycian.CacheKeyProvider;
import one.edee.oss.proxycian.CurriedMethodContextInvocationHandler;
import one.edee.oss.proxycian.OnInstantiationCallback;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.ProxyStateWithConstructorArgs;
import one.edee.oss.proxycian.cache.ClassMethodCacheKey;
import one.edee.oss.proxycian.cache.ConstructorCacheKey;
import one.edee.oss.proxycian.exception.ProxyInstantiationException;
import one.edee.oss.proxycian.exception.SuperConstructorNotFoundException;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.SerializableProxy;
import one.edee.oss.proxycian.trait.SerializableProxy.DeserializationProxyFactory;
import one.edee.oss.proxycian.utils.ClassUtils;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JavassistProxyGenerator {
	// LIST OF "SYSTEM" INTERFACES THAT ARE ADDED TO OUR PROXIES AUTOMATICALLY EITHER BY US OR BY THE BYTECODE LIBRARY
	public static final Set<Class<?>> EXCLUDED_CLASSES = new HashSet<>(
		Arrays.asList(
			ProxyObject.class,
			ProxyStateAccessor.class
		)
	);
	static final Map<ClassMethodCacheKey, CurriedMethodContextInvocationHandler<?,?>> CLASSIFICATION_CACHE = new ConcurrentHashMap<>(32);
	private static final Map<List<Class<?>>, Class<?>> CACHED_PROXY_CLASSES = new ConcurrentHashMap<>(64);
	private static final Map<ConstructorCacheKey, Constructor<?>> CACHED_PROXY_CONSTRUCTORS = new ConcurrentHashMap<>(64);

	/**
	 * Method clears method classification cache that keeps direct references from proxied class methods to their
	 * implementation in the form of {@link PredicateMethodClassification#invocationHandler}. This cache
	 * speeds up method execution heavily.
	 */
	public static void clearMethodClassificationCache() {
		CLASSIFICATION_CACHE.clear();
	}

	/**
	 * Method clears cached classes. Please keep in mind, that classes are probably trapped in the {@link ClassLoader}
	 * and old JVMs were not able to purge non-used classes from the {@link ClassLoader} -
	 * <a href="https://stackoverflow.com/questions/2433261/when-and-how-are-classes-garbage-collected-in-java">see this answer</>.
	 * This cache allows reusing already generated classes for same combination of input interfaces / combination of
	 * {@link CacheKeyProvider} classifiers.
	 */
	public static void clearClassCache() {
		CACHED_PROXY_CLASSES.clear();
		CACHED_PROXY_CONSTRUCTORS.clear();
	}

	/**
	 * Method creates and instantiates new proxy object with passed set of interfaces and the invocation handler.
	 * See {@link com.fg.edee.proxy.model.traits.GenericBucketProxyGenerator#instantiateJavassistProxy(java.lang.Class)}
	 * for example usage.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(MethodHandler methodHandler, Class<?>... interfaces) {
		return instantiateProxy(
			(Class<T>) getProxyClass(interfaces),
			null, methodHandler, null
		);
	}

	/**
	 * Method creates and instantiates new proxy object with passed set of interfaces and the invocation handler.
	 * See {@link com.fg.edee.proxy.model.traits.GenericBucketProxyGenerator#instantiateJavassistProxy(java.lang.Class)}
	 * for example usage.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(MethodHandler methodHandler, Class<?>[] interfaces, Class<?>[] constructorTypes, Object[] constructorArgs) {
		return instantiateProxy(
			(Class<T>) getProxyClass(interfaces),
			null, methodHandler, null,
			constructorTypes, constructorArgs
		);
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is not Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(ProxyRecipe proxyRecipe, Object proxyState, Class<?>[] constructorTypes, Object[] constructorArgs) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfaces()
			),
			proxyState,
			new JavassistDispatcherInvocationHandler<>(
				proxyState,
				proxyRecipe.getMethodClassificationsWith()
			),
			proxyRecipe.getInstantiationCallback(),
			constructorTypes,
			constructorArgs
		);
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is not Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(ProxyRecipe proxyRecipe, Object proxyState) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfacesWith(ProxyStateAccessor.class)
			),
			proxyState,
			new JavassistDispatcherInvocationHandler<>(
				proxyState,
				proxyRecipe.getMethodClassificationsWith()
			),
			proxyRecipe.getInstantiationCallback()
		);
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateSerializable(ProxyRecipe proxyRecipe, Serializable proxyState) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfacesWith(
					SerializableProxy.class
				)
			),
			proxyState,
			new JavassistDispatcherInvocationHandler<>(
				proxyState,
				proxyRecipe.getMethodClassificationsWith(
					SerializableProxy.getWriteReplaceMethodInvoker(
						new ProxyRecipeDeserializationProxyFactory(proxyRecipe)
					)
				)
			),
			proxyRecipe.getInstantiationCallback()
		);
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateSerializable(ProxyRecipe proxyRecipe, ProxyStateWithConstructorArgs proxyState, Class<?>[] constructorTypes, Object[] constructorArgs) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfacesWith(
					SerializableProxy.class
				)
			),
			proxyState,
			new JavassistDispatcherInvocationHandler<>(
				proxyState,
				proxyRecipe.getMethodClassificationsWith(
					SerializableProxy.getWriteReplaceMethodInvoker(
						new ProxyRecipeDeserializationProxyFactory(proxyRecipe)
					)
				)
			),
			proxyRecipe.getInstantiationCallback(),
			constructorTypes,
			constructorArgs
		);
	}

	/**
	 * Returns previously created class or construct new from the passed interfaces. First class of the passed class
	 * array might be abstract class. In such situation the created class will extend this proxy class. All passed
	 * interfaces will be "implemented" by the returned proxy class.
	 */
	public static Class<?> getProxyClass(Class<?>... interfaces) {
		// COMPUTE IF ABSENT = GET FROM MAP, IF MISSING -> COMPUTE, STORE AND RETURN RESULT OF LAMBDA
		return CACHED_PROXY_CLASSES.computeIfAbsent(
			// CACHE KEY
			Arrays.asList(interfaces),
			// LAMBDA THAT CREATES OUR PROXY CLASS
			classes -> {
				final ProxyFactory fct = new ProxyFactory();

				// WE'LL CACHE CLASSES ON OUR OWN
				fct.setUseCache(false);

				// IF WE PROXY ABSTRACT CLASS, WE HAVE A RULE THAT IT HAS TO BE FIRST IN LIST
				if (interfaces[0].isInterface()) {
					// FIRST IS INTERFACE
					// AUTOMATICALLY ADD PROXYSTATEACCESSOR CLASS TO EVERY OUR PROXY WE CREATE
					final Class<?>[] finalContract = new Class[interfaces.length + 1];
					finalContract[0] = ProxyStateAccessor.class;
					System.arraycopy(interfaces, 0, finalContract, 1, interfaces.length);
					// WE'LL EXTEND OBJECT CLASS AND IMPLEMENT ALL INTERFACES
					fct.setInterfaces(finalContract);
				} else {
					// FIRST IS ABSTRACT CLASS
					// AUTOMATICALLY ADD PROXYSTATEACCESSOR CLASS TO EVERY OUR PROXY WE CREATE
					final Class<?>[] finalContract = new Class[interfaces.length];
					finalContract[0] = ProxyStateAccessor.class;
					System.arraycopy(interfaces, 1, finalContract, 1, interfaces.length - 1);
					// WE'LL EXTEND ABSTRACT CLASS AND IMPLEMENT ALL OTHER INTERFACES
					fct.setSuperclass(interfaces[0]);
					fct.setInterfaces(finalContract);
				}

				// SKIP FINALIZE METHOD OVERRIDE - STAY AWAY FROM TROUBLE :)

				/**

				 In Effective java (2nd edition ) Joshua bloch says,

				 "Oh, and one more thing: there is a severe performance penalty for using finalizers. On my machine, the time
				 to create and destroy a simple object is about 5.6 ns.
				 Adding a finalizer increases the time to 2,400 ns. In other words, it is about 430 times slower to create and
				 destroy objects with finalizers."

				 */

				fct.setFilter(method -> !Objects.equals(method.getName(), "finalize"));
				// DON'T USE CACHE - WE CACHE CLASSES OURSELVES
				fct.setUseCache(false);

				return fct.createClass();
			});
	}

	private static <T> T instantiateProxy(Class<T> proxyClass, Object proxyState, MethodHandler methodHandler, OnInstantiationCallback instantiationCallback) {
		try {

			// CREATE PROXY INSTANCE
			T proxy = getDefaultConstructor(proxyClass).newInstance();
			// CALL ON INSTANTIATION CALLBACK
			if (instantiationCallback != null) {
				instantiationCallback.proxyCreated(proxy, proxyState);
			}
			// INJECT OUR METHOD HANDLER INSTANCE TO NEWLY CREATED PROXY INSTANCE
			((Proxy) proxy).setHandler(methodHandler);

			return proxy;

		} catch (Exception e) {
			throw new ProxyInstantiationException("What the heck? Can't create proxy: " + e.getMessage(), e);
		}
	}

	private static <T> T instantiateProxy(Class<T> proxyClass, Object proxyState, MethodHandler methodHandler, OnInstantiationCallback instantiationCallback, Class<?>[] constructorTypes, Object[] constructorArgs) {
		try {

			// CREATE PROXY INSTANCE
			T proxy = getConstructor(proxyClass, constructorTypes).newInstance(constructorArgs);
			// CALL ON INSTANTIATION CALLBACK
			if (instantiationCallback != null) {
				instantiationCallback.proxyCreated(proxy, proxyState);
			}
			// INJECT OUR METHOD HANDLER INSTANCE TO NEWLY CREATED PROXY INSTANCE
			((Proxy) proxy).setHandler(methodHandler);

			return proxy;

		} catch (Exception e) {
			throw new ProxyInstantiationException("What the heck? Can't create proxy: " + e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) {
		// COMPUTE IF ABSENT = GET FROM MAP, IF MISSING -> COMPUTE, STORE AND RETURN RESULT OF LAMBDA
		return (Constructor<T>) CACHED_PROXY_CONSTRUCTORS.computeIfAbsent(
			// CACHE KEY
			new ConstructorCacheKey(clazz),
			// LAMBDA THAT FINDS OUT MISSING CONSTRUCTOR
			aClass -> {
				try {
					final Constructor<?> constructor = aClass.getClazz().getDeclaredConstructor();
					if (Modifier.isPrivate(constructor.getModifiers())) {
						throw new NoSuchMethodException();
					}
					if (Modifier.isProtected(constructor.getModifiers())) {
						constructor.setAccessible(true);
					}
					return constructor;
				} catch (NoSuchMethodException e) {
					throw new SuperConstructorNotFoundException(
						"What the heck? Can't find default public/protected constructor on abstract class: " + e.getMessage(), e
					);
				}
			}
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>[] constructorArgs) {
		// COMPUTE IF ABSENT = GET FROM MAP, IF MISSING -> COMPUTE, STORE AND RETURN RESULT OF LAMBDA
		return (Constructor<T>) CACHED_PROXY_CONSTRUCTORS.computeIfAbsent(
			// CACHE KEY
			new ConstructorCacheKey(clazz, constructorArgs),
			// LAMBDA THAT FINDS OUT MISSING CONSTRUCTOR
			aClass -> ClassUtils.findConstructor(aClass, constructorArgs)
		);
	}

	private static class ProxyRecipeDeserializationProxyFactory implements DeserializationProxyFactory {
		private static final long serialVersionUID = -4840857278948145538L;
		private final ProxyRecipe proxyRecipe;

		public ProxyRecipeDeserializationProxyFactory(ProxyRecipe proxyRecipe) {
			this.proxyRecipe = proxyRecipe;
		}

		@Override
		public @Nonnull Set<Class<?>> getExcludedInterfaces() {
			return new HashSet<>(Collections.singletonList(Serializable.class));
		}

		@Override
		public Object deserialize(@Nonnull Serializable proxyState, @Nonnull Class<?>[] interfaces) {
			return JavassistProxyGenerator.instantiateSerializable(proxyRecipe, proxyState);
		}

		@Override
		public Object deserialize(@Nonnull ProxyStateWithConstructorArgs proxyState, @Nonnull Class<?>[] interfaces, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs) {
			return JavassistProxyGenerator.instantiateSerializable(proxyRecipe, proxyState, constructorTypes, constructorArgs);
		}

	}
}
