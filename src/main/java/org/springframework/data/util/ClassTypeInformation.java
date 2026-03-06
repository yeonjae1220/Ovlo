package org.springframework.data.util;

/**
 * Compatibility stub: ClassTypeInformation moved to org.springframework.data.core in Spring Data 4.x.
 * SpringDoc 2.8.6 (QuerydslPredicateOperationCustomizer) still references the old package.
 * Safe to remove when upgrading to SpringDoc 3.x.
 */
@SuppressWarnings("all")
public class ClassTypeInformation<T> implements TypeInformation<T> {

    private final Class<T> type;

    private ClassTypeInformation(Class<T> type) {
        this.type = type;
    }

    public static <T> ClassTypeInformation<T> from(Class<T> type) {
        return new ClassTypeInformation<>(type);
    }

    @Override
    public Class<T> getType() {
        return type;
    }
}
