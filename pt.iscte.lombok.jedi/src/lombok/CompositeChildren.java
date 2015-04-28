package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with @CompositeLeaf, @CompositeComponent and @Composite to support
 * the implementation of the Composite Pattern.
 * Used to mark the composite's children. 
 * The field annotated must be an object or derive from the class Collections.
 * The type argument of the field must be annotated with Composite.
 * 
 * The class annotated with a field annotated with this will generate the following:
 * -a public method get children, that returns the children of the composite class.
 * -a public method add, that adds a children to this composite.
 * 
 * Used with:
 * -CompositeLeaf
 * -CompositeComponent
 * -Composite
 *
 */
public @interface CompositeChildren {
	
}
