package pt.iscte.lombok.jedi.javac.handlers;

import static lombok.javac.Javac.CTC_BOT;
import static lombok.javac.handlers.JavacHandlerUtil.genTypeRef;
import lombok.CompositeComponent;
import lombok.Composite;
import lombok.CompositeChildren;
import lombok.CompositeLeaf;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;

@HandlerPriority(10) @ProviderFor(JavacAnnotationHandler.class) public class HandleCompositeLeaf extends JavacAnnotationHandler<CompositeLeaf> {
	@Override public void handle(AnnotationValues<CompositeLeaf> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		Types types = Types.instance(typeNode.getAst().getContext());
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
		Type type = clazz.sym.type;
		
		List<Type> closure = types.closure(type);
		
		for (Type s : closure) {
			ClassType ct = (ClassType) s;
			CompositeComponent ann = ct.tsym.getAnnotation(CompositeComponent.class);
			if (ann != null) {
				injectOnConstructor(maker, annotationNode.up(), s, annotationNode.up(), ct);
			}
			// injectOnConstructor(maker,typenode);
			
		}
	}
	
	private void injectOnConstructor(JavacTreeMaker maker, JavacNode classnode, Type s, JavacNode typeNode, ClassType ct) {
		// TODO Auto-generated method stub
		
		boolean existsPublic=false;
		for (JavacNode node : classnode.down()) {
			if (node.getKind().equals(Kind.METHOD)) {
				JCMethodDecl method = ((JCMethodDecl) node.get());
				if (method.restype == null) {
					if(method.mods.flags==Flags.PUBLIC){
						existsPublic=true;
						String compositeName=getParent(ct.toString());
						if (HandleCompositeChildren.hasParentArgument(method.getParameters(),compositeName )) {
							JCExpression cond = maker.Binary(lombok.javac.Javac.CTC_EQUAL, maker.Ident(method.params.get(0).name), maker.Literal(CTC_BOT, null));
							JCExpression exceptionType = genTypeRef(node, NullPointerException.class.getName());
							JCExpression message = maker.Literal("This class must have a parent.");
							JCExpression exceptionInstance = maker.NewClass(null, List.<JCExpression>nil(), exceptionType, List.<JCExpression>of(message), null);
							JCStatement statement = maker.If(cond, maker.Throw(exceptionInstance), null);
							JCExpression addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(method.params.get(0).name), node.toName("add")), List.<JCExpression>of(maker.Ident(node.toName("this"))));
							JCBlock block = maker.Block(0L, List.<JCStatement>of(statement, maker.Exec(addcall)));
							// method.body.stats=method.body.stats.append(block);
							method.body.stats = method.body.stats.append(block);
							node.rebuild();
						}else{
							node.addError("Public constructor must have a argument of type "+compositeName);
						}
					}
					
					
				}
			}
		}
		if(!existsPublic){
			classnode.addError("This Class must have a public constructor");
		}
	}
	
	private String getParent(String rootnode) {
		return HandleComposite.getComposite(rootnode).name.toString();
	}
}
