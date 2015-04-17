package pt.iscte.lombok.jedi.javac.handlers;
import static lombok.core.handlers.HandlerUtil.NULLABLE_PATTERN;
import static lombok.javac.Javac.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Wrapper;
import lombok.Observable;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.JavacTreeMaker.TypeTag;
import lombok.javac.handlers.JavacHandlerUtil.FieldAccess;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.mangosdk.spi.ProviderFor;
import org.objectweb.asm.tree.FieldNode;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Visitor;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

/**
 * Handles the {@code lombok.Getter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleObservable extends JavacAnnotationHandler<Observable> {
	
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	public TypeTag handlePrimitiveType(String type){
		
		if(type.equals(int.class.getName())){
			return CTC_INT;
		}
		
		if(type.equals(double.class.getName())){
			return CTC_DOUBLE;
		}
		
		if(type.equals(float.class.getName())){
			return CTC_FLOAT;
		}
		
		if(type.equals(short.class.getName())){
			return CTC_SHORT;
		}
		
		if(type.equals(byte.class.getName())){
			return CTC_BYTE;
		}
		
		if(type.equals(long.class.getName())){
			return CTC_LONG;
		}
		
		if(type.equals(boolean.class.getName())){
			return CTC_BOOLEAN;
		}
		
		if(type.equals(char.class.getName())){
			return CTC_CHAR;
		}
		if(type.equals("void")){
			return CTC_VOID;
		}
		
		return null;
	}
public Object handleinit(String type){
		
	if(int.class.getName().contains(type)){
		return 0;
	}
	
	if(double.class.getName().contains(type)){
		return 0.0;
	}
	
	if(float.class.getName().contains(type)){
		return 0.0;
	}
	
	if(short.class.getName().contains(type)){
		return 0;
	}
	
	if(byte.class.getName().contains(type)){
		return 0;
	}
	
	if(long.class.getName().contains(type)){
		return 0;
	}
	
	if(boolean.class.getName().contains(type)){
		return false;
	}
	
	if(char.class.getName().contains(type)){
		return ' ';
	}
		
		return null;
	}

	@Override public void handle(AnnotationValues<Observable> annotation, JCAnnotation ast, JavacNode node) {
		ArrayList<String> list = new ArrayList<String>();
		HashMap<String,ListBuffer<JCVariableDecl>> map=new HashMap<String,ListBuffer<JCVariableDecl>>();
		JavacTreeMaker maker = node.up().getTreeMaker();	
		Observable annotationInstance=  annotation.getInstance();
		boolean validationisafter = annotationInstance.after();
		JCMethodDecl annotatedmethod=(JCMethodDecl)node.up().get();
		notificationValitations( maker, validationisafter, node.up(),map,list);
		for(int i=0;i<list.size();i++){
			
			JCClassDecl interf = creatingInterfaceListener(node, map.get(list.get(i)).toList(),list.get(i), maker);
			JavacNode fieldNode = creatinglocalfield(node, maker,interf,list.get(i));
			creatingListenerManagement(node, maker, fieldNode, interf);
			notifyingObservers(node, map.get(list.get(i)),list.get(i), maker, validationisafter, annotatedmethod, fieldNode, interf);	
		}
		
	}
	private JavacNode creatinglocalfield(JavacNode node, JavacTreeMaker maker, JCClassDecl interfdecl,String group) {
		JCExpression Arraytype=maker.TypeApply(handleArrayType(node, maker, java.util.ArrayList.class),List.<JCExpression>of( maker.Ident(interfdecl.name) ));
		JCExpression listtype=maker.TypeApply(handleArrayType(node, maker, java.util.List.class),List.<JCExpression>of( maker.Ident(interfdecl.name) ));
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression>nil(), Arraytype, NIL_EXPRESSION, null);
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.FINAL), node.toName(node.up().getName()+""+firstToUpper(group)+"Listeners"),listtype,fieldinit);
		JavacNode fieldNode = injectField(node.up().up(), field);
		return fieldNode;
	}
	private String firstToUpper(String group) {
		String first="";
		String rest="";
		for(int i =0;i<group.length();i++){
			if(i==0){
				first=first+group.charAt(i);
				first=first.toUpperCase();
			}else{
				rest=rest+group.charAt(i);;
			}
		}
		
		
		return first+""+rest;
	}


	private void notifyingObservers(JavacNode node, ListBuffer<JCVariableDecl> notifiable, String string, JavacTreeMaker maker, boolean validationisafter, JCMethodDecl annotatedmethod, JavacNode fieldNode, JCClassDecl interf) {
		Name fieldName = removePrefixFromField(fieldNode);
		ListBuffer<JCStatement> statements;
		statements = new ListBuffer<JCStatement>();
		ListBuffer<JCExpressionStatement> step = new ListBuffer<JCExpressionStatement>(); // FOR (_;_;X)
		ListBuffer<JCStatement> init = new ListBuffer<JCStatement>();  // For (X ; _ ; _) 
		ListBuffer<JCExpression>parameters= new ListBuffer<JCExpression>(); 
		//FOR (_ ; _.size(); _)
		JCExpression sizecall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), node.up().toName("size")), List.<JCExpression>nil());
		// For (X ; _ ; _) 
		JCStatement i=maker.VarDef(maker.Modifiers(0),node.toName("i") ,maker.TypeIdent(CTC_INT), maker.Literal(0)); // int i=0
		init.add(i);
		// For (_ ; X ; _) 
		JCExpression cond = maker.Binary(CTC_LESS_THAN, maker.Ident(((JCVariableDecl)i).name), sizecall); // i<fieldName.size()
		//
		//FOR (_; _ ; X)
		JCExpression stepleftside=maker.Ident(((JCVariableDecl)i).name);  // i
		JCExpression steprightside =maker.Binary(CTC_PLUS, maker.Ident(((JCVariableDecl)i).name),maker.Literal(1)); // i +1
		JCAssign stepExpressions= maker.Assign(stepleftside, steprightside);  // append(i) com (i+1)  	
		step.add(maker.Exec(stepExpressions));
		parameters.add(maker.Ident(node.toName("this")));
		if(notifiable.size()>0){
		List<JCVariableDecl> notifiableparams =  notifiable.toList();
		for ( int k=0;k<notifiableparams.size();k++)
		parameters.add(maker.Ident(notifiableparams.get(k).getName()));
		
		for ( int k=0;k<notifiableparams.size();k++){
			if(notifiableparams.get(k).getInitializer()!=null){
				parameters.add(notifiableparams.get(k).getInitializer());	
			}else{
				parameters.add(initializer(notifiableparams.get(k).vartype,maker));
			}
		}
			
			
		}
		
		JCMethodInvocation getcall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), node.up().toName("get")), List.<JCExpression>of(maker.Ident(((JCVariableDecl)i).name)));//fieldname.get(i))
		JCMethodInvocation notifycall=maker.Apply(NIL_EXPRESSION, maker.Select(getcall, node.toName("notify")), parameters.toList()); // previous/.notify()
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
	private JCExpression initializer(JCExpression vartype, JavacTreeMaker maker) {
		if(handleinit(vartype.toString()) == null){
			return maker.Literal(CTC_BOT, null);
		}else{
			return maker.Literal(handleinit(vartype.toString()));
		}
		
	}


	private void notificationValitations(JavacTreeMaker maker, boolean validationisafter, JavacNode methodnode, HashMap<String, ListBuffer<JCVariableDecl>> map, ArrayList<String> list) {
		List<JCAnnotation> notifyannotations;
		JCVariableDecl var;
				for (JavacNode nodel : methodnode.down()) {
	
			if(nodel.getKind().equals(lombok.core.AST.Kind.LOCAL) ||nodel.getKind().equals(lombok.core.AST.Kind.ARGUMENT) ){
				notifyannotations = findAnnotations(nodel, Pattern.compile("^(?:ObserverNotify)$", Pattern.CASE_INSENSITIVE));	
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
							 group=methodnode.getName();
						}
						
						
						
						if(map.get(group)==null){
							list.add(group);
							map.put(group, new ListBuffer<JCVariableDecl>());
							
						}
						map.get(group).add(maker.VarDef(maker.Modifiers(0),var.name ,(JCExpression)var.getType() , var.getInitializer()));
						
					}
				}else{
					methodnode.addError("The method must have atleast one @ObserverNotify field");
				}
			}	
		}
			
	}
	private JCClassDecl creatingInterfaceListener(JavacNode node, List<JCVariableDecl> map, String group, JavacTreeMaker maker) {
		JCClassDecl interf = maker.ClassDef(maker.Modifiers(Flags.INTERFACE|Flags.PUBLIC), node.toName(firstToUpper(group)+"Listener"), List.<JCTypeParameter>nil(),null, List.<JCExpression>nil(), List.<JCTree>nil());
		
		//injecting the interface
		JavacNode interfnode = injectType(node.up().up(), interf);
		//defining the interface's method
		//defining the methods arguments
		
		JCClassDecl subject = (JCClassDecl)node.up().up().get();
		JCVariableDecl subjectdeclared = maker.VarDef(maker.Modifiers(0),node.toName("subject") ,maker.Ident(subject.name) , null);
		//verify that sends the subject to the observer
		//JCMethodDecl verifywiththis = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(0), interfnode.toName("notify") ,maker.TypeIdent(CTC_VOID),
		//List.<JCTypeParameter>nil(),List.<JCVariableDecl>of(subjectdeclared), List.<JCExpression>nil(), null, null), node.up().get(), node.up().getContext());
		// injecting the method in the interface
		//injectMethod(interfnode, verifywiththis);
		//if it has @notifys it creates a notify() <-- with the fields annotated
		if(map.size()>0){
			ListBuffer<JCVariableDecl> list = new ListBuffer<JCVariableDecl>();
			for (int i=0;i<map.size();i++) {
				list.add(maker.VarDef(maker.Modifiers(0), interfnode.toName("old"+firstToUpper(map.get(i).name.toString())), map.get(i).vartype, null));
			}
			for (int i=0;i<map.size();i++) {
				list.add(maker.VarDef(maker.Modifiers(0), interfnode.toName("new"+firstToUpper(map.get(i).name.toString())), map.get(i).vartype, null));
			}
		JCMethodDecl verify = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(0), interfnode.toName("notify") ,maker.TypeIdent(CTC_VOID),
				List.<JCTypeParameter>nil(),list.toList(), List.<JCExpression>nil(), null, null), node.up().get(), node.up().getContext());
		// injecting the method in the interface
		verify.params=verify.params.prepend(subjectdeclared);
		injectMethod(interfnode, verify);
		}
	
		return interf;
	}
	private void creatingListenerManagement(JavacNode node, JavacTreeMaker maker, JavacNode fieldNode, JCClassDecl interf) {
		Name fieldName = removePrefixFromField(fieldNode);
		ListBuffer<JCVariableDecl> args=new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> params = new ListBuffer<JCExpression>() ;
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
		//cleaning the args from previous action
		args = new ListBuffer<JCVariableDecl>();
		
		
		//the argument from the method addMethodListener()
				Name argname=node.toName("listener");
				args.add(maker.VarDef(maker.Modifiers(0),argname , maker.Ident(interf.name), null));
				params.add(maker.Ident(argname));
				//
				// calling the method .add() from hashset
				
				JCMethodInvocation fieldmethodcall = maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), node.up().toName("add")), params.toList());
				statements.add(maker.Exec(fieldmethodcall)); //turns the .add call into a statement for the body
				
				JCBlock body = maker.Block(0, statements.toList());
				//defining and injecting the add method
				JCMethodDecl subscribe = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC), node.toName("subscribeTo"+node.up().getName()) ,maker.TypeIdent(CTC_VOID),
						List.<JCTypeParameter>nil(),args.toList(), List.<JCExpression>nil(), body,null ), node.up().get(), node.getContext());
				injectMethod(node.up().up(), subscribe);
		//
		
		//new statements, but same parameters and arguments.
		statements = new ListBuffer<JCStatement>();
		args= new ListBuffer<JCVariableDecl>();
		params= new ListBuffer<JCExpression>();
		
		// calling the method .remove() from hashset
		args.add(maker.VarDef(maker.Modifiers(0),argname , maker.Ident(interf.name), null));
		params.add(maker.Ident(argname));
		JCMethodInvocation fieldmethodcallremove = maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), node.up().toName("remove")), params.toList());
		statements.add(maker.Exec(fieldmethodcallremove)); //turns the .remove call into a statement for the body
		
		JCBlock removeMethodBody = maker.Block(0, statements.toList());
		//defining and injecting the remove method
		JCMethodDecl unsubscribe = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC), node.toName("unsubscribeFrom"+node.up().getName()) ,maker.TypeIdent(CTC_VOID),
		List.<JCTypeParameter>nil(),args.toList(), List.<JCExpression>nil(), removeMethodBody,null ), node.up().get(), node.getContext());
		injectMethod(node.up().up(), unsubscribe);
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


