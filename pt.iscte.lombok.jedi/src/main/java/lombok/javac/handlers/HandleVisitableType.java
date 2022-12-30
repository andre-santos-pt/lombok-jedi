
package lombok.javac.handlers;

import static lombok.javac.Javac.CTC_BOOLEAN;
import lombok.Singleton;
import lombok.Visitor;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;

@ProviderFor(JavacAnnotationHandler.class)
@ResolutionResetNeeded
@HandlerPriority(3)
public class HandleVisitableType extends JavacAnnotationHandler<Visitor> {
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	private static final List<JCTree> NIL_TREE= List.nil();
	
	@Override public void handle(AnnotationValues<Visitor> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		Visitor annotationInstance=  annotation.getInstance();
		String visitorMethodNname = annotationInstance.visitorMethodName();
		String acceptMethodName= annotationInstance.acceptMethodName();

		
		JCClassDecl visitorInterface = maker.ClassDef(
				maker.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.ABSTRACT), 
				typeNode.toName(annotation.getInstance().visitorTypeName()),
				List.<JCTypeParameter>nil(),
				null, 
				NIL_EXPRESSION, 
				NIL_TREE);
		String annotationName= Visitor.class.getName();
		JavacNode visitorType = JediJavacUtil.injectType(typeNode, visitorInterface,annotationName);
		
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
		Type type = clazz.sym.type;
		
		if(!JediJavacUtil.isAbstractType(typeNode) || JediJavacUtil.isInterface(typeNode))
			addVisitMethod(maker, typeNode, visitorType, type,visitorMethodNname,annotationName);
		
		for(Type s : HandleVisitableNode.getVisitorNodes(type.toString()))
			addVisitMethod(maker, typeNode, visitorType, s,visitorMethodNname,annotationName);
		
		String visitorTypeName = type.toString() + "." + annotation.getInstance().visitorTypeName();

		HandleVisitableNode.injectAcceptMethod(typeNode, maker, type, visitorTypeName,visitorMethodNname,acceptMethodName,annotationName);
	}
	
	
	
	private void addVisitMethod(JavacTreeMaker maker, JavacNode parent, JavacNode visitorType, Type s,String visitMethodName,String annotationName) {
		JCVariableDecl param = maker.VarDef(maker.Modifiers(Flags.PARAMETER),
				parent.toName("node"), 
				maker.Ident(visitorType.toName(s.toString())),
				null);		
		
		JCStatement returnStatement = maker.Return(maker.Literal(CTC_BOOLEAN, 1));
		JCBlock block = maker.Block(0, List.of(returnStatement));
		JCMethodDecl visitMethod = JediJavacUtil.createMethod(maker, maker.Modifiers(Flags.PUBLIC), visitorType.toName(visitMethodName), maker.TypeIdent(Javac.CTC_BOOLEAN), List.of(param), block);
		
		JediJavacUtil.injectMethod(visitorType, visitMethod,annotationName);
	}
	
}