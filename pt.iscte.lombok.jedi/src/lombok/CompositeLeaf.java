package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with @CompositeComponent, @CompositeChildren and @Composite to support
 * the implementation of the Composite Pattern.
 * Used to mark the leaf actor of the Composite Pattern. Checks for a public constructor and injects a call to 
 * the leaf's parent to add this class as one of his childs.
 * 
 * 
 * Used with:
 * -CompositeComponent
 * -CompositeChildren
 * -Composite
 *
 */
public @interface CompositeLeaf {
	
}
