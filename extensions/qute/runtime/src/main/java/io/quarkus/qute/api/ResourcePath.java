package io.quarkus.qute.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * Qualifies an injected template. The {@link #value()} is used to locate the template; it represents the path relative from
 * the base path.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface ResourcePath {

    /**
     * @return the path relative from the base path, must not be an empty string
     */
    @Nonbinding
    String value();

}