package lombok.javac.handlers;
import static lombok.javac.Javac.CTC_BOOLEAN;
import static lombok.javac.Javac.CTC_BOT;
import static lombok.javac.Javac.CTC_BYTE;
import static lombok.javac.Javac.CTC_CHAR;
import static lombok.javac.Javac.CTC_DOUBLE;
import static lombok.javac.Javac.CTC_FLOAT;
import static lombok.javac.Javac.CTC_INT;
import static lombok.javac.Javac.CTC_LESS_THAN;
import static lombok.javac.Javac.CTC_LONG;
import static lombok.javac.Javac.CTC_PLUS;
import static lombok.javac.Javac.CTC_SHORT;
import static lombok.javac.Javac.CTC_VOID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

import lombok.CompositeChildren;
import lombok.Observable;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.JavacTreeMaker.TypeTag;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;


@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(20)
public class HandleObservable extends JavacAnnotationHandler<Observable> {
	
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	

	@Override public void handle(AnnotationValues<Observable> annotation, JCAnnotation ast, JavacNode node) {
		ArrayList<String> customNotifiableNames = new ArrayList<String>();
		ListBuffer<JCVariableDecl> notifiable=new ListBuffer<JCVariableDecl>();
		JavacTreeMaker maker = node.up().getTreeMaker();	
		Observable annotationInstance=  annotation.getInstance();
		String annotationName=Observable.class.getName();
		boolean validationisafter = annotationInstance.after();
		//Class<?> customInterface = annotationInstance.type();
		Object obj = annotation.getActualExpression("type");
		String listenermethod=annotationInstance.operation();
		String listenername=annotationInstance.typeName();
		int operation=0;
		notificationValitations( maker, validationisafter, node.up(),notifiable,customNotifiableNames);
		if(obj!=null)
			if(!obj.equals(void.class)){
				operation=operation+1;
			}
		
		if(listenername.equals("") || listenername==null){
			listenername=listenername+firstToUpper(node.up().getName())+"Listener";
		}else{
			operation=operation+2;
		}
		JCMethodDecl annotatedmethod=(JCMethodDecl)node.up().get();
		JavacNode fieldNode;
		switch(operation){
		case 1:
			Symbol method=findMethod(node, notifiable, maker, obj);
			JCFieldAccess field = (JCFieldAccess) obj;
			Type type = field.selected.type;
			 fieldNode = creatinglocalfield(node, maker,type,annotationName);
			creatingListenerManagement(node, maker, fieldNode, type,annotationName);
			if(!listenermethod.equals("")&&listenermethod!=null){
				notifyingObservers(node, notifiable, maker, validationisafter, annotatedmethod, fieldNode,listenermethod,false);	
			}else{
				notifyingObservers(node, notifiable, maker, validationisafter, annotatedmethod, fieldNode,method.name.toString(),false);	
			}
			break;
		case 2:
			if(listenermethod.equals("")||listenermethod==null){
				listenermethod="notify";
			}
			
			JCClassDecl interf = creatingInterfaceListener(node, notifiable.toList(),customNotifiableNames,listenername, maker,listenermethod,annotationName);
			fieldNode = creatinglocalfield(node, maker,interf.type,annotationName);
			creatingListenerManagement(node, maker, fieldNode, interf.type,annotationName);
			notifyingObservers(node, notifiable, maker, validationisafter, annotatedmethod, fieldNode,listenermethod,true);	
			break;
		case 3: node.addError("Either you set a Listener, or choose a name for the creation of one.");
		break;
		}
		
		
		
		
		
	}
	private Symbol findMethod(JavacNode node, ListBuffer<JCVariableDecl> notifiable,
			JavacTreeMaker maker, Object obj) {
		Symbol method = null;
		int i=0;
		if(obj!=null)
		if(!obj.equals(void.class)){
			JCFieldAccess field = (JCFieldAccess) obj;
			Type interfacetype = field.selected.type;
			
			if(!interfacetype.tsym.isInterface()){
				node.addError("The value of the atribute type can only be an interface.");
			}else{
				for (Symbol member : interfacetype.tsym.getEnclosedElements()) {

					ExecutableElement exElem = (ExecutableElement) member;
					if (member.getKind().equals(ElementKind.METHOD) && exElem.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC)) {
						
						ListBuffer<JCVariableDecl> parameters = new ListBuffer<JCVariableDecl>();
						ListBuffer<JCExpression> arguments = new ListBuffer<JCExpression>();
						
						HandleWrapper.drillIntoMethod(node, maker, interfacetype, member, exElem, parameters, arguments);
						if(HandleWrapper.parametersEquals( parameters.toList(), notifiable.toList()))
						method= member;
						i++;
					}
				}
				
			}
			
		}
		if(i>1)
			node.addError("Multiple possible methods found.");
		
		if(method==null){
			String types = "";
			int pos=0 ;
		for (JCVariableDecl var : notifiable) {
			types=types+""+ var.vartype.toString();
			if(pos<notifiable.size()-2)
				types=types+",";
		}
			node.addError("The interface contains no method with the argument types ("+types+")");
		}
		return method;
	}
	private JavacNode creatinglocalfield(JavacNode methodNode, JavacTreeMaker maker, Type interftype,String annotationName) {
		JCExpression Arraytype=maker.TypeApply(handleArrayType(methodNode, maker, java.util.ArrayList.class),List.<JCExpression>of( maker.Ident(methodNode.toName(interftype.toString())) ));
		JCExpression listtype=maker.TypeApply(handleArrayType(methodNode, maker, java.util.List.class),List.<JCExpression>of( maker.Ident(methodNode.toName(interftype.toString())) ));
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression>nil(), Arraytype, NIL_EXPRESSION, null);
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.FINAL), methodNode.toName(methodNode.up().getName()+"Listeners"),listtype,fieldinit);
		JavacNode fieldNode = JediJavacUtil.injectField(methodNode.up().up(), field,annotationName);
		return fieldNode;
	}
	private String firstToUpper(String word) {
		String first="";
		String rest="";
		for(int i =0;i<word.length();i++){
			if(i==0){
				first=first+word.charAt(i);
				first=first.toUpperCase();
			}else{
				rest=rest+word.charAt(i);;
			}
		}
		
		
		return first+""+rest;
	}


	private void notifyingObservers(JavacNode methodNode, ListBuffer<JCVariableDecl> notifiable, JavacTreeMaker maker, boolean validationisafter, JCMethodDecl annotatedmethod, JavacNode fieldNode,String methodname,boolean argumentThis) {
		Name fieldName = JediJavacUtil.removePrefixFromField(fieldNode);
		ListBuffer<JCStatement> statements;
		statements = new ListBuffer<JCStatement>();
		ListBuffer<JCExpressionStatement> step = new ListBuffer<JCExpressionStatement>(); // FOR (_;_;X)
		ListBuffer<JCStatement> init = new ListBuffer<JCStatement>();  // For (X ; _ ; _) 
		ListBuffer<JCExpression>parameters= new ListBuffer<JCExpression>(); 
		//FOR (_ ; _.size(); _)
		JCExpression sizecall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), methodNode.up().toName("size")), List.<JCExpression>nil());
		// For (X ; _ ; _) 
		JCStatement i=maker.VarDef(maker.Modifiers(0),methodNode.toName("i") ,maker.TypeIdent(CTC_INT), maker.Literal(0)); // int i=0
		init.add(i);
		// For (_ ; X ; _) 
		JCExpression cond = maker.Binary(CTC_LESS_THAN, maker.Ident(((JCVariableDecl)i).name), sizecall); // i<fieldName.size()
		//
		//FOR (_; _ ; X)
		JCExpression stepleftside=maker.Ident(((JCVariableDecl)i).name);  // i
		JCExpression steprightside =maker.Binary(CTC_PLUS, maker.Ident(((JCVariableDecl)i).name),maker.Literal(1)); // i +1
		JCAssign stepExpressions= maker.Assign(stepleftside, steprightside);  // append(i) com (i+1)  	
		step.add(maker.Exec(stepExpressions));
		if(argumentThis)
		parameters.add(maker.Ident(methodNode.toName("this")));
		if(notifiable.size()>0){
		List<JCVariableDecl> notifiableparams =  notifiable.toList();
		for ( int k=0;k<notifiableparams.size();k++)
		parameters.add(maker.Ident(notifiableparams.get(k).getName()));
		
		}
		
		JCMethodInvocation getcall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), methodNode.up().toName("get")), List.<JCExpression>of(maker.Ident(((JCVariableDecl)i).name)));//fieldname.get(i))
		JCMethodInvocation notifycall=maker.Apply(NIL_EXPRESSION, maker.Select(getcall, methodNode.toName(methodname)), parameters.toList()); // previous/.notify()
		//add the complete statement
		statements.add(maker.Exec(notifycall));
		//define the body with all the statements
		JCBlock forbody = maker.Block(0, statements.toList());
		//
		
		//defining the loop
		JCForLoop forstat = maker.ForLoop(init.toList(), cond,step.toList() , forbody); //defining complete loop
		//
		if(validationisafter){
			annotatedmethod.body.stats=annotatedmethod.body.stats.append(forstat);	//after
		}else{
			annotatedmethod.body.stats=annotatedmethod.body.stats.prepend(forstat); //before
		}
	}



	private void notificationValitations(JavacTreeMaker maker, boolean validationisafter, JavacNode methodnode, ListBuffer<JCVariableDecl> notifiable, ArrayList<String> customnames) {
		List<JCAnnotation> notifyannotations;
		JCVariableDecl var;
				for (JavacNode nodel : methodnode.down()) {
	
			if(nodel.getKind().equals(lombok.core.AST.Kind.LOCAL) ||nodel.getKind().equals(lombok.core.AST.Kind.ARGUMENT) ){
				notifyannotations = JediJavacUtil.findAnnotations(nodel, Pattern.compile("^(?:ObserverNotify)$", Pattern.CASE_INSENSITIVE));	
				if(notifyannotations.size()>0){
					if(nodel.getKind().equals(lombok.core.AST.Kind.LOCAL) && !validationisafter){
						nodel.addError("Only arguments can be notified when notification is set to before");
					}else{
						var =(JCVariableDecl)nodel.get();
						String group;
						if(notifyannotations.get(0).args.size()>0){
							JCAssign x=(JCAssign)notifyannotations.get(0).args.get(0);
							 group =(String)((JCLiteral)x.rhs).value;	
						}else{
							 group=var.name.toString();
						}
						
						
						
						//if(map.get(group)==null){
							customnames.add(group);
							notifiable.add(maker.VarDef(maker.Modifiers(0),var.name ,(JCExpression)var.getType() , var.getInitializer()));
							
						//}
					
						
					}
				}
			}	
			
		}
				if(notifiable.size()==0)
					methodnode.addError("The method must have atleast one argument or field annotated with @ObserverNotify");
	}
	private JCClassDecl creatingInterfaceListener(JavacNode node, List<JCVariableDecl> notifiable, ArrayList<String> customNames,String listenerName, JavacTreeMaker maker,String methodname,String annotationName) {
		
		JCClassDecl interf = maker.ClassDef(maker.Modifiers(Flags.INTERFACE|Flags.PUBLIC), node.toName(firstToUpper(listenerName)), List.<JCTypeParameter>nil(),null, List.<JCExpression>nil(), List.<JCTree>nil());
		
		//injecting the interface
		JavacNode interfnode = JediJavacUtil.injectType(node.up().up(), interf,annotationName);
		JCClassDecl subject = (JCClassDecl)node.up().up().get();
		JCVariableDecl subjectdeclared = maker.VarDef(maker.Modifiers(0),node.toName("subject") ,maker.Ident(subject.name) , null);
		if(notifiable.size()>0){
			ListBuffer<JCVariableDecl> list = new ListBuffer<JCVariableDecl>();
			for (int i=0;i<notifiable.size();i++) {
				list.add(maker.VarDef(maker.Modifiers(0), interfnode.toName(firstToUpper(customNames.get(i))), notifiable.get(i).vartype, null));
			}
			//for (int i=0;i<map.size();i++) {
			//	list.add(maker.VarDef(maker.Modifiers(0), interfnode.toName("new"+firstToUpper(map.get(i).name.toString())), map.get(i).vartype, null));
			//}
		JCMethodDecl verify = JediJavacUtil.recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(0), interfnode.toName(methodname) ,maker.TypeIdent(CTC_VOID),
				List.<JCTypeParameter>nil(),list.toList(), List.<JCExpression>nil(), null, null), node.up().get(), node.up().getContext());
		// injecting the method in the interface
		verify.params=verify.params.prepend(subjectdeclared);
		JediJavacUtil.injectMethod(interfnode, verify,annotationName);
		}
	
		return interf;
	}
	private void creatingListenerManagement(JavacNode node, JavacTreeMaker maker, JavacNode fieldNode, Type interf,String annotationName) {
		Name fieldName = JediJavacUtil.removePrefixFromField(fieldNode);
		ListBuffer<JCVariableDecl> args=new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> params = new ListBuffer<JCExpression>() ;
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
		//cleaning the args from previous action
		args = new ListBuffer<JCVariableDecl>();
		
		
		//the argument from the method addMethodListener()
				Name argname=node.toName("listener");
				args.add(maker.VarDef(maker.Modifiers(0),argname , maker.Ident(node.toName(interf.toString())), null));
				params.add(maker.Ident(argname));
				//
				// calling the method .add() from hashset
				
				JCMethodInvocation fieldmethodcall = maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), node.up().toName("add")), params.toList());
				statements.add(maker.Exec(fieldmethodcall)); //turns the .add call into a statement for the body
				
				JCBlock body = maker.Block(0, statements.toList());
				//defining and injecting the add method
				JCMethodDecl subscribe = JediJavacUtil.recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC), node.toName("subscribeTo"+firstToUpper(node.up().getName())) ,maker.TypeIdent(CTC_VOID),
						List.<JCTypeParameter>nil(),args.toList(), List.<JCExpression>nil(), body,null ), node.up().get(), node.getContext());
				JediJavacUtil.injectMethod(node.up().up(), subscribe,annotationName);
		//
		
		//new statements, but same parameters and arguments.
		statements = new ListBuffer<JCStatement>();
		args= new ListBuffer<JCVariableDecl>();
		params= new ListBuffer<JCExpression>();
		
		// calling the method .remove() from hashset
		args.add(maker.VarDef(maker.Modifiers(0),argname , maker.Ident(node.toName(interf.toString())), null));
		params.add(maker.Ident(argname));
		JCMethodInvocation fieldmethodcallremove = maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), node.up().toName("remove")), params.toList());
		statements.add(maker.Exec(fieldmethodcallremove)); //turns the .remove call into a statement for the body
		
		JCBlock removeMethodBody = maker.Block(0, statements.toList());
		//defining and injecting the remove method
		JCMethodDecl unsubscribe = JediJavacUtil.recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC), node.toName("unsubscribeFrom"+firstToUpper(node.up().getName())) ,maker.TypeIdent(CTC_VOID),
		List.<JCTypeParameter>nil(),args.toList(), List.<JCExpression>nil(), removeMethodBody,null ), node.up().get(), node.getContext());
		JediJavacUtil.injectMethod(node.up().up(), unsubscribe,annotationName);
		//
	}
	
	
	
	private JCExpression handleArrayType(JavacNode node, JavacTreeMaker maker, Class<?> clazz) {
		int n = 0;
		while(clazz.isArray()) {
			clazz = clazz.getComponentType();
			n++;
		}
		
		//		String typeName = clazz.isArray() ? clazz.getComponentType().getName() : ;
		JCExpression type = JediJavacUtil.genTypeRef(node.up(), clazz.getName());
		
		while(n > 0) {
			type = maker.TypeArray(type);
			n--;
		}
		
		//		if(clazz.isArray())
		//			type = maker.TypeArray(type);
		return type;
	}	
}


