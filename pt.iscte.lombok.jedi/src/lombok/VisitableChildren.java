package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with @VisitableType and @VisitableNode to support
 * the implementation of the Visitor Pattern.
 * Used to mark fields of visitable nodes (@VisitableNode), so that it's
 * objects are considered for descending into the node tree. 
 * Fields should be either reference types annotated with @VisitableNode or an iterable 
 * type (compatible with java.util.Iterable) whose elements are annotated with @VisitableNode
 * 
 *Used with:
 *-VisitableNode
 *-VisitableType
 */
public @interface VisitableChildren {
	
}