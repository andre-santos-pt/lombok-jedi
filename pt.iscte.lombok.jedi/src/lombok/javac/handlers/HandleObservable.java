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
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
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
public class HandleObservable extends JavacAnnotationHandler<Observable> {

	private static final List<JCExpression> NIL_EXPRESSION = List.nil();

	@Override
	public void handle(AnnotationValues<Observable> annotation,
			JCAnnotation ast, JavacNode node) {
		ArrayList<String> customNotifiableNames = new ArrayList<String>();
		ListBuffer<JCVariableDecl> notifiable = new ListBuffer<JCVariableDecl>();
		JavacTreeMaker maker = node.up().getTreeMaker();
		Observable annotationInstance = annotation.getInstance();
		String annotationName = Observable.class.getName();
		boolean validationisafter = annotationInstance.after();
		// Class<?> customInterface = annotationInstance.type();
		Object obj = annotation.getActualExpression("type");
		String listenermethod = annotationInstance.operation();
		String listenername = annotationInstance.typeName();
		String fieldName = annotationInstance.fieldName();
		String addMethodName = annotationInstance.addMethodName();
		String removeMethodName = annotationInstance.removeMethodName();
		boolean interfDefined = false;
		boolean customInfetfDefined = false;
		notificationValitations(maker, validationisafter, node.up(),
				notifiable, customNotifiableNames);
		if (obj != null && !obj.equals(void.class)) {
			customInfetfDefined = true;

		}
		if (fieldName == null || fieldName.equals("")) {
			fieldName = fieldName + node.up().getName() + "Listeners";

		}
		if (listenername == null || listenername.equals("")) {
			listenername = listenername
					+ JediJavacUtil.firstToUpper(node.up().getName())
					+ "Listener";
			if (!customInfetfDefined) {
				interfDefined = true;
			}
		} else {
			interfDefined = true;
		}

		JCMethodDecl annotatedmethod = (JCMethodDecl) node.up().get();
		JavacNode fieldNode;
		if (interfDefined != customInfetfDefined) {
			if (interfDefined) {
				if (listenermethod.equals("") || listenermethod == null) {
					listenermethod = "notify";
				}
				JCClassDecl interf = creatingInterfaceListener(node,
						notifiable.toList(), customNotifiableNames,
						listenername, maker, listenermethod, annotationName);
				fieldNode = creatingFieldAndMethods(node, maker,
						annotationName, interf.name.toString(), fieldName,
						addMethodName, removeMethodName, false);
				notifyingObservers(node, notifiable, maker, validationisafter,
						annotatedmethod, fieldNode, listenermethod, true);
			} else {
				Symbol method = findMethod(node, notifiable, maker, obj,
						listenermethod);
				JCFieldAccess field = (JCFieldAccess) obj;
				Type type = field.selected.type;
				
				fieldNode = creatingFieldAndMethods(node, maker,
						annotationName, type.toString(), fieldName,
						addMethodName, removeMethodName, true);

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

	private JavacNode creatingFieldAndMethods(JavacNode node,
			JavacTreeMaker maker, String annotationName, String interfName,
			String fieldName, String addMethodName, String removeMethodName,
			boolean methodsSkip) {
		JavacNode fieldNode = checkCompatibility(node, interfName, fieldName,
				methodsSkip);
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
			fieldNode = creatinglocalfield(node, maker, interfName,
					annotationName, fieldName);
			
		}
		if (!addMethodExists(node, maker, addMethodName)) {
			
			createAddMethod(node, maker, interfName, annotationName,
					fieldNode.getName());

		}
		if (!removeMethodExists(node, maker, removeMethodName)) {
			createRemoveMethod(node, maker, interfName, annotationName,
					fieldNode.getName());
		}
		return fieldNode;
	}

	private boolean addMethodExists(JavacNode node, JavacTreeMaker maker,
			String addMethod) {

		for (JavacNode subnode : node.up().up().down()) {
			if (subnode.getKind().equals(Kind.METHOD)) {
				if (subnode.getName().equals(addMethod)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean removeMethodExists(JavacNode node, JavacTreeMaker maker,
			String removeMethod) {

		for (JavacNode subnode : node.up().up().down()) {
			if (subnode.getKind().equals(Kind.METHOD)) {
				if (subnode.getName().equals(removeMethod)) {
					return true;
				}
			}
		}

		return false;
	}

	private JavacNode checkCompatibility(JavacNode node, String interfName,
			String fieldName, boolean methodsSkip) {

		JavacNode fieldNode = null;
		if (methodsSkip)
			for (JavacNode subnode : node.up().up().down()) {
				if (subnode.getKind().equals(Kind.FIELD)) {
					if (subnode.getName().equals(fieldName)) {

						JCVariableDecl field = (JCVariableDecl) subnode.get();
						if (field.vartype.toString().contains(
								List.class.getSimpleName())) {
							if (field.vartype.toString().contains(
									"<" + interfName + ">")) {

								fieldNode = subnode;

							} else {
								node.addError("The field " + subnode.getName()
										+ " cannot store interfaces of type "
										+ interfName);
							}
						} else {
							node.addError("Please define a custom name difrent from "
									+ fieldName);
						}
					}
				}
			}
		return fieldNode;
	}

	private Symbol findMethod(JavacNode node,
			ListBuffer<JCVariableDecl> notifiable, JavacTreeMaker maker,
			Object obj, String listenermethod) {
		ArrayList<Symbol> method = new ArrayList<Symbol>();
		boolean nameSet = false;
		if (!listenermethod.equals("") && listenermethod != null) {
			nameSet = true;
		}
		if (obj != null)
			if (!obj.equals(void.class)) {
				JCFieldAccess field = (JCFieldAccess) obj;
				Type interfacetype = field.selected.type;

				if (!interfacetype.tsym.isInterface()) {
					node.addError("The value of the atribute type can only be an interface.");
				} else {
					for (Symbol member : interfacetype.tsym
							.getEnclosedElements()) {

						ExecutableElement exElem = (ExecutableElement) member;
						if (member.getKind().equals(ElementKind.METHOD)
								&& exElem
										.getModifiers()
										.contains(
												javax.lang.model.element.Modifier.PUBLIC)) {

							ListBuffer<JCVariableDecl> parameters = new ListBuffer<JCVariableDecl>();
							ListBuffer<JCExpression> arguments = new ListBuffer<JCExpression>();

							HandleWrapper.drillIntoMethod(node, maker,
									interfacetype, member, exElem, parameters,
									arguments);
							if (nameSet) {
								if (JediJavacUtil.parametersEquals(
										parameters.toList(),
										notifiable.toList())
										&& listenermethod.equals(member
												.getSimpleName().toString()))
									method.add(member);
							} else {
								if (JediJavacUtil.parametersEquals(
										parameters.toList(),
										notifiable.toList()))
									method.add(member);
							}

						}
					}

				}

			}
		if (method.size() > 1) {
			node.addError("Multiple possible methods found."
					+ method.toString());
			return null;
		}

		if (method.size() == 0) {
			String types = "";
			int pos = 0;
			for (JCVariableDecl var : notifiable) {
				types = types + "" + var.vartype.toString();
				if (pos < notifiable.size() - 2)
					types = types + ",";
			}
			node.addError("The interface contains no method with the argument types ("
					+ types + ")");
			return null;
		}
		return method.get(0);
	}

	private JavacNode creatinglocalfield(JavacNode methodNode,
			JavacTreeMaker maker, String interftype, String annotationName,
			String fieldName) {

		JCExpression Arraytype = maker.TypeApply(
				handleArrayType(methodNode, maker, java.util.ArrayList.class),
				List.<JCExpression> of(maker.Ident(methodNode
						.toName(interftype))));
		JCExpression listtype = maker.TypeApply(
				handleArrayType(methodNode, maker, java.util.List.class), List
						.<JCExpression> of(maker.Ident(methodNode
								.toName(interftype))));
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression> nil(),
				Arraytype, NIL_EXPRESSION, null);
		JCVariableDecl field = maker.VarDef(
				maker.Modifiers(Flags.PRIVATE | Flags.FINAL),
				methodNode.toName(fieldName), listtype, fieldinit);
		JavacNode fieldNode = JediJavacUtil.injectField(methodNode.up().up(),
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
		ListBuffer<JCExpressionStatement> step = new ListBuffer<JCExpressionStatement>(); // FOR
																							// (_;_;X)
		ListBuffer<JCStatement> init = new ListBuffer<JCStatement>(); // For (X
																		// ; _ ;
																		// _)
		ListBuffer<JCExpression> parameters = new ListBuffer<JCExpression>();
		// FOR (_ ; _.size(); _)
		JCExpression sizecall = maker.Apply(
				NIL_EXPRESSION,
				maker.Select(maker.Ident(fieldName),
						methodNode.up().toName("size")),
				List.<JCExpression> nil());
		// For (X ; _ ; _)
		JCStatement i = maker.VarDef(maker.Modifiers(0),
				methodNode.toName("i"), maker.TypeIdent(CTC_INT),
				maker.Literal(0)); // int i=0
		init.add(i);
		// For (_ ; X ; _)
		JCExpression cond = maker.Binary(CTC_LESS_THAN,
				maker.Ident(((JCVariableDecl) i).name), sizecall); // i<fieldName.size()
		//
		// FOR (_; _ ; X)
		JCExpression stepleftside = maker.Ident(((JCVariableDecl) i).name); // i
		JCExpression steprightside = maker.Binary(CTC_PLUS,
				maker.Ident(((JCVariableDecl) i).name), maker.Literal(1)); // i
																			// +1
		JCAssign stepExpressions = maker.Assign(stepleftside, steprightside); // append(i)
																				// com
																				// (i+1)
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
		for (JavacNode nodel : methodnode.down()) {

			if (nodel.getKind().equals(lombok.core.AST.Kind.LOCAL)
					|| nodel.getKind().equals(lombok.core.AST.Kind.ARGUMENT)) {
				notifyannotations = JediJavacUtil.findAnnotations(nodel,
						Pattern.compile("^(?:ObserverNotify)$",
								Pattern.CASE_INSENSITIVE));
				if (notifyannotations.size() > 0) {
					if (nodel.getKind().equals(lombok.core.AST.Kind.LOCAL)
							&& !validationisafter) {
						nodel.addError("Only arguments can be notified when notification is set to before");
					} else {
						var = (JCVariableDecl) nodel.get();
						String group;
						if (notifyannotations.get(0).args.size() > 0) {
							JCAssign x = (JCAssign) notifyannotations.get(0).args
									.get(0);
							group = (String) ((JCLiteral) x.rhs).value;
						} else {
							group = var.name.toString();
						}

						// if(map.get(group)==null){
						customnames.add(group);
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

	private JCClassDecl creatingInterfaceListener(JavacNode node,
			List<JCVariableDecl> notifiable, ArrayList<String> customNames,
			String listenerName, JavacTreeMaker maker, String methodname,
			String annotationName) {

		JCClassDecl interf = maker.ClassDef(
				maker.Modifiers(Flags.INTERFACE | Flags.PUBLIC),
				node.toName(JediJavacUtil.firstToUpper(listenerName)),
				List.<JCTypeParameter> nil(), null, List.<JCExpression> nil(),
				List.<JCTree> nil());

		// injecting the interface
		JavacNode interfnode = JediJavacUtil.injectType(node.up().up(), interf,
				annotationName);
		JCClassDecl subject = (JCClassDecl) node.up().up().get();
		JCVariableDecl subjectdeclared = maker.VarDef(maker.Modifiers(0),
				node.toName("subject"), maker.Ident(subject.name), null);
		if (notifiable.size() > 0) {
			ListBuffer<JCVariableDecl> list = new ListBuffer<JCVariableDecl>();
			for (int i = 0; i < notifiable.size(); i++) {
				list.add(maker.VarDef(maker.Modifiers(0),
						interfnode.toName(JediJavacUtil
								.firstToUpper(customNames.get(i))), notifiable
								.get(i).vartype, null));
			}
			// for (int i=0;i<map.size();i++) {
			// list.add(maker.VarDef(maker.Modifiers(0),
			// interfnode.toName("new"+firstToUpper(map.get(i).name.toString())),
			// map.get(i).vartype, null));
			// }
			JCMethodDecl verify = JediJavacUtil.recursiveSetGeneratedBy(maker
					.MethodDef(maker.Modifiers(0),
							interfnode.toName(methodname),
							maker.TypeIdent(CTC_VOID),
							List.<JCTypeParameter> nil(), list.toList(),
							List.<JCExpression> nil(), null, null), node.up()
					.get(), node.up().getContext());
			// injecting the method in the interface
			verify.params = verify.params.prepend(subjectdeclared);
			JediJavacUtil.injectMethod(interfnode, verify, annotationName);
		}

		return interf;
	}



	private void createAddMethod(JavacNode node, JavacTreeMaker maker,
			String interf, String annotationName, String fnameame) {
		Name fieldName=node.toName(fnameame);
		ListBuffer<JCVariableDecl> args = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> params = new ListBuffer<JCExpression>();
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();

		Name argname = node.toName("listener");

		// the argument from the method addMethodListener()

		args.add(maker.VarDef(maker.Modifiers(0), argname,
				maker.Ident(node.toName(interf)), null));
		params.add(maker.Ident(argname));
		//
		// calling the method .add() from hashset

		JCMethodInvocation fieldmethodcall = maker.Apply(NIL_EXPRESSION,
				maker.Select(maker.Ident(fieldName), node.up().toName("add")),
				params.toList());
		statements.add(maker.Exec(fieldmethodcall)); // turns the .add call into
														// a statement for the
														// body

		JCBlock body = maker.Block(0, statements.toList());
		// defining and injecting the add method
		String methodName="add"+ JediJavacUtil.firstToUpper(JediJavacUtil
						.removePrefixFromString(interf));
		JCMethodDecl subscribe = JediJavacUtil.recursiveSetGeneratedBy(maker
				.MethodDef(
						maker.Modifiers(Flags.PUBLIC),
						node.toName(methodName),
						maker.TypeIdent(CTC_VOID),
						List.<JCTypeParameter> nil(), args.toList(),
						List.<JCExpression> nil(), body, null),
				node.up().get(), node.getContext());
		JediJavacUtil.injectMethod(node.up().up(), subscribe, annotationName);
	}

	private void createRemoveMethod(JavacNode node, JavacTreeMaker maker,
			String interf, String annotationName, String fname) {
		Name argname = node.toName("listener");
		Name fieldName=node.toName(fname);
		ListBuffer<JCVariableDecl> args = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> params = new ListBuffer<JCExpression>();
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();

		// calling the method .remove() from hashset
		args.add(maker.VarDef(maker.Modifiers(0), argname,
				maker.Ident(node.toName(interf)), null));
		params.add(maker.Ident(argname));
		JCMethodInvocation fieldmethodcallremove = maker
				.Apply(NIL_EXPRESSION,
						maker.Select(maker.Ident(fieldName),
								node.up().toName("remove")), params.toList());
		statements.add(maker.Exec(fieldmethodcallremove)); // turns the .remove
															// call into a
															// statement for the
															// body

		JCBlock removeMethodBody = maker.Block(0, statements.toList());
		// defining and injecting the remove method
		JCMethodDecl unsubscribe = JediJavacUtil.recursiveSetGeneratedBy(maker
				.MethodDef(
						maker.Modifiers(Flags.PUBLIC),
						node.toName("remove"
								+ JediJavacUtil.firstToUpper(JediJavacUtil
										.removePrefixFromString(interf))),
						maker.TypeIdent(CTC_VOID),
						List.<JCTypeParameter> nil(), args.toList(),
						List.<JCExpression> nil(), removeMethodBody, null),
				node.up().get(), node.getContext());
		JediJavacUtil.injectMethod(node.up().up(), unsubscribe, annotationName);
	}

	private JCExpression handleArrayType(JavacNode node, JavacTreeMaker maker,
			Class<?> clazz) {
		int n = 0;
		while (clazz.isArray()) {
			clazz = clazz.getComponentType();
			n++;
		}

		// String typeName = clazz.isArray() ?
		// clazz.getComponentType().getName() : ;
		JCExpression type = JediJavacUtil
				.genTypeRef(node.up(), clazz.getName());

		while (n > 0) {
			type = maker.TypeArray(type);
			n--;
		}

		// if(clazz.isArray())
		// type = maker.TypeArray(type);
		return type;
	}
}
