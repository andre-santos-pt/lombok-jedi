package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
public @interface VisitableChildren {
	
}