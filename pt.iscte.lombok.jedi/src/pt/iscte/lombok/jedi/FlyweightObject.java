package pt.iscte.lombok.jedi;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
public @interface FlyweightObject {
	boolean intrinsicState() default true;
}
