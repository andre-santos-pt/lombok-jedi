package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation is used in conjunction with &#64;Observable to support
 * the implementation of the Observer Pattern.
 * The purpose of this annotation is to mark the fields that are to be notified.
 * 
 * The field/parameter annotated with it, will generate the following:
 *<p><ul>
 *<li>a final modifier, to guarantee integrity of its values.
 *</ul>
 *
 *Used with:
 * <p><ul>
 * <li>&#64;Observable
 * </ul><p>
 */
@Target({ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface ObserverNotify {
	String value() default "";
}
