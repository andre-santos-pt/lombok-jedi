/*
 * Copyright 2010-2015 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above Copyrightice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR CopyrightDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.javac.handlers;

import static lombok.javac.Javac.CTC_VOID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.CompositeChildren;
import lombok.VisitableChildren;
import lombok.VisitableNode;
import lombok.VisitableType;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.core.AST.Kind;
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
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

@ProviderFor(JavacAnnotationHandler.class) 
@ResolutionResetNeeded 
@HandlerPriority(2)
public class HandleVisitableNode extends JavacAnnotationHandler<VisitableNode> {
	
	private static Map<String, Set<Type>> subtypes = new HashMap<String, Set<Type>>();
	public static Set<Type> getVisitorNodes(String rootNode) {
		if (!subtypes.containsKey(rootNode)) return Collections.emptySet();
		else
			return subtypes.get(rootNode);
	}
	
	@Override public void handle(AnnotationValues<VisitableNode> annotation, JCAnnotation ast, JavacNode annotationNode) {

		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		Types types = Types.instance(typeNode.getAst().getContext());
		
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
		int count=0;
		for (JavacNode subnode : annotationNode.up().down()) {
			if(subnode.getKind().equals(Kind.FIELD)){
				for (JavacNode fieldannotations : subnode.down()) {
					if(fieldannotations.getKind().equals(Kind.ANNOTATION)){
						JCAnnotation ann= (JCAnnotation)fieldannotations.get();
						if(ann.type.toString().equals(VisitableChildren.class.getName())){
							count++;
							
						}



					}


				}

			}
		}
		if(count>1){
			typeNode.addError("Cannot have more than one field annotated with @"+VisitableChildren.class.getSimpleName());
		}else
		if(!clazz.sym.isInterface()){
			
			Type type = clazz.sym.type;
			
			List<Type> closure = types.closure(type);
			for (Type s : closure) {
				
				ClassType ct = (ClassType) s;
				VisitableType ann = ct.tsym.getAnnotation(VisitableType.class);
				
				if (ann != null) {
					if (!subtypes.containsKey(ct.toString())) 
						subtypes.put(ct.toString(), new HashSet<Type>());
					subtypes.get(ct.toString()).add(type);
					String visitorType = ct.toString() + "." + ann.visitorTypeName();
					injectAcceptMethod(typeNode, maker, type, visitorType,VisitableNode.class.getName());
				}

			}	
		}else{
			annotationNode.addError("Cannot be used on Interfaces.");
		}
		
		
	}

	static void injectAcceptMethod(JavacNode typeNode, JavacTreeMaker maker, Type type, String visitorType,String annotationName) {
		boolean abstractType = isAbstractType(typeNode);
		Types types = Types.instance(typeNode.getAst().getContext());
		JCVariableDecl param = maker.VarDef(maker.Modifiers(Flags.PARAMETER), typeNode.toName("visitor"), JediJavacUtil.chainDotsString(typeNode, visitorType),
		// maker.Ident(visitorType),
				null);
		
		JCBlock bodyBlock = null;
		
		if (!abstractType) {
			JCExpression callVisit = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(param.getName()), typeNode.toName("visit")), List.<JCExpression>of(maker.Ident(typeNode.toName("this"))));
			
			JCStatement statement = null;
			
			if (HandleVisitableChildren.hasChildren(type.toString())) {
				java.util.List<JCStatement> statementsList = new ArrayList<JCStatement>();
				Name itVarName = typeNode.toName("_child_");
				
				for (JCVariableDecl field : HandleVisitableChildren.getChildrenVariables(type.toString())) {
					
					List<Type> closure = types.closure(field.sym.type);
					if (!HandleCompositeChildren.iscollection(closure)) {
						JCExpression callAccept = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(field.name), typeNode.toName("accept")), List.<JCExpression>of(maker.Ident(typeNode.toName("visitor"))));
						statementsList.add(maker.Exec(callAccept));
					}else{
						Type collectionType = field.sym.type.getTypeArguments().get(0);
						JCVariableDecl var = maker.VarDef(maker.Modifiers(0), itVarName, maker.Ident(typeNode.toName(collectionType.toString())), null);
						// JCVariableDecl var = maker.VarDef(maker.Modifiers(0),
						// itVarName, maker.Type(collectionType), null);
						JCExpression callAccept = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(itVarName), typeNode.toName("accept")), List.<JCExpression>of(maker.Ident(typeNode.toName("visitor"))));
						
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
		
		JCMethodDecl acceptMethod = maker.MethodDef(maker.Modifiers(abstractType ? Flags.PUBLIC | Flags.ABSTRACT : Flags.PUBLIC), typeNode.toName("accept"),
		// maker.Type(Javac.createVoidType(maker, CTC_VOID)),
				maker.TypeIdent(CTC_VOID), List.<JCTypeParameter>nil(), List.of(param), List.<JCExpression>nil(), bodyBlock, null);
		
		JediJavacUtil.injectMethod(typeNode, acceptMethod,annotationName);
	}
	
	static boolean isAbstractType(JavacNode typeNode) {
		JCClassDecl clazz = (JCClassDecl) typeNode.get();
		boolean abstractType = clazz.sym.type.isInterface() || (clazz.getModifiers().flags & Flags.ABSTRACT) == Flags.ABSTRACT;
		return abstractType;
	}
	
}