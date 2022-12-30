package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Used to give extra behavior to the class annotated with it to support the implementation of a Decorator Pattern to decorate classes.
 * The Interface annotated with it will generate the following:

 * <p><ul>
 * <li>local field that will store the annotated interface
 * <li>public constructor that will receive the interface
 * <li>all the public methods, the class target has.
 * </ul><p>
 * Validations:
 * <p><ul>
 * <li>Only classes can be annotated with &#64;Wrapper
 * <ul><p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Wrapper {
	/**
	 *the class that is going to be wrapped.
	 */
	Class<?> classType();
	/**
	 *custom name of the interface's instance. The default name is "_instance"
	 */
	String fieldName() default "";
}
