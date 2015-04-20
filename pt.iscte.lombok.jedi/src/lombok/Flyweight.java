package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface Flyweight {
	boolean factory() default true;
	int factoryType() default 0;
}
