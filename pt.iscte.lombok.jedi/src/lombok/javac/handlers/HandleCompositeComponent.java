package lombok.javac.handlers;

import static lombok.javac.Javac.CTC_BOT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import lombok.AccessLevel;
import lombok.CompositeChildren;
import lombok.CompositeComponent;
import lombok.Singleton;
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
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
@HandlerPriority(10)
@ResolutionResetNeeded
@ProviderFor(JavacAnnotationHandler.class)
public class HandleCompositeComponent extends JavacAnnotationHandler<CompositeComponent> {
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	
	@Override public void handle(AnnotationValues<CompositeComponent> annotation, JCAnnotation ast, JavacNode annotationNode) {
		
		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
		Type type = clazz.sym.type;
		JCClassDecl composite =HandleComposite.getComposite(((ClassType)type).toString());
		CompositeComponent annotationInstance=  annotation.getInstance();
	
		String methodname = annotationInstance.methodName();
		if(methodname.equals("")||methodname==null){
			methodname="getParent";
		}
		String fieldName = annotationInstance.fieldName();
		if(fieldName.equals("")||fieldName==null){
			fieldName="parent";
		}
		JavacNode fieldnode=null;
		if(!JediJavacUtil.isInterface(annotationNode.up())){
			fieldnode=createParentField(annotationNode,maker,composite,fieldName,CompositeComponent.class.getName());
			createConstructor(annotationNode,maker,composite,fieldnode,CompositeComponent.class.getName());
			createGetParent(annotationNode,maker,composite,fieldnode,methodname,true,CompositeComponent.class.getName());
		}else{
			createGetParent(annotationNode,maker,composite,fieldnode,methodname,false,CompositeComponent.class.getName());
		}
		
		
		
	}
	private void createConstructor(JavacNode annotationNode, JavacTreeMaker maker, JCClassDecl composite,JavacNode fieldnode,String annotationName) {
		// TODO Auto-generated method stub
		
		ArrayList<List<JCVariableDecl>> constuctors =new ArrayList<List<JCVariableDecl>>();
		boolean haspublic=false;
		JavacNode constructornode=null;
		ListBuffer<JCExpression> thisargs=new ListBuffer<JCExpression>();
		for (JavacNode node : annotationNode.up().down()) {
			if(node.getKind().equals(Kind.METHOD)){
				JCMethodDecl method = ((JCMethodDecl)node.get());
				if(method.restype==null ){
					if(method.mods.flags==Flags.PUBLIC){
						haspublic=true;
						constructornode=node;
					}
					if(method.mods.flags==Flags.PRIVATE){
						constuctors.add(method.getParameters());
					}	
				}
			}	
		}
		
		if(!haspublic){
			if(constuctors.size()>0){
			for (List<JCVariableDecl> parameters : constuctors) {
				createProtectedConstructorBasedOnPrivate(annotationNode, maker, composite,
						fieldnode, thisargs, parameters,annotationName);	
			}
			}else{
				createDefaultProtectedConstructor(annotationNode, maker, composite,
						fieldnode,annotationName);	
				
			}
		}else{
			constructornode.addError("A class @CompositeComponent cannot have manual public constructors.");
		}
		
		
	}
	private void createProtectedConstructorBasedOnPrivate(JavacNode annotationNode,
			JavacTreeMaker maker, JCClassDecl composite, JavacNode fieldnode,
			ListBuffer<JCExpression> thisargs, List<JCVariableDecl> parameters,String annotationName) {
		JCVariableDecl parentclass = (JCVariableDecl) fieldnode.get();
		ListBuffer<JCVariableDecl> args = new ListBuffer<JCVariableDecl>();
		args.add(maker.VarDef(maker.Modifiers(0), annotationNode.toName("parentArg"), maker.Ident(composite.name), null));
		JCAssign assignexpr= maker.Assign( maker.Ident(parentclass.name),maker.Ident(fieldnode.toName("parentArg")));
		//tratar do que recebe
		
		for (JCVariableDecl var : parameters) {
			args.add(maker.VarDef(maker.Modifiers(0), var.name, var.vartype, null));
			thisargs.add(maker.Ident(var.name));
		}
		JCExpression thiscall=maker.Apply(NIL_EXPRESSION, maker.Ident(fieldnode.toName("this")), thisargs.toList());
		JCBlock body= maker.Block(0, List.<JCStatement>of(maker.Exec(thiscall),maker.Exec(assignexpr)));
		JCMethodDecl constructor = JediJavacUtil.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), annotationNode.up(), List.<JavacNode>nil(), null, annotationNode);
		//JCMethodDecl constructor=maker.MethodDef(maker.Modifiers(Flags.PROTECTED),clazz.name , null, 
		//		List.<JCTypeParameter>nil(), args.toList(), List.<JCExpression>nil(),body, null);
		constructor.body=body;
		constructor.mods=maker.Modifiers(Flags.PROTECTED);
		constructor.params=args.toList();
		
		//JCExpression thiscall=maker.Apply(NIL_EXPRESSION,maker.Ident(annotationNode.toName("banana()")), List.<JCExpression>of(maker.Ident(var.name)));
		
		//typeNode.toName("$")
		JediJavacUtil.injectMethod(annotationNode.up(),constructor,annotationName);
	}
	private void createDefaultProtectedConstructor(JavacNode annotationNode,
			JavacTreeMaker maker, JCClassDecl composite, JavacNode fieldnode,String annotationName) {
		JCVariableDecl parentclass = (JCVariableDecl) fieldnode.get();
		JCVariableDecl parent=maker.VarDef(maker.Modifiers(0), annotationNode.toName("parentArg"), maker.Ident(composite.name), null);
		JCMethodDecl constructor = JediJavacUtil.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), annotationNode.up(), List.<JavacNode>nil(), null, annotationNode);
		//JCMethodDecl constructor=maker.MethodDef(maker.Modifiers(Flags.PROTECTED),clazz.name , null, 
		//		List.<JCTypeParameter>nil(), args.toList(), List.<JCExpression>nil(),body, null);
		JCAssign assignexpr= maker.Assign( maker.Ident(parentclass.name),maker.Ident(fieldnode.toName("parentArg")));
		constructor.body=maker.Block(0, List.<JCStatement>of(maker.Exec(assignexpr)));
		constructor.mods=maker.Modifiers(Flags.PROTECTED);
		constructor.params=List.<JCVariableDecl>of(parent);
		
		//JCExpression thiscall=maker.Apply(NIL_EXPRESSION,maker.Ident(annotationNode.toName("banana()")), List.<JCExpression>of(maker.Ident(var.name)));
		
		//typeNode.toName("$")
		JediJavacUtil.injectMethod(annotationNode.up(),constructor,annotationName);
	}
	private void createGetParent(JavacNode node, JavacTreeMaker maker, JCClassDecl composite, JavacNode fieldnode, String methodname,boolean withBody, String annotationName) {
		JCBlock body =null;
		if(withBody){
			JCReturn statement= maker.Return(maker.Ident(node.toName(fieldnode.getName())));
		body= maker.Block(0, List.<JCStatement>of(statement));
		}
		
		JCMethodDecl getParent=maker.MethodDef(maker.Modifiers(Flags.PUBLIC),node.toName(methodname) , maker.Ident(composite.name), 
				List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(),body, null);
		JediJavacUtil.injectMethod(node.up(),getParent,annotationName);
		
	}
	private JavacNode createParentField(JavacNode node, JavacTreeMaker maker, JCClassDecl composite, String fieldName,String annotationName) {
		// TODO Auto-generated method stub
		JCVariableDecl field= maker.VarDef(maker.Modifiers(Flags.PRIVATE), node.toName(fieldName), maker.Ident(composite.name), null);
		return JediJavacUtil.injectField(node.up(),field,annotationName);
		
	}
	
	
}