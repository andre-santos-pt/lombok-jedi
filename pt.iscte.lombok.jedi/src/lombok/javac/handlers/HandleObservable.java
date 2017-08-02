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

import lombok.Composite;
import lombok.Flyweight;
import lombok.Observable;
import lombok.Composite.Children;
import lombok.Observable.Notify;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import lombok.javac.JavacTreeMaker.TypeTag;
import lombok.javac.handlers.JediJavacUtil;

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
@ResolutionResetNeeded
public class HandleObservable extends JavacAnnotationHandler<Observable> {

	private static final List<JCExpression> NIL_EXPRESSION = List.nil();

	@Override
	public void handle(AnnotationValues<Observable> annotation,
			JCAnnotation ast, JavacNode node) {
		JavacNode methodNode=node.up();
		ArrayList<String> customNotifiableNames = new ArrayList<String>();
		ListBuffer<JCVariableDecl> notifiable = new ListBuffer<JCVariableDecl>();
		JavacTreeMaker maker = node.getTreeMaker();
		Observable annotationInstance = annotation.getInstance();
		String annotationName = Observable.class.getName();
		boolean validationisafter = annotationInstance.after();
		Object obj = annotation.getActualExpression("type");
		String listenermethod = annotationInstance.operation();
		String listenername = annotationInstance.typeName();
		String fieldName = annotationInstance.fieldName();
		String addMethodName = annotationInstance.addMethodName();
		String removeMethodName = annotationInstance.removeMethodName();
		boolean interfDefined = false;
		boolean customInfetfDefined = false;
		notificationValitations(maker, validationisafter, methodNode,
				notifiable, customNotifiableNames);
		if (obj != null && !obj.equals(void.class)) {
			customInfetfDefined = true;

		}
		if (fieldName == null || fieldName.equals("")) {
			fieldName = fieldName + methodNode.getName() + "Listeners";

		}
		if (listenername == null || listenername.equals("")) {
			listenername = listenername
					+ JediJavacUtil.firstToUpper(methodNode.getName())
					+ "Listener";
			if (!customInfetfDefined) {
				interfDefined = true;
			}
		} else {
			interfDefined = true;
		}

		JCMethodDecl annotatedmethod = (JCMethodDecl) methodNode.get();
		JavacNode fieldNode;
		if (interfDefined != customInfetfDefined) {
			if (interfDefined) {
				if (listenermethod.equals("") || listenermethod == null) {
					listenermethod = "notify";
				}
				JavacNode interf = creatingInterfaceListener(methodNode.up(),
						listenername, maker, annotationName);
				if (notifiable.size() > 0) {
					
					createMethodOnInterface(listenermethod, annotationName,
							interf, notifiable.toList(),customNotifiableNames);
				}
				fieldNode = creatingFieldAndMethods(node, maker,
						annotationName, interf.getName(), fieldName,
						addMethodName, removeMethodName);
				notifyingObservers(node, notifiable, maker, validationisafter,
						annotatedmethod, fieldNode, listenermethod, false);
			} else {
				Symbol method = JediJavacUtil.findMethod(node, notifiable, maker, obj,
						listenermethod);
				JCFieldAccess field = (JCFieldAccess) obj;
				Type type = field.selected.type;
				
				fieldNode = creatingFieldAndMethods(node, maker,
						annotationName, type.toString(), fieldName,
						addMethodName, removeMethodName);

				if (!listenermethod.equals("") && listenermethod != null) {
					notifyingObservers(node, notifiable, maker,
							validationisafter, annotatedmethod, fieldNode,
							listenermethod, false);
				} else {
					if (method != null)
						notifyingObservers(node, notifiable, maker,
								validationisafter, annotatedmethod, fieldNode,
								method.name.toString(), false);
				}
			}
		} else {
			node.addError("Either you set a Listener, or choose a name for the creation of one.");
		}

	}

	private JavacNode creatingFieldAndMethods(JavacNode annotationNode,
			JavacTreeMaker maker, String annotationName, String interfName,
			String fieldName, String addMethodName, String removeMethodName) {
		JavacNode methodNode=annotationNode.up();
		JavacNode fieldNode = checkCompatibility(methodNode, interfName, fieldName);
		if (addMethodName == null || addMethodName.equals("")) {
			addMethodName = "add"
					+ JediJavacUtil.firstToUpper(JediJavacUtil
							.removePrefixFromString(interfName));
		}
		if (removeMethodName == null || removeMethodName.equals("")) {
			removeMethodName = "remove"
					+ JediJavacUtil.firstToUpper(JediJavacUtil
							.removePrefixFromString(interfName));
		}
		if (fieldNode == null) {
			fieldNode = creatinglocalfield(annotationNode, maker, interfName,
					annotationName, fieldName);
			
		}
		JCVariableDecl arg = maker.VarDef(null, methodNode.toName("listener"), maker.Ident(methodNode.toName(interfName)), null);
		if (!JediJavacUtil.methodExists(addMethodName,List.<JCVariableDecl>of(arg), methodNode.up())) {
			createMethod(methodNode, maker, interfName, annotationName,
					fieldNode.getName(),addMethodName);
		}
		if (!JediJavacUtil.methodExists(removeMethodName,List.<JCVariableDecl>of(arg),methodNode.up())) {
			createMethod(methodNode, maker, interfName, annotationName,
					fieldNode.getName(),removeMethodName);
		}
		return fieldNode;
	}



	private JavacNode checkCompatibility(JavacNode methodNode, String interfName,
			String fieldName) {

		JavacNode fieldNode = null;
			for (JavacNode subnode : methodNode.up().down()) {
				if (subnode.getKind().equals(Kind.FIELD)) {
					if (subnode.getName().equals(fieldName)) {

						JCVariableDecl field = (JCVariableDecl) subnode.get();
						if (field.vartype.toString().contains(
								List.class.getSimpleName())) {
							if (field.vartype.toString().contains(
									"<" + interfName + ">")) {

								fieldNode = subnode;

							} else {
								fieldNode.addError("The field " + subnode.getName()
										+ " cannot store interfaces of type "
										+ interfName);
							}
						}
					}
				}
			}
		return fieldNode;
	}


	private JavacNode creatinglocalfield(JavacNode annotationNode,
			JavacTreeMaker maker, String interftype, String annotationName,
			String fieldName) {

		JCExpression Arraytype = maker.TypeApply(
				JediJavacUtil.handleArrayType(annotationNode, maker, java.util.ArrayList.class),
				List.<JCExpression> of(maker.Ident(annotationNode
						.toName(interftype))));
		JCExpression listtype = maker.TypeApply(
				JediJavacUtil.handleArrayType(annotationNode, maker, java.util.List.class), List
						.<JCExpression> of(maker.Ident(annotationNode
								.toName(interftype))));
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression> nil(),
				Arraytype, NIL_EXPRESSION, null);
		JCVariableDecl field = maker.VarDef(
				maker.Modifiers(Flags.PRIVATE | Flags.FINAL),
				annotationNode.toName(fieldName), listtype, fieldinit);
		JavacNode fieldNode = JediJavacUtil.injectField(annotationNode.up().up(),
				field, annotationName);
		return fieldNode;
	}

	private void notifyingObservers(JavacNode methodNode,
			ListBuffer<JCVariableDecl> notifiable, JavacTreeMaker maker,
			boolean validationisafter, JCMethodDecl annotatedmethod,
			JavacNode fieldNode, String methodname, boolean argumentThis) {
		Name fieldName = fieldNode.toName(fieldNode.getName());
		ListBuffer<JCStatement> statements;
		statements = new ListBuffer<JCStatement>();
		ListBuffer<JCExpressionStatement> step = new ListBuffer<JCExpressionStatement>(); 
		ListBuffer<JCStatement> init = new ListBuffer<JCStatement>(); 
		ListBuffer<JCExpression> parameters = new ListBuffer<JCExpression>();
		// FOR (_ ; _.size(); _)
		JCExpression sizecall = maker.Apply(
				NIL_EXPRESSION,
				maker.Select(maker.Ident(fieldName),
						methodNode.up().toName("size")),
				List.<JCExpression> nil());
		JCStatement i = maker.VarDef(maker.Modifiers(0),
				methodNode.toName("i"), maker.TypeIdent(CTC_INT),
				maker.Literal(0));
		init.add(i);
		JCExpression cond = maker.Binary(CTC_LESS_THAN,
				maker.Ident(((JCVariableDecl) i).name), sizecall); 
		JCExpression stepleftside = maker.Ident(((JCVariableDecl) i).name); 
		JCExpression steprightside = maker.Binary(CTC_PLUS,
				maker.Ident(((JCVariableDecl) i).name), maker.Literal(1)); 
		JCAssign stepExpressions = maker.Assign(stepleftside, steprightside); 
		step.add(maker.Exec(stepExpressions));
		if (argumentThis)
			parameters.add(maker.Ident(methodNode.toName("this")));
		if (notifiable.size() > 0) {
			List<JCVariableDecl> notifiableparams = notifiable.toList();
			for (int k = 0; k < notifiableparams.size(); k++)
				parameters.add(maker.Ident(notifiableparams.get(k).getName()));

		}

		JCMethodInvocation getcall = maker.Apply(
				NIL_EXPRESSION,
				maker.Select(maker.Ident(fieldName),
						methodNode.up().toName("get")),
				List.<JCExpression> of(maker.Ident(((JCVariableDecl) i).name)));// fieldname.get(i))
		JCMethodInvocation notifycall = maker.Apply(NIL_EXPRESSION,
				maker.Select(getcall, methodNode.toName(methodname)),
				parameters.toList()); // previous/.notify()
		// add the complete statement
		statements.add(maker.Exec(notifycall));
		// define the body with all the statements
		JCBlock forbody = maker.Block(0, statements.toList());
		//

		// defining the loop
		JCForLoop forstat = maker.ForLoop(init.toList(), cond, step.toList(),
				forbody); // defining complete loop
		//
		if (validationisafter) {
			annotatedmethod.body.stats = annotatedmethod.body.stats
					.append(forstat); // after
		} else {
			annotatedmethod.body.stats = annotatedmethod.body.stats
					.prepend(forstat); // before
		}
	}

	private void notificationValitations(JavacTreeMaker maker,
			boolean validationisafter, JavacNode methodnode,
			ListBuffer<JCVariableDecl> notifiable, ArrayList<String> customnames) {
		List<JCAnnotation> notifyannotations;
		JCVariableDecl var;
		String customName = null;
		for (JavacNode nodel : methodnode.down()) {

			if (nodel.getKind().equals(lombok.core.AST.Kind.LOCAL)
					|| nodel.getKind().equals(lombok.core.AST.Kind.ARGUMENT)) {
			//	notifyannotations = JediJavacUtil.findAnnotations(nodel,Pattern.compile("^(?:ObserverNotify)$",Pattern.CASE_INSENSITIVE));
				var=(JCVariableDecl) nodel.get();
				
				notifyannotations=var.mods.getAnnotations();
				JCAnnotation fieldType=JediJavacUtil.varContainsAnnotation(notifyannotations);
				if (fieldType!=null) {
					
					if (nodel.getKind().equals(lombok.core.AST.Kind.LOCAL)
							&& !validationisafter) {
						nodel.addError("Only arguments can be notified when notification is set to before");
					} else {
						
						
						
						if (fieldType.args.size() > 0) {
							if(fieldType.args.get(0).getKind().equals(com.sun.source.tree.Tree.Kind.ASSIGNMENT)){
								JCAssign x = (JCAssign) fieldType.args.get(0);
								customName = (String) ((JCLiteral) x.rhs).value;	
							}
							if(fieldType.args.get(0).getKind().equals(com.sun.source.tree.Tree.Kind.STRING_LITERAL)){

								customName=(String) ((JCLiteral) fieldType.args.get(0)).value;
							}
							
						} else {
							customName = var.name.toString();
						}
						
						// if(map.get(group)==null){
						customnames.add(customName);
						notifiable.add(maker.VarDef(maker.Modifiers(0),
								var.name, (JCExpression) var.getType(),
								var.getInitializer()));

						// }

					}
				}
			}

		}
		if (notifiable.size() == 0)
			methodnode
					.addError("The method must have atleast one argument or field annotated with @ObserverNotify");
	}

	private JavacNode creatingInterfaceListener(JavacNode clazzNode,
			String listenerName, JavacTreeMaker maker,
			String annotationName) {
		JavacNode interfnode=JediJavacUtil.interfaceExists(listenerName,clazzNode);
		if(interfnode==null){
					JCClassDecl interf = maker.ClassDef(
					maker.Modifiers(Flags.INTERFACE | Flags.PUBLIC),
					clazzNode.toName(JediJavacUtil.firstToUpper(listenerName)),
					List.<JCTypeParameter> nil(), null, List.<JCExpression> nil(),
					List.<JCTree> nil());

			// injecting the interface
			interfnode = JediJavacUtil.injectType(clazzNode, interf,
					annotationName);
			
			
		}
		
		

		return interfnode;
	}

	private void createMethodOnInterface(
			String methodname, String annotationName, JavacNode interfnode,
			List<JCVariableDecl> notifiable, ArrayList<String> customNames) {
		JavacTreeMaker maker = interfnode.getTreeMaker();
		ListBuffer<JCVariableDecl> list = new ListBuffer<JCVariableDecl>();
		for (int i = 0; i < notifiable.size(); i++) {
			list.add(maker.VarDef(maker.Modifiers(0),
					interfnode.toName(JediJavacUtil
							.firstToUpper(customNames.get(i))), notifiable
							.get(i).vartype, null));
		}
		JCMethodDecl verify = JediJavacUtil.createMethod(maker, maker.Modifiers(0), interfnode.toName(methodname), maker.TypeIdent(CTC_VOID), list.toList(), null);
		JediJavacUtil.injectMethod(interfnode, verify, annotationName);
	}


	private void createMethod(JavacNode methodNode, JavacTreeMaker maker,
			String interfName, String annotationName, String fieldName,
			String methodName) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> paramsRef = new ListBuffer<JCExpression>();
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
		Name argname = methodNode.toName("listener");
		params.add(maker.VarDef(maker.Modifiers(0), argname,
				maker.Ident(methodNode.toName(interfName)), null));
		paramsRef.add(maker.Ident(argname));
		JCMethodInvocation fieldmethodcall = maker.Apply(NIL_EXPRESSION,
				maker.Select(maker.Ident(methodNode.toName(fieldName)), methodNode.toName("add")),
				paramsRef.toList());
		statements.add(maker.Exec(fieldmethodcall)); 

		JCBlock body = maker.Block(0, statements.toList());

		JCMethodDecl method= JediJavacUtil.createMethod(maker, maker.Modifiers(Flags.PUBLIC), methodNode.toName(methodName), maker.TypeIdent(CTC_VOID), params.toList(), body);
		JediJavacUtil.injectMethod(methodNode.up(), method, annotationName);
		
		
	}
}
