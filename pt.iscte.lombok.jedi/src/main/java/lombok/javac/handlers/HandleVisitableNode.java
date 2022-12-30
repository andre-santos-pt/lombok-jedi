
package lombok.javac.handlers;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import lombok.Visitor;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import org.mangosdk.spi.ProviderFor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static lombok.javac.Javac.CTC_VOID;

@ProviderFor(JavacAnnotationHandler.class) 
@ResolutionResetNeeded 
@HandlerPriority(2)
public class HandleVisitableNode extends JavacAnnotationHandler<Visitor.Node> {
	
	private static Map<String, Set<Type>> subtypes = new HashMap<String, Set<Type>>();
	public static Set<Type> getVisitorNodes(String rootNode) {
		if (!subtypes.containsKey(rootNode)) return Collections.emptySet();
		else
			return subtypes.get(rootNode);
	}
	
	@Override public void handle(AnnotationValues<Visitor.Node> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		Types types = Types.instance(typeNode.getAst().getContext());
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();			
			Type type = clazz.sym.type;
			List<Type> closure = types.closure(type);
			for (Type s : closure) {
				
				ClassType ct = (ClassType) s;
				Visitor ann = ct.tsym.getAnnotation(Visitor.class);
				
				if (ann != null) {
					//if(!JediJavacUtil.isAbstractType(annotationNode.up())){
						if (!subtypes.containsKey(ct.toString())) 
						subtypes.put(ct.toString(), new HashSet<Type>());
					subtypes.get(ct.toString()).add(type);
					//}
					String visitorType = ct.toString() + "." + ann.visitorTypeName();
					injectAcceptMethod(typeNode, maker, type, visitorType,ann.visitorMethodName(),ann.acceptMethodName(),Visitor.Node.class.getName());
					
				}

			}	
	}

	static void injectAcceptMethod(JavacNode typeNode, JavacTreeMaker maker, Type type, String visitorType,String visitorMethodName,String acceptVMethodName,String annotationName) {
		boolean abstractType = JediJavacUtil.isAbstractType(typeNode);
		boolean InterfaceTyoe= JediJavacUtil.isInterface(typeNode);
		Types types = Types.instance(typeNode.getAst().getContext());
		JCVariableDecl param = maker.VarDef(maker.Modifiers(Flags.PARAMETER), typeNode.toName("visitor"), JediJavacUtil.chainDotsString(typeNode, visitorType),
		// maker.Ident(visitorType),
				null);
		
		JCBlock bodyBlock = null;
		
		if (!abstractType && !InterfaceTyoe) {
			JCExpression callVisit = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(param.getName()), typeNode.toName(visitorMethodName)), List.<JCExpression>of(maker.Ident(typeNode.toName("this"))));
			
			JCStatement statement = null;
			
			if (HandleVisitableChildren.hasChildren(type.toString())) {
				java.util.List<JCStatement> statementsList = new ArrayList<JCStatement>();
				Name itVarName = typeNode.toName("_child_");
				
				for (JCVariableDecl field : HandleVisitableChildren.getChildrenVariables(type.toString())) {
					
					List<Type> closure = types.closure(field.sym.type);
					if (!HandleCompositeChildren.iscollection(closure)) {
						JCExpression callAccept = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(field.name), typeNode.toName(acceptVMethodName)), List.<JCExpression>of(maker.Ident(typeNode.toName("visitor"))));
						statementsList.add(maker.Exec(callAccept));
					}else{
						Type collectionType = field.sym.type.getTypeArguments().get(0);
						JCVariableDecl var = maker.VarDef(maker.Modifiers(0), itVarName, maker.Ident(typeNode.toName(collectionType.toString())), null);
						JCExpression callAccept = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(itVarName), typeNode.toName(acceptVMethodName)), List.<JCExpression>of(maker.Ident(typeNode.toName("visitor"))));
						
						JCEnhancedForLoop loop = maker.ForeachLoop(var, maker.Ident(field), maker.Block(0, List.<JCStatement>of(maker.Exec(callAccept))));
						
						statementsList.add(loop);	
					}
					
				}
				statement = maker.If(callVisit, maker.Block(0, List.<JCStatement>from(statementsList.toArray(new JCStatement[statementsList.size()]))), null);
			} else {
				statement = maker.Exec(callVisit);
			}
			
			bodyBlock = maker.Block(0, List.<JCStatement>of(statement));
		}
		
		JCMethodDecl acceptMethod =JediJavacUtil.createMethod(maker, maker.Modifiers(abstractType ? Flags.PUBLIC | Flags.ABSTRACT : Flags.PUBLIC), typeNode.toName(acceptVMethodName), maker.TypeIdent(CTC_VOID),  List.of(param), bodyBlock);
		JediJavacUtil.injectMethod(typeNode, acceptMethod,annotationName);
	}
}
