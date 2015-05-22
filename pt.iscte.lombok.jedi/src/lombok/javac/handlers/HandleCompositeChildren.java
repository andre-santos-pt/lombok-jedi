package lombok.javac.handlers;

import static lombok.javac.Javac.CTC_BOT;

import java.awt.Composite;
import java.util.Collection;

import lombok.CompositeChildren;
import lombok.CompositeComponent;
import lombok.VisitableNode;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
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
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;

@ProviderFor(JavacAnnotationHandler.class) 
@HandlerPriority(8) 
@ResolutionResetNeeded
public class HandleCompositeChildren extends JavacAnnotationHandler<CompositeChildren> {
	public void handle(AnnotationValues<CompositeChildren> annotation, JCAnnotation ast, JavacNode annotationNode) {
		
		
		JavacNode fieldNode=annotationNode.up();
		JCVariableDecl field = (JCVariableDecl) annotationNode.up().get();
		boolean isCompositeNode=false;
		//		Types types = Types.instance(typeNode.getContext());
		for (JavacNode subnode : fieldNode.up().down()) {
			if(subnode.getKind()==Kind.ANNOTATION){
				JCAnnotation ann =(JCAnnotation)subnode.get();
				if(ann.annotationType.toString().equals("Composite")){
					isCompositeNode=true;
				}
			}
		}
		if(isCompositeNode){
			checkFieldCompatibility(fieldNode,
					field,CompositeChildren.class.getName());
				
		
		}else{
			annotationNode.up().up().addError("Only a class annotated with "+Composite.class.getSimpleName()+" can have a field annotated with "+CompositeChildren.class.getSimpleName()+".");
		}
		
	}

	private void checkFieldCompatibility(JavacNode fieldNode,
			JCVariableDecl field,String annotationName) {
		
		JavacTreeMaker maker = fieldNode.getTreeMaker();
		Types types = Types.instance(fieldNode.getAst().getContext());
		List<Type> parameterTypes = field.sym.type.getTypeArguments();
		Type type;	
		if(parameterTypes.size()>0){
				 type = parameterTypes.get(0);	
			}else{
				 type =field.sym.type;
			}
		
		CompositeComponent containsAnnotation = type.tsym.getAnnotation(CompositeComponent.class);
		
		List<Type> closure = types.closure(field.sym.type);
			
			
				if (!iscollection(closure)) {
					if (parameterTypes.size() != 0) {
						fieldNode.addError("This Type is not a subtype "+Collection.class.getSimpleName()+".");	
					} else {
						if (containsAnnotation == null) {
							fieldNode.addError("This type must be a class annotated with @"+CompositeComponent.class.getSimpleName());
							//fieldNode.addError("The type argument of this Collection must be annotated with  @ " + CompositeComponent.class.getSimpleName());
							} else {
								if(!type.tsym.isInterface()){
									injectOnConstructor(maker, fieldNode.up(),annotationName);	
								}
								createMethodAdd(maker, fieldNode.up(), field,annotationName);
								createMethodgetSon(maker, fieldNode.up(), field,annotationName);
								
							}
						
					}
					
				} else {
					if (parameterTypes.size() > 0) {
						if (parameterTypes.size() !=1) {
							fieldNode.addError("The type "+type.tsym.toString()+" cannot have more than one type argument.");
						}else{
							if (containsAnnotation == null) {
								fieldNode.addError("The type argument of this Collection must be annotated with  @ " + CompositeComponent.class.getSimpleName());
							}else{
								if(!type.tsym.isInterface()){
									injectOnConstructor(maker, fieldNode.up(),annotationName);
								}
								createMethodAddList(maker, fieldNode.up(), field,annotationName);
								createMethodgetChildren(maker, fieldNode.up(), field,annotationName);
								
							}
						}
						
					}else{
						fieldNode.addError("The Type Arguments must be defined for this field.");
					}
				}
			
	}
	
	private void createMethodgetSon(JavacTreeMaker maker, JavacNode classnode,
			JCVariableDecl list,String annotationName) {
		String componentName = list.sym.type.toString();
		JCTypeApply type = maker.TypeApply(handleArrayType(classnode, maker, java.util.Collection.class), List.<JCExpression>of(maker.Ident(classnode.toName(componentName))));
		//JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(handleArrayType(classnode, maker, java.util.Collections.class), classnode.toName("unmodifiableCollection")), List.<JCExpression>of(maker.Ident(list.name)));
		JCBlock body = maker.Block(0, List.<JCStatement>of(maker.Return((maker.Ident(list.name)))));
		JCMethodDecl method = maker.MethodDef(maker.Modifiers(Flags.PUBLIC), classnode.toName("getChildren"), maker.Ident(classnode.toName(componentName)), List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), body, null);
		JediJavacUtil.injectMethod(classnode, method,annotationName);
		
	}

	private void createMethodAdd(JavacTreeMaker maker, JavacNode classnode,
			JCVariableDecl list,String annotationName) {
		String componentName = list.sym.type.toString();
		JCVariableDecl param = maker.VarDef(maker.Modifiers(0), classnode.toName("parent"), maker.Ident(classnode.toName(componentName)), null);
		//JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(list.name), classnode.toName("add")), List.<JCExpression>of(maker.Ident(param.name)));
		JCAssign assign = maker.Assign(maker.Ident(list.name), maker.Ident(param.name));
		JCBlock body = maker.Block(0, List.<JCStatement>of(maker.Exec(assign)));
		JCMethodDecl method = maker.MethodDef(maker.Modifiers(Flags.PUBLIC), classnode.toName("add"), maker.TypeIdent(lombok.javac.Javac.CTC_VOID), List.<JCTypeParameter>nil(), List.<JCVariableDecl>of(param), List.<JCExpression>nil(), body, null);
		JediJavacUtil.injectMethod(classnode, method,annotationName);
		
	}

	private void injectOnConstructor(JavacTreeMaker maker, JavacNode node, String annotationName) {
		boolean publiconstructor = false;
		
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
			JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(constructor.getParameters().get(0).name), node.toName("add")), List.<JCExpression>of(maker.Ident(node.toName("this"))));
			JCStatement statement = maker.If(cond, maker.Exec(addcall), null);
			JCBlock body= maker.Block(0, List.<JCStatement>of(statement));
			constructor.body.stats = constructor.body.stats.append(body);
			constructornode.rebuild();
		} else {
			node.addError("class must have a public constructor with a parameter of type " + ((JCClassDecl) node.get()).name.toString());
		}
	}
	
	static boolean hasParentArgument(List<JCVariableDecl> parameters, String componentName) {
		for (JCVariableDecl var : parameters) {
			if (var.vartype.toString().equals(componentName)) {
				return true;
			}
		}
		return false;
	}
	
	private void createMethodgetChildren(JavacTreeMaker maker, JavacNode classnode, JCVariableDecl list,String annotationName) {
		String componentName = list.sym.type.getTypeArguments().get(0).toString();
		JCTypeApply type = maker.TypeApply(handleArrayType(classnode, maker, java.util.Collection.class), List.<JCExpression>of(maker.Ident(classnode.toName(componentName))));
		JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(handleArrayType(classnode, maker, java.util.Collections.class), classnode.toName("unmodifiableCollection")), List.<JCExpression>of(maker.Ident(list.name)));
		JCBlock body = maker.Block(0, List.<JCStatement>of(maker.Return((addcall))));
		JCMethodDecl method = maker.MethodDef(maker.Modifiers(Flags.PUBLIC), classnode.toName("getChildren"), type, List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), body, null);
		JediJavacUtil.injectMethod(classnode, method,annotationName);
		
	}
	
	private void createMethodAddList(JavacTreeMaker maker, JavacNode classnode, JCVariableDecl list,String annotationName) {
		String componentName = list.sym.type.getTypeArguments().get(0).toString();
		JCVariableDecl param = maker.VarDef(maker.Modifiers(0), classnode.toName("parent"), maker.Ident(classnode.toName(componentName)), null);
		JCMethodInvocation addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(list.name), classnode.toName("add")), List.<JCExpression>of(maker.Ident(param.name)));
		JCBlock body = maker.Block(0, List.<JCStatement>of(maker.Exec(addcall)));
		JCMethodDecl method = maker.MethodDef(maker.Modifiers(Flags.PUBLIC), classnode.toName("add"), maker.TypeIdent(lombok.javac.Javac.CTC_VOID), List.<JCTypeParameter>nil(), List.<JCVariableDecl>of(param), List.<JCExpression>nil(), body, null);
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

	private JCExpression handleArrayType(JavacNode node, JavacTreeMaker maker, Class<?> clazz) {
		int n = 0;
		while (clazz.isArray()) {
			clazz = clazz.getComponentType();
			n++;
		}

		// String typeName = clazz.isArray() ?
		// clazz.getComponentType().getName() : ;
		JCExpression type = JediJavacUtil.genTypeRef(node.up(), clazz.getName());
		
		while (n > 0) {
			type = maker.TypeArray(type);
			n--;
		}
		
		// if(clazz.isArray())
		// type = maker.TypeArray(type);
		return type;
	}
}
