package pt.iscte.lombok.jedi;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
public @interface Observable {
	 boolean after() default true;
	 Class<?> type() default void.class;
	 String typeName() default "";
	 /**
	  * blabl
	  * @return
	  */
	 String operation() default "";
}
