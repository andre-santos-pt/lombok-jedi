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
public @interface VisitableType {
	String visitorTypeName() default "Visitor";
	String visitorMethodName() default "visit";
	String acceptMethodName() default "accept";
}