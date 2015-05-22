package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 *
 * Used to give extra behavior to the class annotated with it, to support the implementation of the Observer Pattern.
 *The Class containing the annotated method will be injected with the following:
 *<p><ul>
 *<li>local list that will store the interfaces that will be notified of the event on the method.
 *<li>a method subscribeTo<method_name> that adds interfaces to that field.
 *<li>a method unsubscribeTo<method_name> that removes interfaces from that field.
 *<li>if no interface is specified, an interface is generated, with a method whose arguments are the fields and arguments<p>
 * annotated with &#64;ObserverNotify and the class itself.
 *</ul>
 *
 *The method annotated with it will be injected with the following:
 *<ul>
 *<li>a notification to the interfaces "subscribed" to the method with the fields and arguments annotated with &#64;ObserverNotify. <p>
 *The location of where this notification is done depends on the parameter after.
 *</ul>
 *
 *Used with:
 *<ul>
 *<li>ObserverNotify
 *</ul>
 */
@Target({ElementType.METHOD})
public @interface Observable {
	/**
	 * 	 
	 * defines if the notification should be after(default), or before the method occur. 
	 * <p>If it's set to before, no fields can be annotated with &#64;ObserverNotify, only arguments.
	 */
	 boolean after() default true;
	 /**
	  * 
	  * parameter to define a custom interface, instead of letting the annotation create one. 
	  */
	 Class<?> type() default void.class;
	 /**
	  * the name of the method,from the custom interface, to be used for notification.
	  * <p>if the parameter operation is not defined, it is attempted to find a compatible method. In case of multiple compatible methods found, an error is thrown.
	  */
	 String operation() default "";
	 /**
	  * 
	  * the name of the interface generated, in case no custom interface is defined.
	  * <p>default value is <method_name>listener
	  */
	 String typeName() default "";
	 /**
	  * 
	  * the name of the field generated, in case no custom interface is defined.
	  * <p>default value is <method_name>listeners
	  * <p>if there is a field with the custom or default name, the field is not created.
	  */
	 String fieldName() default "";
	 /**
	  * 
	  * the name of method that adds a listener to the list.
	  * <p>default value is add<Interface_Name>
	  * <p>if there is a method with the custom or default name, the method won't be created.
	  */
	String addMethodName() default ""; 
	 /**
	  * 
	  * the name of method that removes	 a listener to the list.
	  * <p>default value is remove<Interface_Name>
	  * <p>if there is a method with the custom or default name, the method won't be created.
	  */
	String removeMethodName() default "";
	 
	
}
