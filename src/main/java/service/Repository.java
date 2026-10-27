package service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Predicate;

import model.annotation.Id;

/**
 * A generic in-memory repository: one class that stores any type safely.
 * The key of each object is the value of its field marked with @Id, found by
 * reflection, so Repository<Account> and Repository<Customer> share this code.
 *
 * @param <T> the type of object stored
 */
public class Repository<T> {
    private final HashMap<String, T> items = new HashMap<>();

    /** Stores the entity under its @Id value, replacing any earlier entity with the same id. */
    public T save(T entity) {
        items.put(idOf(entity), entity);
        return entity;
    }

    /**
     * Saves several entities at once. @SafeVarargs promises that this method
     * only reads the T[] array, so the generic-array warning can be suppressed.
     */
    @SafeVarargs
    public final void saveAll(T... entities) {
        for (T entity : entities) {
            save(entity);
        }
    }

    /** Saves every entity in a collection of T or of any subtype of T. */
    public void saveAllFrom(Collection<? extends T> entities) {
        for (T entity : entities) {
            save(entity);
        }
    }

    /** @return the entity with that id, or null */
    public T findById(String id) {
        return items.get(id);
    }

    public boolean existsById(String id) {
        return items.containsKey(id);
    }

    public ArrayList<T> findAll() {
        return new ArrayList<>(items.values());
    }

    /** Entities matching a condition, e.g. findAll(a -> a.getBalance() > 5000). */
    public ArrayList<T> findAll(Predicate<? super T> condition) {
        ArrayList<T> result = new ArrayList<>();
        for (T item : items.values()) {
            if (condition.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /** @return true if something was removed */
    public boolean delete(String id) {
        return items.remove(id) != null;
    }

    public int count() {
        return items.size();
    }

    /** Average of any numeric array: Integer[], Long[], Double[] ... (N must be a Number). */
    public static <N extends Number> double average(N[] values) {
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (N value : values) {
            sum += value.doubleValue();
        }
        return sum / values.length;
    }

    /** Reads the value of the field marked @Id, looking in superclasses too. */
    static String idOf(Object entity) {
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    try {
                        field.setAccessible(true);
                        Object id = field.get(entity);
                        if (id == null) {
                            throw new IllegalArgumentException("@Id field " + field.getName() + " is null");
                        }
                        return id.toString();
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Cannot read @Id field " + field.getName(), e);
                    }
                }
            }
        }
        throw new IllegalArgumentException(entity.getClass().getSimpleName() + " has no field marked @Id");
    }
}
