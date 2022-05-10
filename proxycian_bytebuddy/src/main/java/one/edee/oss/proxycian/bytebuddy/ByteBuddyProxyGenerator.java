package one.edee.oss.proxycian.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder.FieldDefinition.Optional.Valuable;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Simple.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender.ForInstrumentedMethod;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.matcher.ElementMatchers;
import one.edee.oss.proxycian.CacheKeyProvider;
import one.edee.oss.proxycian.CurriedMethodContextInvocationHandler;
import one.edee.oss.proxycian.DispatcherInvocationHandler;
import one.edee.oss.proxycian.OnInstantiationCallback;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.ProxyStateWithConstructorArgs;
import one.edee.oss.proxycian.bytebuddy.generated.GeneratedDummyClass;
import one.edee.oss.proxycian.cache.ClassMethodCacheKey;
import one.edee.oss.proxycian.cache.ConstructorCacheKey;
import one.edee.oss.proxycian.recipe.ProxyRecipe;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.SerializableProxy;
import one.edee.oss.proxycian.trait.SerializableProxy.DeserializationProxyFactory;
import one.edee.oss.proxycian.utils.ArrayUtils;
import one.edee.oss.proxycian.utils.ClassUtils;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ByteBuddyProxyGenerator {
	static final Map<ClassMethodCacheKey, CurriedMethodContextInvocationHandler<?,?>> CLASSIFICATION_CACHE = new ConcurrentHashMap<>(32);
	static final Map<ClassMethodCacheKey, MethodHandle> DEFAULT_METHOD_CACHE = new ConcurrentHashMap<>(32);
	public static final String INVOCATION_HANDLER_FIELD = "dispatcherInvocationHandler";
	// LIST OF "SYSTEM" INTERFACES THAT ARE ADDED TO OUR PROXIES AUTOMATICALLY EITHER BY US OR BY THE BYTECODE LIBRARY
	public static final Set<Class<?>> EXCLUDED_CLASSES = new HashSet<>(
		Collections.singletonList(
			ProxyStateAccessor.class
		)
	);
	public static final String GENERATED_PACKAGE_PATH = "one.edee.oss.proxycian.bytebuddy.generated.";

	private static final Map<List<Class<?>>, Class<?>> CACHED_PROXY_CLASSES = new ConcurrentHashMap<>(64);
	private static final Map<ConstructorCacheKey, Constructor<?>> CACHED_PROXY_CONSTRUCTORS = new ConcurrentHashMap<>(64);
	private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);
	private static final Method PROXY_CREATED_METHOD;

	static {
		try {
			PROXY_CREATED_METHOD = OnInstantiationCallback.class.getDeclaredMethod("proxyCreated", Object.class, Object.class);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Method `proxyCreated` not found on OnInstantiationCallback!");
		}
	}

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
	 * See {@link com.fg.edee.proxy.model.traits.GenericBucketProxyGenerator#instantiateByteBuddyProxy(java.lang.Class)}
	 * for example usage.
	 */
	public static <T> T instantiate(@Nonnull DispatcherInvocationHandler invocationHandler, @Nonnull Class<?>... interfaces) {
		return instantiate(invocationHandler, interfaces, ByteBuddyProxyGenerator.class.getClassLoader());
	}

	/**
	 * Method creates and instantiates new proxy object with passed set of interfaces and the invocation handler.
	 * See {@link com.fg.edee.proxy.model.traits.GenericBucketProxyGenerator#instantiateByteBuddyProxy(java.lang.Class)}
	 * for example usage.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(@Nonnull DispatcherInvocationHandler invocationHandler, @Nonnull Class<?>[] interfaces, @Nonnull ClassLoader classLoader) {
		return instantiateProxy(
			(Class<T>) getProxyClass(interfaces, classLoader),
			null, invocationHandler, null
		);
	}

	/**
	 * Method creates and instantiates new proxy object with passed set of interfaces and the invocation handler.
	 * See {@link com.fg.edee.proxy.model.traits.GenericBucketProxyGenerator#instantiateByteBuddyProxy(java.lang.Class)}
	 * for example usage.
	 */
	public static <T> T instantiate(@Nonnull DispatcherInvocationHandler invocationHandler, @Nonnull Class<?>[] interfaces, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs) {
		return instantiate(invocationHandler, interfaces, constructorTypes, constructorArgs, ByteBuddyProxyGenerator.class.getClassLoader());
	}

	/**
	 * Method creates and instantiates new proxy object with passed set of interfaces and the invocation handler.
	 * See {@link com.fg.edee.proxy.model.traits.GenericBucketProxyGenerator#instantiateByteBuddyProxy(java.lang.Class)}
	 * for example usage.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(@Nonnull DispatcherInvocationHandler invocationHandler, @Nonnull Class<?>[] interfaces, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs, @Nonnull ClassLoader classLoader) {
		return instantiateProxy(
			(Class<T>) getProxyClass(interfaces, classLoader),
			null, invocationHandler, null,
			constructorTypes, constructorArgs
		);
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is not Serializable.
	 */
	public static <T> T instantiate(@Nonnull ProxyRecipe proxyRecipe, @Nonnull Object proxyState) {
		return instantiate(proxyRecipe, proxyState, ByteBuddyProxyGenerator.class.getClassLoader());
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is not Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(@Nonnull ProxyRecipe proxyRecipe, @Nonnull Object proxyState, @Nonnull ClassLoader classLoader) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfaces(), classLoader
			),
			proxyState,
			new ByteBuddyDispatcherInvocationHandler<>(
				proxyState,
				proxyRecipe.getMethodClassificationsWith()
			),
			proxyRecipe.getInstantiationCallback()
		);
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is not Serializable.
	 */
	public static <T> T instantiate(@Nonnull ProxyRecipe proxyRecipe, @Nonnull Object proxyState, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs) {
		return instantiate(proxyRecipe, proxyState, constructorTypes, constructorArgs, ByteBuddyProxyGenerator.class.getClassLoader());
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is not Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiate(@Nonnull ProxyRecipe proxyRecipe, @Nonnull Object proxyState, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs, @Nonnull ClassLoader classLoader) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfaces(),
				classLoader
			),
			proxyState,
			new ByteBuddyDispatcherInvocationHandler<>(
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
	 * object as proxy internal memory. The created proxy is Serializable.
	 */
	public static <T> T instantiateSerializable(@Nonnull ProxyRecipe proxyRecipe, @Nonnull Serializable proxyState) {
		return instantiateSerializable(proxyRecipe, proxyState, ByteBuddyProxyGenerator.class.getClassLoader());
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateSerializable(@Nonnull ProxyRecipe proxyRecipe, @Nonnull Serializable proxyState, @Nonnull ClassLoader classLoader) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfacesWith(
					SerializableProxy.class
				),
				classLoader
			),
			proxyState,
			new ByteBuddyDispatcherInvocationHandler<>(
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
	public static <T> T instantiateSerializable(@Nonnull ProxyRecipe proxyRecipe, @Nonnull ProxyStateWithConstructorArgs proxyState, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs) {
		return instantiateSerializable(proxyRecipe, proxyState, constructorTypes, constructorArgs, ByteBuddyProxyGenerator.class.getClassLoader());
	}

	/**
	 * Method creates and instantiates new proxy object defined by passed {@link ProxyRecipe} and uses passed `proxyState`
	 * object as proxy internal memory. The created proxy is Serializable.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateSerializable(@Nonnull ProxyRecipe proxyRecipe, @Nonnull Serializable proxyState, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs, @Nonnull ClassLoader classLoader) {
		proxyRecipe.verifyProxyState(proxyState);
		return instantiateProxy(
			(Class<T>) getProxyClass(
				proxyRecipe.getInterfacesWith(
					SerializableProxy.class
				),
				classLoader
			),
			proxyState,
			new ByteBuddyDispatcherInvocationHandler<>(
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
	public static Class<?> getProxyClass(@Nonnull Class<?>[] interfaces) {
		return getProxyClass(interfaces, ByteBuddyProxyGenerator.class.getClassLoader());
	}

	/**
	 * Returns previously created class or construct new from the passed interfaces. First class of the passed class
	 * array might be abstract class. In such situation the created class will extend this proxy class. All passed
	 * interfaces will be "implemented" by the returned proxy class.
	 */
	public static Class<?> getProxyClass(@Nonnull Class<?>[] interfaces, @Nonnull ClassLoader classLoader) {
		// COMPUTE IF ABSENT = GET FROM MAP, IF MISSING -> COMPUTE, STORE AND RETURN RESULT OF LAMBDA
		return CACHED_PROXY_CLASSES.computeIfAbsent(
			// CACHE KEY
			Arrays.asList(interfaces),
			// LAMBDA THAT CREATES OUR PROXY CLASS
			classes -> {

				DynamicType.Builder<?> builder;

				final Class<?> superClass;
				final String className;
				// IF WE PROXY ABSTRACT CLASS, WE HAVE A RULE THAT IT HAS TO BE FIRST IN LIST
				if (interfaces[0].isInterface()) {
					// FIRST IS INTERFACE
					// AUTOMATICALLY ADD PROXYSTATEACCESSOR CLASS TO EVERY OUR PROXY WE CREATE
					final Class<?>[] finalContract = new Class[interfaces.length + 1];
					finalContract[0] = ProxyStateAccessor.class;
					System.arraycopy(interfaces, 0, finalContract, 1, interfaces.length);
					// WE'LL EXTEND OBJECT CLASS AND IMPLEMENT ALL INTERFACES
					superClass = Object.class;
					className = interfaces[0].getSimpleName();
					builder = new ByteBuddy().subclass(Object.class).implement(finalContract);
				} else {
					// FIRST IS ABSTRACT CLASS
					superClass = interfaces[0];
					className = superClass.getSimpleName();
					// AUTOMATICALLY ADD PROXYSTATEACCESSOR CLASS TO EVERY OUR PROXY WE CREATE
					final Class<?>[] finalContract = new Class[interfaces.length];
					finalContract[0] = ProxyStateAccessor.class;
					if (interfaces.length > 1) {
						System.arraycopy(interfaces, 1, finalContract, 1, interfaces.length - 1);
					}
					// WE'LL EXTEND ABSTRACT CLASS AND IMPLEMENT ALL OTHER INTERFACES
					builder = new ByteBuddy().subclass(superClass).implement(finalContract);
				}

				final Valuable<?> theBuilder = builder
					// WE CAN DEFINE OUR OWN PACKAGE AND NAME FOR THE CLASS
					.name(GENERATED_PACKAGE_PATH + className + '_' + CLASS_COUNTER.incrementAndGet())
					// WE'LL CREATE PRIVATE FINAL FIELD FOR STORING OUR INVOCATION HANDLER ON INSTANCE
					.defineField(INVOCATION_HANDLER_FIELD, ByteBuddyDispatcherInvocationHandler.class, Modifier.PRIVATE + Modifier.FINAL);

				// OVERRIDE ALL PUBLIC/PROTECTED CONSTRUCTOR FOUND ON SUPERCLASS WITH CUSTOM BEHAVIOUR
				final Constructor<?>[] superClassConstructors = superClass.getDeclaredConstructors();
				ReceiverTypeDefinition<?> ongoingDefinition = null;
				for (final Constructor<?> superClassConstructor : superClassConstructors) {
					if (Modifier.isPublic(superClassConstructor.getModifiers()) || Modifier.isProtected(superClassConstructor.getModifiers())) {
						Annotatable<?> baseConstructorBuilder = (ongoingDefinition == null ? theBuilder : ongoingDefinition)
							// LET'S HAVE PUBLIC CONSTRUCTOR
							.defineConstructor(superClassConstructor.getModifiers())
							// ACCEPTING SINGLE ARGUMENT OF OUR INVOCATION HANDLER
							.withParameter(ByteBuddyDispatcherInvocationHandler.class)
							.withParameter(OnInstantiationCallback.class)
							.withParameter(Object.class);

						final Class<?>[] superClassConstructorParameterTypes = superClassConstructor.getParameterTypes();
						final int length = superClassConstructorParameterTypes.length;
						int[] indexes = new int[length];
						for (int j = 0; j < length; j++) {
							final Class<?> constructorArgument = superClassConstructorParameterTypes[j];
							baseConstructorBuilder = baseConstructorBuilder.withParameter(constructorArgument);
							indexes[j] = j + 3;
						}

						// AND THIS CONSTRUCTOR WILL
						ongoingDefinition = baseConstructorBuilder.intercept(
							MethodCall
								// CALL DEFAULT (NON-ARG) CONSTRUCTOR ON SUPERCLASS
								.invoke(superClassConstructor)
								.onSuper()
								.withArgument(indexes)
								// AND THEN CALL ON INSTANTIATION CALLBACK PASSED IN ARGUMENT
								.andThen(
									MethodCall.invoke(PROXY_CREATED_METHOD)
										.onArgument(1)
										.withThis()
										.withArgument(2)
										.withAssigner(Assigner.DEFAULT, Typing.DYNAMIC)
								)
								// AND THEN FILL PRIVATE FIELD WITH PASSED INVOCATION HANDLER
								.andThen(
									FieldAccessor.ofField(INVOCATION_HANDLER_FIELD).setsArgumentAt(0)
								)
						);
					}
				}

				return Objects.requireNonNull(ongoingDefinition)
					// AND TRAP ALL METHODS EXCEPT CONSTRUCTORS AND FINALIZER
					.method(
						ElementMatchers.noneOf(
							ElementMatchers.isFinalizer(), ElementMatchers.isConstructor()
						)
					)
					// AND DELEGATE CALL TO OUR INVOCATION HANDLER STORED IN PRIVATE FIELD OF THE CLASS
					.intercept(MethodDelegation.to(ByteBuddyDispatcherInvocationHandler.class))
					// COPY ALL METHOD / PARAMETER ANNOTATIONS TO THE OVERRIDEN METHODS
					.attribute(ForInstrumentedMethod.EXCLUDING_RECEIVER)
					// NOW CREATE THE BYTE-CODE
					.make()
					// AND LOAD IT IN CURRENT CLASSLOADER
					/* see https://github.com/raphw/byte-buddy/issues/513 and http://mydailyjava.blogspot.com/2018/04/jdk-11-and-proxies-in-world-past.html */
					/* this needs to be changed with upgrade to JDK 11 */
					.load(classLoader, new StrategyProvider().getStrategy(GeneratedDummyClass.class))
					// RETURN
					.getLoaded();
			});
	}

	private static <T> T instantiateProxy(Class<T> proxyClass, Object proxyState, DispatcherInvocationHandler invocationHandler, OnInstantiationCallback instantiationCallback) {
		try {
			final Constructor<T> constructor = getConstructor(proxyClass, new Class[] {ByteBuddyDispatcherInvocationHandler.class, OnInstantiationCallback.class, Object.class});
			return constructor.newInstance(
				invocationHandler,
				instantiationCallback == null ? OnInstantiationCallback.DEFAULT : instantiationCallback,
				proxyState
			);
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("What the heck? Can't create proxy: " + e.getMessage(), e);
		}
	}

	private static <T> T instantiateProxy(Class<T> proxyClass, Object proxyState, DispatcherInvocationHandler invocationHandler, OnInstantiationCallback instantiationCallback, Class<?>[] constructorTypes, Object[] constructorArgs) {
		try {
			final Constructor<T> constructor = getConstructor(
				proxyClass,
				ArrayUtils.mergeArrays(new Class<?>[] {ByteBuddyDispatcherInvocationHandler.class, OnInstantiationCallback.class, Object.class}, constructorTypes)
			);
			return constructor.newInstance(
				ArrayUtils.mergeArrays(
					new Object[] {
						invocationHandler,
						instantiationCallback == null ? OnInstantiationCallback.DEFAULT : instantiationCallback,
						proxyState
					},
					constructorArgs
				)
			);
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("What the heck? Can't create proxy: " + e.getMessage(), e);
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
					throw new IllegalArgumentException("What the heck? Can't find default public/protected constructor on abstract class: " + e.getMessage(), e);
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

	public static class ProxyRecipeDeserializationProxyFactory implements DeserializationProxyFactory {
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
			return ByteBuddyProxyGenerator.instantiateSerializable(proxyRecipe, proxyState);
		}

		@Override
		public Object deserialize(@Nonnull ProxyStateWithConstructorArgs proxyState, @Nonnull Class<?>[] interfaces, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs) {
			return ByteBuddyProxyGenerator.instantiateSerializable(proxyRecipe, proxyState, constructorTypes, constructorArgs);
		}
	}

}
