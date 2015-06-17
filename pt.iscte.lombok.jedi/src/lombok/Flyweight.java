package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 
 * This annotation is used in conjunction with &#64;FlyweightObject to support
 * the implementation of the Flyweight Pattern.
 * This annotation also turns the object into a value object, by overriding the equals and hashcode method.
 * The class annotated with it, will generate the following:
 *<p><ul>
 *<li>a private constructor with the fields annotated with  &#64;FlyweightObject as arguments.
 *<li>a getMethod for each of the fields annotated with  &#64;FlyweightObject.
 *<li>an overrided hashcode and equals for value Object.
 *<li>a getInstance method that returns the Flyweight if exists, otherwise creates it, and then returns it( depends on the method type).
 *</ul>
 * Validations:
 * <p><ul>
 * <li>Class will not be able to contain public constructors.
 * <li>Can not be used on Interfaces and abstract classes.
 * <li>Need to contain at least one  &#64;FlyweightObject
 * </ul><p>
 *  
 *Used with:
 * <p><ul>
 * <li>&#64;FlyweightObject
 * </ul><p>
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Flyweight {
	/**
	 *boolean that defines if the factory is generated or not.
	 */
	boolean factory() default true;
	/**
	 * defines the type of factory to be generated.
	 *<p><ul>
 * <li>factory Type 0:	The flyweight will be store in a map, whose key is itself. <p>
 *			It requires creating a temporary object( with the arguments received), 
 *			to check if it already exists.
 *<li>factory Type 1:	The flyweight will be stored in a map of maps, whose keys are the flyweightObjects.<p>
 *			In this case no object needs to be created to verify if the object exists.
 * </ul><p> 
*/
	int factoryType() default 0;

	/**
	 * This annotation is used in conjunction with &#64;Flyweight to support
	 * the implementation of the Flyweight Pattern.
	 * The purpose of this annotation is to mark the fields that compose the flyweight object.
	 * 
	 * The field/argument annotated with it, will generate the following:
	 *<p><ul>
	 *<li>a final modifier, to guarantee integrity of its values.
	 *</ul>
	 *
	 *Used with:
	 * <p><ul>
	 * <li>&#64;Flyweight
	 * </ul><p>
	 */
	@Target({ElementType.FIELD})
	public @interface Object {
	}
}


