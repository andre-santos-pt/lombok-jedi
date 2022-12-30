package lombok.javac.handlers;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import lombok.Composite;
import lombok.Composite.Children;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import org.mangosdk.spi.ProviderFor;

import java.util.Collection;

import static lombok.javac.Javac.CTC_BOT;

@ProviderFor(JavacAnnotationHandler.class) 
@HandlerPriority(8) 
@ResolutionResetNeeded
public class HandleCompositeChildren extends JavacAnnotationHandler<Children> {
	private static final List<JCVariableDecl> NIL_VARIABLEDECL= List.nil();
	public void handle(AnnotationValues<Children> annotation, JCAnnotation ast, JavacNode annotationNode) {
		Composite.Children annotationInstance=  annotation.getInstance();
		String methodAddName = annotationInstance.methodAddChildrenName();
		String methodGetName = annotationInstance.methodGetChildrenName();
		
		JavacNode fieldNode=annotationNode.up();
		JavacNode clazzNode = fieldNode.up();
		Types types = Types.instance(clazzNode.getAst().getContext());
		
		JCVariableDecl field = (JCVariableDecl) annotationNode.up().get();
		boolean isCompositeNode=false;
		JCClassDecl clazz=(JCClassDecl) clazzNode.get();
		List<Type> closure = types.closure(clazz.sym.type);
		for (Type s : closure) {
			
			ClassType ct = (ClassType) s;
			Composite ann = ct.tsym.getAnnotation(Composite.class);
			if(ann!=null)
				isCompositeNode=true;
		}

		if(isCompositeNode){
			checkFieldCompatibility(fieldNode,
					field,Composite.Children.class.getName(),methodAddName,methodGetName);
				
		
		}else{
			annotationNode.up().up().addError("Only a class annotated with "+Composite.class.getSimpleName()+" can have a field annotated with "+Composite.Children.class.getSimpleName()+".");
		}
		
	}

	private void checkFieldCompatibility(JavacNode fieldNode,
			JCVariableDecl field,String annotationName, String methodAddName, String methodGetName) {
		
		JavacTreeMaker maker = fieldNode.getTreeMaker();
		Types types = Types.instance(fieldNode.getAst().getContext());
		List<Type> parameterTypes = field.sym.type.getTypeArguments();
		Type type;	
		if(parameterTypes.size()>0){
				 type = parameterTypes.get(0);	
			}else{
				 type =field.sym.type;
			}
		
		Composite.Component containsAnnotation = type.tsym.getAnnotation(Composite.Component.class);
		
		List<Type> closure = types.closure(field.sym.type);
			
			
				if (!iscollection(closure)) {
					if (parameterTypes.size() != 0) {
						fieldNode.addError("This Type is not a subtype "+Collection.class.getSimpleName()+".");	
					} else {
						if (containsAnnotation == null) {
							fieldNode.addError("This type must be a class annotated with @"+Composite.Component.class.getSimpleName());
							} else {
								if(!type.tsym.isInterface()){
									injectOnConstructor(maker, fieldNode.up(),annotationName,methodAddName);	
								}
								createMethodAdd(maker, fieldNode.up(), field,annotationName,methodAddName,!type.tsym.isInterface());
								createMethodgetChild(maker, fieldNode.up(), field,annotationName,methodGetName,!type.tsym.isInterface());
								
							}
						
					}
					
				} else {
					if (parameterTypes.size() > 0) {
						if (parameterTypes.size() !=1) {
							fieldNode.addError("The type "+type.tsym.toString()+" cannot have more than one type argument.");
						}else{
							if (containsAnnotation == null) {
								fieldNode.addError("The type argument of this Collection must be annotated with  @ " + Composite.Component.class.getSimpleName());
							}else{
								if(!type.tsym.isInterface()){
									injectOnConstructor(maker, fieldNode.up(),annotationName,methodAddName);
								}
								createMethodAddList(maker, fieldNode.up(), field,annotationName,methodAddName,!type.tsym.isInterface());
								createMethodgetChildren(maker, fieldNode.up(), field,annotationName,methodGetName,!type.tsym.isInterface());
								
							}
						}
						
					}else{
						fieldNode.addError("The Type Arguments must be defined for this field.");
					}
				}
			
	}
	
	private void createMethodgetChild(JavacTreeMaker maker, JavacNode classnode,
			JCVariableDecl list,String annotationName,String methodGetName, boolean hasbody) {
		if(methodGetName==null || methodGetName.equals("")){
			methodGetName="getChild";
		}
		String componentName = list.sym.type.toString();
		JCTypeApply type = maker.TypeApply(JediJavacUtil.handleArrayType(classnode, maker, java.util.Collection.class), List.<JCExpression>of(maker.Ident(classnode.toName(componentName))));
		JCBlock body= null;
		if(hasbody)
		body = maker.Block(0, List.<JCStatement>of(maker.Return((maker.Ident(list.name)))));
		JCMethodDecl method=JediJavacUtil.createMethod(maker, maker.Modifiers(Flags.PUBLIC), classnode.toName(methodGetName), maker.Ident(classnode.toName(componentName)), NIL_VARIABLEDECL, body);
		JediJavacUtil.injectMethod(classnode, method,annotationName);
		
	}

	private void createMethodAdd(JavacTreeMaker maker, JavacNode classnode,
			JCVariableDecl list,String annotationName, String methodAddName, boolean hasbody) {
		if(methodAddName==null || methodAddName.equals("")){
			methodAddName="addChild";
		}
		String componentName = list.sym.type.toString();
		JCVariableDecl param = maker.VarDef(maker.Modifiers(0), classnode.toName("child"), maker.Ident(classnode.toName(componentName)), null);
		JCBlock body = null;
		if(hasbody){
			JCAssign assign = maker.Assign(maker.Ident(list.name), maker.Ident(param.name));
		 body = maker.Block(0, List.<JCStatement>of(maker.Exec(assign)));
		}
		
		JCMethodDecl method=JediJavacUtil.createMethod(maker, maker.Modifiers(Flags.PUBLIC), classnode.toName(methodAddName),  maker.TypeIdent(lombok.javac.Javac.CTC_VOID), List.<JCVariableDecl>of(param), body);
		JediJavacUtil.injectMethod(classnode, method,annotationName);
		
	}

	private void injectOnConstructor(JavacTreeMaker maker, JavacNode node, String annotationName, String methodAddName) {
		boolean publiconstructor = false;
		if(methodAddName==null || methodAddName.equals("")){
			methodAddName="addChild";
		}
		boolean mainConstructor = false;
		JCMethodDecl constructor = null;
		JavacNode constructornode = null;
		for (JavacNode subnode : node.down()) {
			if (subnode.getKind().equals(Kind.METHOD)) {
				JCMethodDecl method = ((JCMethodDecl) subnode.get());
				if (method.restype == null) {
					if (!publiconstructor) {
						constructornode = subnode;
					}
					
					if (method.mods.flags == Flags.PUBLIC) {
						
						if (hasParentArgument(method.getParameters(), ((JCClassDecl) node.get()).name.toString())) {
							mainConstructor = true;
							constructor = method;
							constructornode = subnode;
						}
					}
					
				}
				
			}
		}
		
		if (mainConstructor) {
			JCExpression cond = maker.Binary(lombok.javac.Javac.CTC_NOT_EQUAL, maker.Ident(constructor.getParameters().get(0).name), maker.Literal(CTC_BOT, null));
			JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(constructor.getParameters().get(0).name), node.toName(methodAddName)), List.<JCExpression>of(maker.Ident(node.toName("this"))));
			JCStatement statement = maker.If(cond, maker.Exec(addcall), null);
			JCBlock body= maker.Block(0, List.<JCStatement>of(statement));
			constructor.body.stats = constructor.body.stats.append(body);
			constructornode.rebuild();
		} else {
			node.addError("class must have a public constructor with a parameter of type " + ((JCClassDecl) node.get()).name.toString());
		}
	}
	
	
	private void createMethodgetChildren(JavacTreeMaker maker, JavacNode classnode, JCVariableDecl list,String annotationName,String methodGetName, boolean hasbody) {
		if(methodGetName==null || methodGetName.equals("")){
			methodGetName="getChildren";
		}
		String componentName = list.sym.type.getTypeArguments().get(0).toString();
		JCTypeApply type = maker.TypeApply(JediJavacUtil.handleArrayType(classnode, maker, java.util.Collection.class), List.<JCExpression>of(maker.Ident(classnode.toName(componentName))));
		JCBlock body=null;
		
		if(hasbody){
						//JCExpression paramclasstype=JediJavacUtil.handleArrayType(classnode, maker, java.lang.IndexOutOfBoundsException.class);
						//JCVariableDecl param = maker.VarDef(maker.Modifiers(0), classnode.toName("err"), paramclasstype, null);
						//JCExpression exceptionType=JediJavacUtil.handleArrayType(classnode, maker, ArrayIndexOutOfBoundsException.class);
						//JCExpression message = maker.Literal("No child exists in that position.");
						//JCExpression exceptionInstance = maker.NewClass(null, List.<JCExpression>nil(), exceptionType, List.<JCExpression>of(message), null);
						//JCBlock	catchBody =maker.Block(0, List.<JCStatement>of(maker.Throw(exceptionInstance)));
						//JCCatch c = maker.Catch(param, catchBody);
			
			
			
			JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(JediJavacUtil.handleArrayType(classnode, maker, java.util.Collections.class), classnode.toName("unmodifiableCollection")), List.<JCExpression>of(maker.Ident(list.name)));
			body = maker.Block(0, List.<JCStatement>of(maker.Return((addcall))));
			//JCTry tryv = maker.Try(body, List.<JCCatch>of(c), null);
			//body = maker.Block(0, List.<JCStatement>of(tryv));
		}
		JCMethodDecl method=JediJavacUtil.createMethod(maker, maker.Modifiers(Flags.PUBLIC), classnode.toName(methodGetName), type, NIL_VARIABLEDECL, body);
		JediJavacUtil.injectMethod(classnode, method,annotationName);
		
	}
	
	static boolean hasParentArgument(List<JCVariableDecl> parameters, String componentName) {
		for (JCVariableDecl var : parameters) {
			if (var.vartype.toString().equals(componentName)) {
				return true;
			}
		}
		return false;
	}

	private void createMethodAddList(JavacTreeMaker maker, JavacNode classnode, JCVariableDecl list,String annotationName,String methodAddName, boolean hasbody) {
		if(methodAddName==null || methodAddName.equals("")){
			methodAddName="addChild";
		}
		String componentName = list.sym.type.getTypeArguments().get(0).toString();
		JCVariableDecl param = maker.VarDef(maker.Modifiers(0), classnode.toName("child"), maker.Ident(classnode.toName(componentName)), null);
		JCBlock body= null;
		if(hasbody){
			JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(list.name), classnode.toName("add")), List.<JCExpression>of(maker.Ident(param.name)));
			body = maker.Block(0, List.<JCStatement>of(maker.Exec(addcall)));
				
		}
		JCMethodDecl method= JediJavacUtil.createMethod(maker, maker.Modifiers(Flags.PUBLIC), classnode.toName(methodAddName), maker.TypeIdent(lombok.javac.Javac.CTC_VOID), List.<JCVariableDecl>of(param), body);
		JediJavacUtil.injectMethod(classnode, method,annotationName);
		
	}
	
	static boolean iscollection(java.util.List<Type> closure) {
		boolean iscollection = false;
		for (Type s : closure) {
			if (s.toString().contains("Collection")) {
				iscollection = true;
			}
		}
		return iscollection;
	}

}
