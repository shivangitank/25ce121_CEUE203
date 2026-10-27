package util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import model.annotation.MaxLength;
import model.annotation.Positive;

/**
 * Validates any object by reading the @Positive and @MaxLength annotations on
 * its fields at run time - the same idea validation frameworks use.
 */
public final class AnnotationValidator {

    private AnnotationValidator() {
        // only static methods
    }

    /**
     * Checks every instance field of obj, including fields inherited from
     * superclasses (getDeclaredFields() only returns one class's own fields,
     * so we walk up the hierarchy).
     *
     * @return one "field: message" entry per broken rule; an empty array if obj is valid
     */
    public static String[] validate(Object obj) {
        List<String> errors = new ArrayList<>();
        for (Class<?> type = obj.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isAnnotationPresent(Positive.class)) {
                    Object value = read(field, obj);
                    if (!(value instanceof Number number) || number.doubleValue() <= 0) {
                        errors.add(field.getName() + ": " + field.getAnnotation(Positive.class).message());
                    }
                }
                if (field.isAnnotationPresent(MaxLength.class)) {
                    int max = field.getAnnotation(MaxLength.class).value();
                    Object value = read(field, obj);
                    if (value instanceof String text && text.length() > max) {
                        errors.add(field.getName() + ": must be at most " + max
                                + " characters (was " + text.length() + ")");
                    }
                }
            }
        }
        return errors.toArray(new String[0]);
    }

    private static Object read(Field field, Object obj) {
        try {
            field.setAccessible(true);        // fields are private
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read field " + field.getName(), e);
        }
    }
}
