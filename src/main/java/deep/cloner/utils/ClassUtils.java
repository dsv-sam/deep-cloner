package deep.cloner.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.*;

public class ClassUtils {
    private static final int SINGLE_ELEMENT_ARRAY_SIZE = 1;
    private static final int FIRST_ELEMENT_INDEX = 0;

    public static <T> boolean checkClassIsCollection(Class<T> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    public static <T> boolean checkClassIsMap(Class<T> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    public static boolean checkClassIsWrapper(Class<?> clazz) {
        return Objects.equals(clazz.getSuperclass(), Number.class) ||
                clazz.equals(String.class) ||
                clazz.equals(Boolean.class);
    }

    private static <T> T getDefaultValue(Class<T> clazz) throws Exception {
        if (clazz.isPrimitive()) {
            return (T) Array.get(Array.newInstance(clazz, SINGLE_ELEMENT_ARRAY_SIZE), FIRST_ELEMENT_INDEX);
        }

        Constructor<?> noArgsConstructor = getNoArgsConstructor(clazz, false);
        if (noArgsConstructor != null) {
            return (T) noArgsConstructor.newInstance();
        }

        return null;
    }

    public static Constructor<?> getNoArgsConstructor(Class<?> clazz, boolean required) {
        Optional<Constructor<?>> constructorOptional = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .findFirst();
        if (constructorOptional.isPresent()) {
            Constructor<?> constructor = constructorOptional.get();
            constructor.setAccessible(true);

            return constructor;
        } else if (required) {
            throw new RuntimeException("No args constructor not found for class " + clazz);
        } else {
            return null;
        }
    }

    public static <T> T newInstance(Class<T> clazz) throws Exception {
        Constructor<?> noArgsConstructor = getNoArgsConstructor(clazz, false);
        if (noArgsConstructor != null) {
            return (T) noArgsConstructor.newInstance();
        }

        final Constructor<T> constructor = (Constructor<T>) Arrays.stream(clazz.getDeclaredConstructors())
                .findAny()
                .orElseThrow(() -> new RuntimeException("No contructors found for class " + clazz));
        constructor.setAccessible(true);

        final List<Object> constructorParameters = new ArrayList<>();
        for (Class<?> parameterType : constructor.getParameterTypes()) {
            constructorParameters.add(getDefaultValue(parameterType));
        }

        return constructor.newInstance(constructorParameters.toArray());
    }
}
