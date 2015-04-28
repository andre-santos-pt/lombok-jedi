/*
 * Copyright (C) 2010-2015 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.javac.handlers;

import static lombok.javac.Javac.CTC_BOOLEAN;
import lombok.VisitableType;
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
public class HandleVisitableType extends JavacAnnotationHandler<VisitableType> {
	
	
	@Override public void handle(AnnotationValues<VisitableType> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		JCClassDecl visitorInterface = maker.ClassDef(
				maker.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.ABSTRACT), 
				typeNode.toName(annotation.getInstance().visitorTypeName()),
				List.<JCTypeParameter>nil(),
				null, 
				List.<JCExpression>nil(), 
				List.<JCTree>nil());
		
		JavacNode visitorType = JediJavacUtil.injectType(typeNode, visitorInterface);
		
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
		Type type = clazz.sym.type;
		
		if(!HandleVisitableNode.isAbstractType(typeNode))
			addVisitMethod(maker, typeNode, visitorType, type);
		
		for(Type s : HandleVisitableNode.getVisitorNodes(type.toString()))
			addVisitMethod(maker, typeNode, visitorType, s);
		
		String visitorTypeName = type.toString() + "." + annotation.getInstance().visitorTypeName();
		HandleVisitableNode.injectAcceptMethod(typeNode, maker, type, visitorTypeName);
	}
	
	
	
	private void addVisitMethod(JavacTreeMaker maker, JavacNode parent, JavacNode visitorType, Type s) {
		JCVariableDecl param = maker.VarDef(maker.Modifiers(Flags.PARAMETER),
				parent.toName("node"), 
//				JavacHandlerUtil.chainDotsString(visitorType, s.toString()),
				maker.Ident(visitorType.toName(s.toString())),
				null);		
		
		JCStatement returnStatement = maker.Return(maker.Literal(CTC_BOOLEAN, 1));
		JCBlock block = maker.Block(0, List.of(returnStatement));
		
		JCMethodDecl visitMethod = maker.MethodDef(
				maker.Modifiers(Flags.PUBLIC),
				visitorType.toName("visit"),
				maker.TypeIdent(Javac.CTC_BOOLEAN), 
				List.<JCTypeParameter>nil(), 
				List.of(param), 
				List.<JCExpression>nil(), 
				block,
				null);
		
		JediJavacUtil.injectMethod(visitorType, visitMethod);
	}
	
	
	//	static Name getVisitorTypeName() {
	//		
	//	}
	
}