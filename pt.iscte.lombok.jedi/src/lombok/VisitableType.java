package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
/**
 * This annotation is used in conjunction with @VisitableNode and @VisitableChildren to support
 * the implementation of the Visitor Pattern.
 * Used to mark the type of the visitable nodes, which is typically an interface or abstract class.
 *The Interface/Abstract-class annotated it will receive the following components:
 * -Visitor Interface containing a visit method for each of the non-abstract visitable nodes. It 
 * will also contain an abstract accept method for the VisitableNodes to implement.
 * 
 * Used with:
 * -VisitableNode
 * -VisitableChildren
 */
public @interface VisitableType {
	String visitorTypeName() default "Visitor";
	String visitorMethodName() default "visit";
	String acceptMethodName() default "accept";
}