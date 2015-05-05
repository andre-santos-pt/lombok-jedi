package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with &#64;VisitableType and &#64;VisitableChildren to support<p>
 * the implementation of the Visitor Pattern.<p>
 * Used to mark the visitable nodes; these classes must be compatible with a single <p>
 * type marked with &#64;VisitableType.<p>
 * The class annotated will generate the following:<p>
 * <ul>
 * <li>accept method that will propagate the visit method defined in the &#64;VisitableType class, through its children.
 * <p>If it has no children, it will only contain visitor.accept(this).<p>
 * </ul>
 * Used with:
 * <ul>
 * <li>VisitableType
 * <li>VisitableChildren
 *</ul>
 */
public @interface VisitableNode {
	
}