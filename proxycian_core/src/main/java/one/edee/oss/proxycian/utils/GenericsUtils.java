package one.edee.oss.proxycian.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public static Class<?> getMethodReturnType(Class<?> mainClass, Method methodInvocation) {
		return getMethodReturnType(mainClass, methodInvocation, null);
	}

	/**
	 * This method will resolve return type of the method signature. Generic will be translated to the
	 * simple type declaration.
	 */
	public static Class<?> getMethodReturnType(Class<?> mainClass, Method method, List<Class<?>> alreadyResolvedClasses) {
		Type genericReturnType = method.getGenericReturnType();
		Class<?> returnType = method.getReturnType();
		if (genericReturnType == returnType) {
			return returnType;
		} else {
			Class<?> result = null;
			if (!(genericReturnType instanceof Class)) {
				if (mainClass != null) {
					List<Class<?>> resolvedTypes = getGenericType(mainClass, genericReturnType);
					if (!resolvedTypes.isEmpty()) {
						result = resolvedTypes.get(0);
					}
				}
				if (result == null && alreadyResolvedClasses != null && !alreadyResolvedClasses.isEmpty()) {
					result = alreadyResolvedClasses.get(0);
				}
			}
			if (result == null) {
				result = returnType;
			}
			return result;
		}
	}

	/**
	 * Returns generic class of the return type if declared.
	 */
	public static Class<?> getGenericTypeFromCollection(Class<?> mainClass, Type returnType) {
		return getGenericTypeFromCollection(mainClass, returnType, null);
	}

	/**
	 * Returns generic class of the return type if declared.
	 */
	public static Class<?> getGenericTypeFromCollection(Class<?> mainClass, Type returnType, List<Class<?>> alreadyResolvedTypes) {
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
					List<Class<?>> arguments;
					if (alreadyResolvedTypes != null && !alreadyResolvedTypes.isEmpty()) {
						arguments = alreadyResolvedTypes;
					} else if (mainClass != null) {
						arguments = getGenericType(mainClass, typeArgument);
					} else if (typeArgument instanceof WildcardType) {
						Type[] upperBounds = ((WildcardType) typeArgument).getUpperBounds();
						if (upperBounds != null && upperBounds.length == 1) {
							arguments = Collections.singletonList(getClass(upperBounds[0]));
						} else {
							throw new IllegalArgumentException("Cannot handle generic type: " + returnType.toString());
						}
					} else {
						throw new IllegalArgumentException("Cannot handle generic type: " + returnType.toString());
					}
					if (!arguments.isEmpty() && arguments.get(0) != null) {
						return arguments.get(0);
					} else {
						try {
							return (Class<?>) ((TypeVariable<?>) typeArgument).getBounds()[0];
						} catch (Exception ex) {
							throw new IllegalArgumentException("Cannot handle generic type: " + returnType.toString(), ex);
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
	public static List<Class<?>> getGenericType(Class<?> childClass, Type searchedType) {
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
		List<Class<?>> typeArgumentsAsClasses = new ArrayList<>();
		// resolve types by chasing down type variables.
		for (Type baseType : actualTypeArguments) {
			while (resolvedTypes.containsKey(baseType)) {
				baseType = resolvedTypes.get(baseType);
			}
			typeArgumentsAsClasses.add(getClass(baseType));
		}
		return typeArgumentsAsClasses;
	}

	/**
	 * Get the underlying class for a type, or null if the type is a variable type.
	 *
	 * @param type the type
	 * @return the underlying class
	 */
	@SuppressWarnings({"TailRecursion"})
	public static Class<?> getClass(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			return getClass(((ParameterizedType) type).getRawType());
		} else if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) type).getGenericComponentType();
			Class<?> componentClass = getClass(componentType);
			if (componentClass != null) {
				return Array.newInstance(componentClass, 0).getClass();
			} else {
				return null;
			}
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

}

