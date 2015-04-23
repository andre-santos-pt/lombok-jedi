package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * Used to give extra behavior to the class annotated with it to support the implementation of the Singleton Pattern.
 * The class annotated with it will be injected with the following:
 * -Local field that will store the class's sole instance
 * -Method that will retried the instance. If its the first time retrieving the instance, it will instantiate it.
 * -Private constructor as default
 * 
 * Validations:
 * -Class will not be able to contain public constructors
 * 
 * @param value the name to the method that retrieves the Singleton object. Default value is "getInstance()"
 */
public @interface Singleton {
	String value() default "";
}
