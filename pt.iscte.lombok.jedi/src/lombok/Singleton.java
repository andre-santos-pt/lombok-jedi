package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to give extra behavior to the class annotated with it, to support the implementation of the Singleton Pattern.
 * <p>The class annotated with it will generate the following:
 *  * <p><ul>
   * <li>Local field( final and static) that will store the class's sole instance.
   * <li>t-Method that will retried the instance. If its the first time retrieving the instance, it will instantiate it.
   * <li>Private constructor as default
   * </ul><p>
 * <p>
 * Validations:
 * <p><ul>
 * <li>Class will not be able to contain public constructors.
 * <li>Can not be used on Interfaces and abstract classes.
 * </ul><p>
 *  
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Singleton {
	/**
	 * methodName the custom name for the method that retrieves the Singleton's instance. 
	 * Default value is "getInstance()"
	 */
	String methodName() default "";
	/**
	 * fieldName the custom name for the field that stores the Singleton's instance. 
	 * Default value is "_instance"
	 */
	String fieldName() default "";
}
