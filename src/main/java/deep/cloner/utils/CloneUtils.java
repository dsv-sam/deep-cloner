package deep.cloner.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CloneUtils {
    private static final String WRAPPER_CLASS_VALUE_FIELD_NAME = "value";

    public static <T> T cloneObject(T sourceObject) {
        return doCloneObject(sourceObject, null);
    }

    private static <T> T doCloneObject(T sourceObject, IdentityHashMap<Object, Object> sourcesClonesMap) {
        if (sourceObject == null) {
            return null;
        }

        if (sourcesClonesMap == null) {
            sourcesClonesMap = new IdentityHashMap<>();
        } else {
            T alreadyClonedObject = (T) sourcesClonesMap.get(sourceObject);
            if (alreadyClonedObject != null) {
                return alreadyClonedObject;
            }
        }

        T clonedObject;
        try {
            Class<?> sourceObjectClass = sourceObject.getClass();
            if (ClassUtils.checkClassIsWrapper(sourceObjectClass)) {
                return cloneWrapper(sourceObject, sourceObjectClass);
            }

            clonedObject = (T) ClassUtils.newInstance(sourceObjectClass);

            while (sourceObjectClass != null) {
                if (sourceObjectClass.isPrimitive()) {
                    clonedObject = sourceObject;

                    return clonedObject;
                } else if (ClassUtils.checkClassIsWrapper(sourceObjectClass)) {
                    return cloneWrapper(sourceObject, sourceObjectClass);
                }

                cloneFields(sourceObject, sourceObjectClass, clonedObject, sourcesClonesMap);
                
                sourceObjectClass = sourceObjectClass.getSuperclass();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return clonedObject;
    }

    private static <T> void cloneFields(T sourceObject, Class<?> sourceObjectClass, T clonedObject,
                                        IdentityHashMap<Object, Object> sourcesClonesMap) throws Exception {
        for (Field field : sourceObjectClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            Object sourceFieldValue = field.get(sourceObject);
            if (sourceFieldValue == null) {
                continue;
            }

            Class<?> sourceFieldType = field.getType();

            if (sourceFieldType.isPrimitive() || sourceFieldType.isEnum()) {
                field.set(clonedObject, sourceFieldValue);
            } else if (ClassUtils.checkClassIsWrapper(sourceFieldType)) {
                field.set(clonedObject, cloneWrapper(sourceFieldValue, sourceFieldType));
            } else {
                Object clonedFieldValue;
                if (sourceFieldValue == sourceObject) {
                    clonedFieldValue = clonedObject;
                } else {
                    Class<?> fieldValueClass = sourceFieldValue.getClass();
                    if (ClassUtils.checkClassIsCollection(fieldValueClass)) {
                        Collection clonedCollection = cloneCollection((Collection) sourceFieldValue, sourcesClonesMap);
                        clonedFieldValue = clonedCollection;
                    } else if (ClassUtils.checkClassIsMap(fieldValueClass)) {
                        clonedFieldValue = cloneMap((Map) sourceFieldValue, sourcesClonesMap);
                    } else if (fieldValueClass.isArray()) {
                        clonedFieldValue = cloneArray(sourceFieldValue, sourcesClonesMap);
                    } else {
                        clonedFieldValue = doCloneObject(sourceFieldValue, sourcesClonesMap);
                    }
                }

                field.set(clonedObject,
                        saveAndReturnClonedObject(
                                sourceFieldValue,
                                clonedFieldValue,
                                sourcesClonesMap)
                );
            }
        }
    }

    private static <T> T cloneWrapper(T sourceObject, Class<?> sourceObjectClass) throws Exception {
        Field valueField = sourceObjectClass.getDeclaredField(WRAPPER_CLASS_VALUE_FIELD_NAME);
        valueField.setAccessible(true);
        Class<?> valueFieldClass = valueField.getType();
        Constructor<?> boxingConstructor = sourceObjectClass.getConstructor(valueFieldClass);
        T clonedObject = (T) boxingConstructor.newInstance(valueField.get(sourceObject));

        return clonedObject;
    }

    private static <T> Collection<T> cloneCollection(Collection<T> sourceCollection,
                                                     IdentityHashMap<Object, Object> sourcesClonesMap) {
        Class<? extends Collection> sourceCollectionClass = sourceCollection.getClass();
        Collection<T> clonedCollection = (Collection<T>) sourceCollection.stream()
                .map((T sourceObject) -> doCloneObject(sourceObject, sourcesClonesMap))
                .collect(
                        Collectors.toCollection(() -> {
                            try {
                                return sourceCollectionClass.cast(
                                        ClassUtils.newInstance(sourceCollectionClass)
                                );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                );

        return clonedCollection;
    }

    private static <K, V> Map<K, V> cloneMap(Map<K, V> sourceMap,
                                             IdentityHashMap<Object, Object> sourcesClonesMap) throws Exception {
        Class<? extends Map> sourceMapClass = sourceMap.getClass();
        Map<K, V> clonedMap = sourceMapClass.cast(
                ClassUtils.getNoArgsConstructor(sourceMapClass, true).newInstance()
        );

        sourceMap.forEach((key, value) ->
                clonedMap.put(
                        doCloneObject(key, sourcesClonesMap),
                        doCloneObject(value, sourcesClonesMap)
                ));

        return clonedMap;
    }

    private static <T> Object cloneArray(T sourceArray, IdentityHashMap<Object, Object> sourcesClonesMap) {
        int arrayLength = Array.getLength(sourceArray);
        Class<?> componentType = sourceArray.getClass().getComponentType();
        T clonedArray = (T) Array.newInstance(componentType, arrayLength);

        for (int i = 0; i < arrayLength; i++) {
            Array.set(clonedArray, i, doCloneObject(Array.get(sourceArray, i), sourcesClonesMap));
        }

        return clonedArray;
    }

    private static <T> T saveAndReturnClonedObject(T sourceObject, T clonedObject,
                                                   IdentityHashMap<Object, Object> objectsClonesMap) {
        objectsClonesMap.put(sourceObject, clonedObject);
        return clonedObject;
    }
}
