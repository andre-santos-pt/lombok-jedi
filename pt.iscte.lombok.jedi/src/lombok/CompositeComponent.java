package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with @CompositeComponent, @CompositeChildren and @Composite to support
 * the implementation of the Composite Pattern.
 * Used to mark the component actor of the Composite Pattern. Can only be used on Abstract class.
 * The class annotated with this will generate the following:
 * -a public constructor based on a manually done constructor, and add an 
 * argument parent of of a type annotated with @Composite.
 * -create a getParent method, that returns the parent(class annotated with @composite).
 * 
 * Used with:
 * -CompositeComponent
 * -CompositeChildren
 * -Composite
 *
 * @param methodName the custom name for the method that retrieves the Singleton's instance. Default value is "getInstance()"
 * @param fieldName the custom name for the field that stores the Singleton's instance. Default value is "_instance"

 */
public @interface CompositeComponent {
	String fieldName() default "";
	String methodName() default "";
}
