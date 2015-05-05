package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * This annotation is used in conjunction with &#64;CompositeComponent,
 * <p>&#64;CompositeChildren and &#64;CompositeLeaf to support 
 * <p>the implementation of the Composite Pattern.<p>
 * Used to mark the composite actor of the Composite Pattern. Must have a field annotated with &#64;CompositeChildren
 * 
 * 
 * <p><ul>
 * <li>&#64;CompositeComponent
 * <li>&#64;CompositeChildren
 * <li>&#64;CompositeLeaf
 * </ul><p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Composite {
	
}
