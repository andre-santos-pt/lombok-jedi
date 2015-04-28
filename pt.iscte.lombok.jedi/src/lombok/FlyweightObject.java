package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
/**
 * 
 * Used to support the @Flyweight annotation. It marks the fields the flyweight will have.
 * 
 *
 */
@Target({ElementType.FIELD})
public @interface FlyweightObject {
}
