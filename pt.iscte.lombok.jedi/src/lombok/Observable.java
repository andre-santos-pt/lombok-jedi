package lombok;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 *
 * Used to give extra behavior to the class annotated with it, to support the implementation of the Observer Pattern.
 *The Class containing the annotated method will be injected with the following:
 *-local list that will store the interfaces that will be notified of the event on the method.
 *-a method subscribeTo<method_name> that adds interfaces to that field.
 *-a method unsubscribeTo<method_name> that removes interfaces from that field.
 *-if no interface is specified, an interface is generated, with a method whose arguments are the fields and arguments
 *annotated with @ObserverNotify and the class itself.
 *
 *The method annotated with it will be injected with the following:
 *-a notification to the interfaces "subscribed" to the method with the fields and arguments annotated with @ObserverNotify. 
 *The location of where this notification is done depends on the parameter after.
 *
 *@param after defines if the notification should be after(default), or before the method occur. If it's set to before, no fields can be annotated with @ObserverNotify, only arguments.
 *@param type define a custom interface, instead of letting the annotation create one. 
 *if the parameter operation is not defined, it is attempted to find a compatible method. In case of multiple compatible methods found, an error is thrown.
 *@param operation the name of the method,from the custom interface, to be used for notification.
 *@param typeName  the name of the interface generated in case no custom interface is defined.
 *
 *
 */
@Target({ElementType.METHOD})
public @interface Observable {
	 boolean after() default true;
	 Class<?> type() default void.class;
	 String operation() default "";
	 String typeName() default "";
	
	
}
