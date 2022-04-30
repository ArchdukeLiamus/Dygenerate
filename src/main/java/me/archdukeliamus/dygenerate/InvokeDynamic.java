package me.archdukeliamus.dygenerate;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks methods that are to be used as invokedynamic surrogates. The value string provides information about the
 * bootstrap method.
 *
 */
@Retention(CLASS)
@Target(METHOD)
public @interface InvokeDynamic {
	String value();
}
