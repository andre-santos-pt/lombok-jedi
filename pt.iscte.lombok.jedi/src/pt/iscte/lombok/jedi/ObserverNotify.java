package pt.iscte.lombok.jedi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


@Target({ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface ObserverNotify {
	String name() default "";
}
