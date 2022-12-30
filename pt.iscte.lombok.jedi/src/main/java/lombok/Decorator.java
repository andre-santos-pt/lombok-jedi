package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to give extra behavior to the class annotated with it to support the implementation of the Decorator Pattern to decorate interfaces.
 * The Interface annotated with it will generate the following:
 * <p><ul>
 * <li>An Inner-Abstract-Class that will forward every call to the interface annotated. 
 * 	<p>The Inner-class will contain:
 * <p><ul>
 * <li>local field that will store the annotated interface
 * <li>public constructor that will receive the interface
 * <li>all the public methods the interface has.
 * </ul><p> 
 * </ul><p>

 * 
 * Validations:
 * <p><ul>
 * <li>Only Interfaces can be annotated with &#64;Decorator
 * <ul><p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Decorator {
 /**
 *abstractClassName the custom name of the inner-class generated. The default name is <b>InterfaceName +"Decorator"</b>
 */
	String abstractClassName() default "";
/**
 *fieldName the custom name of the interface's instance. The default name is "_instance"
 */
	String fieldName() default "";
}
