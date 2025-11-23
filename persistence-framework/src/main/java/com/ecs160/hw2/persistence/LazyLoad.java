package com.ecs160.hw2.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation with a single argument that specifies which field is lazy loaded.
 * Assume that a method can only have one lazy-loaded field.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyLoad {
    String field();
}

