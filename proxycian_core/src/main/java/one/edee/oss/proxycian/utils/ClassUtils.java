package one.edee.oss.proxycian.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import one.edee.oss.proxycian.cache.ConstructorCacheKey;
import one.edee.oss.proxycian.exception.SuperConstructorNotFoundException;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Class contains utility method for work with the class reflection.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassUtils {

	/**
	 * Method attempts to find appropriate public/protected constructor in specified `aClass` that matches the array
	 * of passed `constructorArgs`. The matching is performed on arguments "compatibility" - i.e. whether the passed
	 * arguments can be assigned to required constructor arguments.
	 *
	 * @param aClass the class to search constructor on
	 * @param constructorArgs the constructor parameters that must "match"
	 * @return found compatible constructor
	 * @throws IllegalArgumentException when no appropriate constructor is found
	 */
	@Nonnull
	public static Constructor<?> findConstructor(@Nonnull ConstructorCacheKey aClass, @Nonnull Class<?>[] constructorArgs) {
		final Class<?> ownerClass = aClass.getClazz();
		try {
			final Constructor<?> constructor = ownerClass.getDeclaredConstructor(aClass.getArgumentTypes());
			if (Modifier.isPrivate(constructor.getModifiers())) {
				throw new NoSuchMethodException();
			}
			if (Modifier.isProtected(constructor.getModifiers())) {
				constructor.setAccessible(true);
			}
			return constructor;
		} catch (NoSuchMethodException e) {
			// attempt to find constructor by compatible arguments
			nextConstructor: for (Constructor<?> declaredConstructor : ownerClass.getDeclaredConstructors()) {
				if (declaredConstructor.getParameterTypes().length == constructorArgs.length) {
					final Class<?>[] requiredConstructorArgs = declaredConstructor.getParameterTypes();
					for (int i = 0; i < requiredConstructorArgs.length; i++) {
						final Class<?> requiredConstructorArg = requiredConstructorArgs[i];
						final Class<?> providedConstructorArg = constructorArgs[i];
						if (!requiredConstructorArg.isAssignableFrom(providedConstructorArg)) {
							continue nextConstructor;
						}
					}
					return declaredConstructor;
				}
			}

			throw new SuperConstructorNotFoundException(
				"What the heck? Can't find public/protected constructor with arguments " +
					Arrays.stream(constructorArgs).map(Class::toGenericString).collect(Collectors.joining(", ")) +
					" on abstract class: " + e.getMessage(), e
			);
		}
	}

}
