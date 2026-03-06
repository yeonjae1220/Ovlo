package org.springframework.data.util;

/**
 * Compatibility stub: TypeInformation moved to org.springframework.data.core in Spring Data 4.x.
 * SpringDoc 2.8.6 (QuerydslPredicateOperationCustomizer) still references the old package.
 * Safe to remove when upgrading to SpringDoc 3.x.
 */
@SuppressWarnings("all")
public interface TypeInformation<S> {
    Class<S> getType();
}
