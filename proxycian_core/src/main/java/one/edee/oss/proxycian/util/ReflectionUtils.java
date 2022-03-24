package one.edee.oss.proxycian.util;

import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public interface ReflectionUtils {
	Map<Class<?>, Map<String, PropertyDescriptor>> PROPERTY_ACCESSOR_CACHE = new WeakHashMap<>(128);

	/**
	 * Retrieves parameter names from the method via. reflection.
	 * Requires "-parameters" argument to be used during compilation.
	 */
	static String[] getParameterNames(Method method) {
		final String[] parameterNames = new String[method.getParameterCount()];
		final Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			final Parameter param = parameters[i];
			if (!param.isNamePresent()) {
				throw new IllegalStateException("Source code is not compiled with -parameters argument!");
			}
			parameterNames[i] = param.getName();
		}
		return parameterNames;
	}

	/**
	 * Retrieves property value from the object by calling getter method on it. It is usually used for retrieving
	 * "computed" properties.
	 */
	static Object getProperty(Object object, String propertyName) {
		// collect and cache property descriptors for certain class
		final Map<String, PropertyDescriptor> beanDescriptors = PROPERTY_ACCESSOR_CACHE.computeIfAbsent(
			object.getClass(),
			aClass -> {
				try {
					return Stream.of(Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors())
						.collect(toMap(FeatureDescriptor::getName, identity()));
				} catch (IntrospectionException e) {
					throw new IllegalArgumentException(
						"Can't introspect class " + object.getClass().getName() + "!", e
					);
				}
			});

		// invoke read method to gather value
		return ofNullable(beanDescriptors.get(propertyName))
			.map(pd -> {
				try {
					return pd.getReadMethod().invoke(object);
				} catch (Exception e) {
					// error in getter?!
					throw new RuntimeException(e);
				}
			})
			.orElse(null);
	}

	/**
	 * Finds method handle for the default method.
	 */
	static MethodHandle findMethodHandle(Method method) {
		try {

			final Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
			constructor.setAccessible(true);

			final Class<?> declaringClass = method.getDeclaringClass();
			return constructor
				.newInstance(declaringClass, Lookup.PRIVATE)
				.unreflectSpecial(method, declaringClass);

		} catch (Exception ex) {
			throw new IllegalArgumentException("Can't find handle to method " + method.toGenericString() + "!", ex);
		}

	}

	/**
	 * Returns true if method equals method onClass with the same name and same parameters.
	 */
	static boolean isMethodDeclaredOn(Method method, Class<?> onClass, String withSameName, Class<?>... withSameTypes) {
		try {
			return method.equals(onClass.getMethod(withSameName, withSameTypes));
		} catch (NoSuchMethodException ex) {
			return false;
		} catch (Exception ex) {
			throw new IllegalStateException(
				"Matcher " + onClass.getName() + " failed to process " + method.toGenericString() + ": " + ex.getMessage(), ex
			);
		}
	}

	/**
	 * Returns true if method equals method onClass with the same name and same parameters.
	 */
	static boolean isNonPublicMethodDeclaredOn(Method method, Class<?> onClass, String withSameName, Class<?>... withSameTypes) {
		try {
			final Method lookedUpMethod = onClass.getDeclaredMethod(withSameName, withSameTypes);
			return method.equals(lookedUpMethod) && !Modifier.isPublic(lookedUpMethod.getModifiers());
		} catch (NoSuchMethodException ex) {
			return false;
		} catch (Exception ex) {
			throw new IllegalStateException(
				"Matcher " + onClass.getName() + " failed to process " + method.toGenericString() + ": " + ex.getMessage(), ex
			);
		}
	}

	/**
	 * Returns true if method matches the same name and same parameters.
	 */
	static boolean isMethodMatching(Method method, String name, Class<?>... withSameTypes) {
		return name.equals(method.getName()) && Arrays.equals(withSameTypes, method.getParameterTypes());
	}

	/**
	 * Returns true if method equals method on any of `onClass` with the same name and same parameters.
	 */
	static boolean isMatchingMethodPresentOn(Method method, Class<?>... onClass) {
		for (Class<?> aClass : onClass) {
			try {
				aClass.getMethod(method.getName(), method.getParameterTypes());
				return true;
			} catch (NoSuchMethodException ex) {
				// just continue
			} catch (Exception ex) {
				throw new IllegalStateException(
					"Matcher " + aClass.getName() + " failed to process " + method.toGenericString() + ": " + ex.getMessage(), ex
				);
			}
		}
		return false;
	}

	/**
	 * Returns true if method is not equal to a method on any of `onClass` with the same name and same parameters.
	 */
	static boolean isMatchingMethodNotPresentOn(Method method, Class<?>... onClass) {
		return !isMatchingMethodPresentOn(method, onClass);
	}

}
