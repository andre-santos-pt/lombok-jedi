package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with @CompositeComponent, @CompositeChildren and @CompositeLeaf to support
 * the implementation of the Composite Pattern.
 * Used to mark the composite actor of the Composite Pattern. Must have a field annotated with @CompositeChildren
 * 
 * 
 * Used with:
 * -CompositeComponent
 * -CompositeChildren
 * -CompositeLeaf
 *
 */
public @interface Composite {
	
}
