package one.edee.oss.proxycian.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static one.edee.oss.proxycian.utils.GenericsUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test verifying {@link GenericsUtils}
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2023
 */
class GenericsUtilsTest {

    @Test
    void shouldReturnGenericTypeFromCollection() throws NoSuchMethodException {
        assertEquals(
                String.class,
                getGenericTypeFromCollection(TestInterface.class, TestInterface.class.getMethod("getStrings").getGenericReturnType())
        );
        assertEquals(
            String.class,
            getGenericTypeFromCollection(TestInterface.class, TestInterface.class.getMethod("getData").getGenericReturnType())
        );
    }

    @Test
    void shouldReturnMultipleGenericTypesFromCollection() throws NoSuchMethodException {
        assertEquals(
                Collections.singletonList(new GenericBundle(List.class, new GenericBundle[]{new GenericBundle(String.class)})),
                getNestedMethodReturnTypes(TestInterface.class, TestInterface.class.getMethod("getOptionalStrings"))
        );
        assertEquals(
                Collections.singletonList(new GenericBundle(List.class, new GenericBundle[]{new GenericBundle(String.class)})),
                getNestedMethodReturnTypes(TestInterface.class, TestInterface.class.getMethod("getOptionalData"))
        );
    }

    @Test
    void shouldRecognizeReturnType() throws NoSuchMethodException {
        assertEquals(
                String.class,
                getMethodReturnType(TestInterface.class, TestInterface.class.getMethod("getDataItem"))
        );
    }

    interface AbstractGenericInterface<T> {

        T getDataItem();

        List<T> getData();

        Optional<List<T>> getOptionalData();

    }

    interface TestInterface extends AbstractGenericInterface<String> {
        List<String> getStrings();

        Optional<List<String>> getOptionalStrings();

    }

}