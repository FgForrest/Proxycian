package one.edee.oss.proxycian.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;

/**
 * The utility that contains helper methods for working with arrays. Adds the missing functionality for
 * {@link java.util.Arrays} class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArrayUtils {

	/**
	 * This method will merge all passed arrays into one.
	 */
	public static <T> T[] mergeArrays(T[]... array) {
		if (array.length == 0) {
			return null;
		}
		int resultSize = 0;
		for (T[] configItem : array) {
			resultSize = resultSize + configItem.length;
		}
		int offset = 0;
		@SuppressWarnings({"unchecked"})
		T[] result = (T[]) Array.newInstance(array[0].getClass().getComponentType(), resultSize);
		for (T[] configItem : array) {
			System.arraycopy(configItem, 0, result, offset, configItem.length);
			offset = offset + configItem.length;
		}
		return result;
	}

}
