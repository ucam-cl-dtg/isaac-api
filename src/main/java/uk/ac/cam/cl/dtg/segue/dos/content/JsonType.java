package uk.ac.cam.cl.dtg.segue.dos.content;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to allow mapping of Domain Objects to 
 * string type identifiers.
 * 
 * e.g. jsonType("video").
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonType {

	/**
	 * gets the string value associated with the jsonType annotation. 
	 * 
	 * default is "string"
	 */
	String value() default "string";
}
