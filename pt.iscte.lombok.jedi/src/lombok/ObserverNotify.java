package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 
 * Used to support the annotation @Observable . 
 * It marks the the parameters and fields that will be notified.
 * It also turns the parameters and fields to final.
 * 
 * @param value custom name of the field or argument in the interface.
 *
 */
@Target({ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface ObserverNotify {
	String value() default "";
}
