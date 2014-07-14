package uk.ac.cam.cl.dtg.segue.dos.content;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to allow mapping of Domain Objects to 
 * string type identifiers.
 * 
 * e.g. DTOClass(Content.class).
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DTOMapping {

	/**
	 * gets the DTO class that any Domain objects should be mapped to and from.
	 * 
	 * default is Object.class
	 */
	Class<?> value() default Object.class;
}
