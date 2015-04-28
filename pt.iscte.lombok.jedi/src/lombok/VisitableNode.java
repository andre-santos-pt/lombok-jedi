package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with @VisitableType and @VisitableChildren to support
 * the implementation of the Visitor Pattern.
 * Used to mark the visitable nodes; these classes must be compatible with a single 
 * type marked with @VisitableType
 * The class annotated will generate the following:
 * - accept method that will propagate the visit method defined in the @VisitableType class, through its children.
 * If it has no children, it will only contain visitor.accept(this).
 * 
 * Used with:
 * -VisitableType
 * -VisitableChildren
 */
public @interface VisitableNode {
	
}