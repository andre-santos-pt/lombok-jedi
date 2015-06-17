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
	public @interface Children {
		 /**
			 * 
			 */
			String methodAddChildrenName() default "";
			/**
			 * 
			 * used to define the name of the getChildren method. The default value depents on the field annotated 
			 * with  &#64;CompositeChildren.
			 *  * <ul>
	 * <li>if the local field is a subclass of Collection.class, the default value is "getChildren".
	 * <li>if the local field is an object whose class is annotated with  &#64;CompositeComponent, the default value is "getSon"
	 * </ul>
			 * 
			 */
			String methodGetChildrenName() default "";
			
	}
	
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.SOURCE)
	/**
	 * This annotation is used in conjunction with @CompositeComponent, @CompositeChildren and @Composite to support
	 * the implementation of the Composite Pattern.
	 * Used to mark the component actor of the Composite Pattern. Can only be used on Abstract class.
	 * The class annotated with this will generate the following:
	 * -a public constructor based on a manually done constructor, and add an 
	 * argument parent of of a type annotated with @Composite.
	 * -create a getParent method, that returns the parent(class annotated with @composite).
	 * 
	 * Used with:
	 * -CompositeComponent
	 * -CompositeChildren
	 * -Composite
	 *
	 * @param methodName the custom name for the method that retrieves the parent. Default value is "getParent()"
	 * @param fieldName the custom name for the field that stores the parent. Default value is "parent"

	 */
	public @interface Component {
		String fieldName() default "";
		String methodName() default "";
	}
	
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.SOURCE)
	/**
	 * This annotation is used in conjunction with @CompositeComponent, @CompositeChildren and @Composite to support
	 * the implementation of the Composite Pattern.
	 * Used to mark the leaf actor of the Composite Pattern. Checks for a public constructor and injects a call to 
	 * the leaf's parent to add this class as one of his childs.
	 * 
	 * 
	 * Used with:
	 * -CompositeComponent
	 * -CompositeChildren
	 * -Composite
	 *
	 */
	public @interface Leaf {
		
	}
}
