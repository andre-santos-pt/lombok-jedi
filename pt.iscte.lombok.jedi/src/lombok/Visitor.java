package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * This annotation is used in conjunction with &#64;VisitableNode and &#64;VisitableChildren to support<p>
 * the implementation of the Visitor Pattern.<p>
 * Used to mark the type of the visitable nodes, which is typically an interface or abstract class.<p>
 * The Interface/Abstract-class annotated it will generate the following:<p>
 * <ul>
 * <li>Visitor Interface containing a visit method for each of the non-abstract visitable nodes. It 
 * <p> will also contain an abstract accept method for the VisitableNodes to implement.
 * </ul>
 * 
 * Used with:
 * <ul>
 * <li>VisitableNode
 * <li>VisitableChildren
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Visitor {
	String visitorTypeName() default "Visitor";
	String visitorMethodName() default "visit";
	String acceptMethodName() default "accept";
	
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
	public @interface Node {
		
	}
	
	@Target({ElementType.FIELD,ElementType.METHOD})
	@Retention(RetentionPolicy.SOURCE)
	/**
	 * This annotation is used in conjunction with &#64;VisitableType and &#64;VisitableNode to support<p>
	 * the implementation of the Visitor Pattern.<p>
	 *It can be used on a field or on a method:
	 *<ul>
	 *<li>Field:Used to mark fields of visitable nodes (@VisitableNode), so that its<p>
	 * objects are considered for descending into the node tree. <p>
	 * Fields should be either reference types annotated with &#64;VisitableNode or an iterable <p>
	 * type (compatible with java.util.Iterable) whose elements are annotated with &#64;VisitableNode
	 *<li>Method:
	 *</ul>
	 * 
	 * 
	 *Used with:
	 *<ul>
	 *<li>VisitableNode
	 *<li>VisitableType
	 *</ul>
	 */
	public @interface Children {
		
	}
}