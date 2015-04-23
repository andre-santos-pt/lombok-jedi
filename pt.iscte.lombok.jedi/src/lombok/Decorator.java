package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * Used to give extra behavior to the class annotated with it to support the implementation of the Decorator Pattern.
 * The Interface annotated with it will be injected with the following:
 * -An Inner-Abstract-Class that will forward every call to the interface annotated. 
 *		The Inner-Class will contain:
 *			-local field that will store the annotated interface
 *			-public constructor that will recieve the interface
 *			-all the public methods, the interface has.
 * 
 * Validations:
 * -Only Interfaces can be annotated with @Decorator
 * 
 *@param abstractClassName the custom name of the inner-class generated. The default name is <b>InterfaceName +"Decorator"</b>
 *@param fieldName the custom name of the interface's instance. The default name is "_instance"
 */
public @interface Decorator {
	String abstractClassName() default "";

	String fieldName() default "";
}
