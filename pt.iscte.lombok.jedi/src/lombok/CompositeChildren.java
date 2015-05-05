package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with &#64;CompositeLeaf, &#64;CompositeComponent and &#64;Composite to support
 *<p> the implementation of the Composite Pattern.
 *<p>Used to mark the composite's children. 
 *<p>The field annotated must be an object or derive from the class Collections.
 *<p>The type argument of the field must be annotated with Composite.
 * 
 * The class annotated with a field annotated with this will generate the following:
 * <ul>
 * <li>a public method add, that adds a children to this composite.
 * <li>a public method get children, that returns the children of the composite class.
 * </ul>
 * Used with:
 * <ul>
 * <li>CompositeLeaf
 * <li>CompositeComponent
 * <li>Composite
 *</ul>
 */
public @interface CompositeChildren {
	
}
