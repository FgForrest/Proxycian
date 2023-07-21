package one.edee.oss.proxycian.utils;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.lang.reflect.*;
import java.util.*;

/**
 * This helper class contains generic reflection utils.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenericsUtils {

	/**
	 * This method will resolve return type of the method signature. Generic will be translated to the
	 * simple type declaration.
	 */
	public static Class<?> getMethodReturnType(Class<?> mainClass, Method method) {
		Type genericReturnType = method.getGenericReturnType();
		Class<?> returnType = method.getReturnType();
		if (genericReturnType == returnType) {
			return returnType;
		} else {
			if (!(genericReturnType instanceof Class)) {
				if (mainClass != null) {
					List<GenericBundle> resolvedTypes = getGenericType(mainClass, genericReturnType);
					if (!resolvedTypes.isEmpty()) {
						return resolvedTypes.get(0).getResolvedType();
					}
				}
			}
			return returnType;
		}
	}

	/**
	 * This method will resolve field type of the class field. Generic will be translated to the
	 * simple type declaration.
	 */
	public static Class<?> getFieldType(Class<?> mainClass, Field field) {
		Type genericReturnType = field.getGenericType();
		Class<?> returnType = field.getType();
		if (genericReturnType == returnType) {
			return returnType;
		} else {
			if (!(genericReturnType instanceof Class)) {
				if (mainClass != null) {
					List<GenericBundle> resolvedTypes = getGenericType(mainClass, genericReturnType);
					if (!resolvedTypes.isEmpty()) {
						return resolvedTypes.get(0).getResolvedType();
					}
				}
			}
			return returnType;
		}
	}

	/**
	 * This method will resolve record component type of the record. Generic will be translated to the
	 * simple type declaration.
	 */
	public static Class<?> getRecordComponentType(Class<?> mainClass, RecordComponent recordComponent) {
		Type genericReturnType = recordComponent.getGenericType();
		Class<?> returnType = recordComponent.getType();
		if (genericReturnType == returnType) {
			return returnType;
		} else {
			if (!(genericReturnType instanceof Class)) {
				if (mainClass != null) {
					List<GenericBundle> resolvedTypes = getGenericType(mainClass, genericReturnType);
					if (!resolvedTypes.isEmpty()) {
						return resolvedTypes.get(0).getResolvedType();
					}
				}
			}
			return returnType;
		}
	}

	/**
	 * This method will resolve return type of the method signature. Generic will be translated to the
	 * simple type declaration.
	 */
	public static List<GenericBundle> getNestedMethodReturnTypes(Class<?> mainClass, Method method) {
		Type genericReturnType = method.getGenericReturnType();
		Class<?> returnType = method.getReturnType();
		if (genericReturnType == returnType) {
			return Collections.singletonList(new GenericBundle(returnType));
		} else {
			if (!(genericReturnType instanceof Class)) {
				if (mainClass != null) {
					List<GenericBundle> resolvedTypes = getGenericType(mainClass, genericReturnType);
					if (!resolvedTypes.isEmpty()) {
						return resolvedTypes;
					}
				}
			}
			return Collections.singletonList(new GenericBundle(returnType));
		}
	}

	/**
	 * This method will resolve return type of the method signature. Generic will be translated to the
	 * simple type declaration.
	 */
	public static List<GenericBundle> getNestedFieldTypes(Class<?> mainClass, Field field) {
		Type genericReturnType = field.getGenericType();
		Class<?> returnType = field.getType();
		if (genericReturnType == returnType) {
			return Collections.singletonList(new GenericBundle(returnType));
		} else {
			if (!(genericReturnType instanceof Class)) {
				if (mainClass != null) {
					List<GenericBundle> resolvedTypes = getGenericType(mainClass, genericReturnType);
					if (!resolvedTypes.isEmpty()) {
						return resolvedTypes;
					}
				}
			}
			return Collections.singletonList(new GenericBundle(returnType));
		}
	}

	/**
	 * This method will resolve return type of the method signature. Generic will be translated to the
	 * simple type declaration.
	 */
	public static List<GenericBundle> getNestedRecordComponentType(Class<?> mainClass, RecordComponent recordComponent) {
		Type genericReturnType = recordComponent.getGenericType();
		Class<?> returnType = recordComponent.getType();
		if (genericReturnType == returnType) {
			return Collections.singletonList(new GenericBundle(returnType));
		} else {
			if (!(genericReturnType instanceof Class)) {
				if (mainClass != null) {
					List<GenericBundle> resolvedTypes = getGenericType(mainClass, genericReturnType);
					if (!resolvedTypes.isEmpty()) {
						return resolvedTypes;
					}
				}
			}
			return Collections.singletonList(new GenericBundle(returnType));
		}
	}

	/**
	 * Returns generic class of the return type if declared.
	 */
	public static Class<?> getGenericTypeFromCollection(Class<?> mainClass, Type returnType) {
		if (returnType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) returnType;
			Type[] typeArguments = type.getActualTypeArguments();
			if (typeArguments.length == 1) {
				Type typeArgument = typeArguments[0];
				if (typeArgument instanceof Class) {
					return (Class<?>) typeArgument;
				} else if (typeArgument instanceof ParameterizedType) {
					return (Class<?>) ((ParameterizedType) typeArgument).getRawType();
				} else {
					List<GenericBundle> arguments;
					if (mainClass != null) {
						arguments = getGenericType(mainClass, typeArgument);
					} else if (typeArgument instanceof WildcardType) {
						Type[] upperBounds = ((WildcardType) typeArgument).getUpperBounds();
						if (upperBounds != null && upperBounds.length == 1) {
							arguments = Collections.singletonList(getClass(upperBounds[0], Collections.emptyMap()));
						} else {
							throw new IllegalArgumentException("Cannot handle generic type: " + returnType);
						}
					} else {
						throw new IllegalArgumentException("Cannot handle generic type: " + returnType);
					}
					if (!arguments.isEmpty() && arguments.get(0) != null) {
						return arguments.get(0).getResolvedType();
					} else {
						try {
							return (Class<?>) ((TypeVariable<?>) typeArgument).getBounds()[0];
						} catch (Exception ex) {
							throw new IllegalArgumentException("Cannot handle generic type: " + returnType, ex);
						}
					}
				}
			} else {
				throw new IllegalArgumentException("Expected single generic type in method return declaration!");
			}
		} else {
			return null;
		}
	}

	/**
	 * Get the actual type arguments a child class has used to extend a generic base class.
	 *
	 * @param childClass the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	@SuppressWarnings({"StaticMethodOnlyUsedInOneClass"})
	public static List<GenericBundle> getGenericType(Class<?> childClass, Type searchedType) {
		Map<Type, Type> resolvedTypes = new HashMap<>();
		Set<Type> examinedTypes = new HashSet<>();
		//walk through entire hierarchy
		classWalk(childClass, examinedTypes, resolvedTypes);

		// finally, for each actual type argument provided to baseClass, determine (if possible)
		// the raw class for that type argument.
		Type[] actualTypeArguments;
		if (searchedType instanceof Class) {
			actualTypeArguments = ((GenericDeclaration) searchedType).getTypeParameters();
		} else if (searchedType instanceof ParameterizedType) {
			actualTypeArguments = ((ParameterizedType) searchedType).getActualTypeArguments();
		} else {
			actualTypeArguments = new Type[]{searchedType};
		}
		List<GenericBundle> typeArgumentsAsClasses = new ArrayList<>(actualTypeArguments.length);
		// resolve types by chasing down type variables.
		for (Type baseType : actualTypeArguments) {
			while (resolvedTypes.containsKey(baseType)) {
				baseType = resolvedTypes.get(baseType);
			}
			typeArgumentsAsClasses.add(getClass(baseType, resolvedTypes));
		}
		return typeArgumentsAsClasses;
	}

	/**
	 * Get the underlying class for a type, or null if the type is a variable type.
	 *
	 * @param type          the type
	 * @param resolvedTypes
	 * @return the underlying class
	 */
	@SuppressWarnings({"TailRecursion"})
	public static GenericBundle getClass(Type type, Map<Type, Type> resolvedTypes) {
		if (type instanceof Class) {
			return new GenericBundle((Class<?>) type);
		} else if (type instanceof ParameterizedType) {
			final ParameterizedType parameterizedType = (ParameterizedType) type;
			final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
			final GenericBundle[] childBundles = new GenericBundle[actualTypeArguments.length];
			for (Type actualTypeArgument : actualTypeArguments) {
				childBundles[0] = getClass(actualTypeArgument, resolvedTypes);
			}
			return new GenericBundle(
					getClass(parameterizedType.getRawType(), resolvedTypes).getResolvedType(),
					childBundles
			);
		} else if (type instanceof GenericArrayType) {
			final GenericArrayType arrayType = (GenericArrayType) type;
			Type componentType = arrayType.getGenericComponentType();
			Class<?> componentClass = getClass(componentType, resolvedTypes).getResolvedType();
			if (componentClass != null) {
				return new GenericBundle(Array.newInstance(componentClass, 0).getClass());
			} else {
				return null;
			}
		} else if (type instanceof TypeVariable<?>) {
			final TypeVariable<?> typeVariable = (TypeVariable<?>) type;
			final Type resolvedType = resolvedTypes.get(typeVariable);
			return resolvedType == null ? null : getClass(resolvedType, resolvedTypes);
		} else {
			return null;
		}
	}

	/**
	 * This methos will recursively walk through class inheritance / implementation tree and resolve all
	 * generic types (if possible).
	 */
	private static void classWalk(Type type, Set<Type> examinedTypes, Map<Type, Type> resolvedTypes) {
		if (type instanceof Class) {
			Class<?> typeClass = (Class<?>) type;
			examinedTypes.add(typeClass);
			Type superClass = typeClass.getGenericSuperclass();
			if (superClass != null) {
				classWalk(superClass, examinedTypes, resolvedTypes);
			}
			Type[] interfaces = typeClass.getGenericInterfaces();
			for (Type anInterface : interfaces) {
				classWalk(anInterface, examinedTypes, resolvedTypes);
			}
		} else if (type != null) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Class<?> rawType = (Class<?>) parameterizedType.getRawType();

			Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
			TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
			for (int i = 0; i < actualTypeArguments.length; i++) {
				final Type alreadyResolvedType = resolvedTypes.get(typeParameters[i]);
				if (!(alreadyResolvedType instanceof Class)) {
					resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
				}
			}

			classWalk(rawType, examinedTypes, resolvedTypes);
		}
	}

	@Data
	@RequiredArgsConstructor
	public static class GenericBundle {
		private final Class<?> resolvedType;
		private final GenericBundle[] genericTypes;

		public GenericBundle(@Nonnull Class<?> resolvedType) {
			this.resolvedType = resolvedType;
			this.genericTypes = null;
		}
	}

}

