package org.springframework.data.util;

/**
 * Compatibility stub: CastUtils moved/removed in Spring Data 4.x.
 * SpringDoc 2.8.6 (QuerydslPredicateOperationCustomizer) still references the old package.
 * Safe to remove when upgrading to SpringDoc 3.x.
 */
@SuppressWarnings("all")
public class CastUtils {

    private CastUtils() {}

    public static <T> T cast(Object object) {
        return (T) object;
    }
}
