package one.edee.oss.proxycian.trait;

import one.edee.oss.proxycian.DispatcherInvocationHandler;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.ProxyStateWithConstructorArgs;
import one.edee.oss.proxycian.util.ReflectionUtils;

import javax.annotation.Nonnull;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

public interface SerializableProxy extends Serializable {

	static <T extends Serializable> PredicateMethodClassification<ProxyStateAccessor, Void, T> getWriteReplaceMethodInvoker(DeserializationProxyFactory deserializationProxyFactory) {
		return new PredicateMethodClassification<>(
			/* description */   "SerializableProxy.writeReplace()",
			/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, SerializableProxy.class, "writeReplace"),
			/* methodContext */ (method, proxyState) -> null,
			/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) ->
			new SerializableProxyDescriptor<>(
				deserializationProxyFactory,
				proxy,
				proxyState,
				combineInterfaces(
					proxy.getClass().getSuperclass(),
					proxy.getClass().getInterfaces(),
					deserializationProxyFactory.getExcludedInterfaces()
				)
			)
		);
	}

	/**
	 * Combines superclass with interfaces into single list excluding "system" interfaces that are added to the proxy
	 * automatically.
	 */
	static Class<?>[] combineInterfaces(Class<?> superclass, Class<?>[] interfaces, Set<Class<?>> excludedClasses) {
		final List<Class<?>> combined = new LinkedList<>();
		if (!Object.class.equals(superclass) && !excludedClasses.contains(superclass)) {
			combined.add(superclass);
		}
		for (Class<?> anInterface : interfaces) {
			if (!excludedClasses.contains(anInterface)) {
				combined.add(anInterface);
			}
		}

		return combined.toArray(new Class[0]);
	}

	/**
	 * This method will be called
	 */
	Object writeReplace() throws ObjectStreamException;

	/**
	 * Logic that will deserialize {@link SerializableProxyDescriptor} back to the proxy class and instance.
	 * We need this only because we want to support several byte code generation libraries here in this example.
	 */
	interface DeserializationProxyFactory extends Serializable {

		@Nonnull
		Set<Class<?>> getExcludedInterfaces();

		Object deserialize(@Nonnull Serializable proxyState, @Nonnull Class<?>[] interfaces);

		Object deserialize(@Nonnull ProxyStateWithConstructorArgs proxyState, @Nonnull Class<?>[] interfaces, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs);

	}

	/**
	 * Recipe how to recreate proxy class and its instance on deserialization. Contains all mandatory informations
	 * to create identical proxy on deserialization.
	 */
	class SerializableProxyDescriptor<T extends Serializable> implements Serializable {
		private static final long serialVersionUID = 8401525823871149500L;
		private final Class<?>[] interfaces;
		private final T proxyState;
		private final DeserializationProxyFactory deserializationProxyFactory;
		private final Map<String, Serializable> fieldValues;

		private SerializableProxyDescriptor(DeserializationProxyFactory deserializationProxyFactory, Object proxy, T proxyState, Class<?>... interfaces) {
			this.interfaces = interfaces;
			this.proxyState = proxyState;
			this.deserializationProxyFactory = deserializationProxyFactory;
			this.fieldValues = !interfaces[0].isInterface() ? gatherFieldValues(proxy, interfaces[0]) : Collections.emptyMap();
		}

		/**
		 * This method will be called by JDK to deserialize object.
		 */
		protected Object readResolve() throws ObjectStreamException {
			final Object deserializedProxy = proxyState instanceof ProxyStateWithConstructorArgs ?
				deserializationProxyFactory.deserialize(
					(ProxyStateWithConstructorArgs) proxyState, interfaces,
					((ProxyStateWithConstructorArgs) proxyState).getConstructorTypes(),
					((ProxyStateWithConstructorArgs) proxyState).getConstructorArgs()
				) : deserializationProxyFactory.deserialize(proxyState, interfaces);
			writeFieldValues(deserializedProxy, this.fieldValues);
			return deserializedProxy;
		}

		private Map<String, Serializable> gatherFieldValues(Object pojo, Class<?> mainClass) {
			final Map<String, Serializable> data = new HashMap<>();
			try {
				Class<?> currentClass = mainClass;
				while(currentClass != null && !currentClass.equals(Object.class)) {
					for(Field field : currentClass.getDeclaredFields()) {
						if (!Modifier.isStatic(field.getModifiers())) {
							try {
								makeAccessible(field);

								final Object value = field.get(pojo);
								if (!(value instanceof DispatcherInvocationHandler)) {
									if (value instanceof Serializable) {
										data.put(field.getName(), (Serializable) value);
									} else {
										// field value is not Serializable!
									}
								}
							} catch (Exception ignored) {
								// cannot access field
							}
						}
					}

					currentClass = currentClass.getSuperclass();
				}
			} catch (Exception ex) {
				// cannot serialize additional fields of the proxy
			}
			return data;
		}

		private void writeFieldValues(Object proxy, Map<String, Serializable> fieldValues) {
			//init all pojo fields
			Class<?> currentClass = proxy.getClass();
			do {
				final Iterator<Entry<String, Serializable>> it = fieldValues.entrySet().iterator();
				while (it.hasNext()) {
					final Entry<String, Serializable> fieldEntry = it.next();
					try {
						final Field field = currentClass.getDeclaredField(fieldEntry.getKey());
						makeAccessible(field);
						field.set(proxy, fieldEntry.getValue());
						it.remove();
					} catch (NoSuchFieldException ex) {
						// continue
					} catch (IllegalAccessException ex) {
						// cannot deserialize class field
					}
				}
				currentClass = currentClass.getSuperclass();
			} while (!fieldValues.isEmpty() && !Object.class.equals(currentClass));

			if (!fieldValues.isEmpty()) {
				// fields were not deserialized, there are no matching fields in proxy class
			}
		}

		private void makeAccessible(Field field) {
			if ((!Modifier.isPublic(field.getModifiers()) ||
				!Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
				Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
				field.setAccessible(true);
			}
		}

	}

}
