package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
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
public @interface FlyweightObject {
}
