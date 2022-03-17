package one.edee.oss.proxycian.utils;

import java.lang.reflect.Array;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
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
