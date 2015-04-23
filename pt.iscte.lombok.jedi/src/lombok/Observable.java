package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 
 * @author duartecoelho
 *
 */
@Target({ElementType.METHOD})
public @interface Observable {
	 boolean after() default true;
	 
	 Class<?> type() default void.class;
	 String operation() default "";
	   /**
	  * blabl
	  * @return
	  */
	 String typeName() default "";
	
	
}
