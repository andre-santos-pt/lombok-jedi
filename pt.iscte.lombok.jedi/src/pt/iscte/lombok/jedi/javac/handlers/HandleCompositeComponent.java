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
package pt.iscte.lombok.jedi.javac.handlers;

import static lombok.javac.handlers.JavacHandlerUtil.*;
import lombok.AccessLevel;
import lombok.CompositeComponent;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
@HandlerPriority(10)
@ProviderFor(JavacAnnotationHandler.class)
public class HandleCompositeComponent extends JavacAnnotationHandler<CompositeComponent> {
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	
	@Override public void handle(AnnotationValues<CompositeComponent> annotation, JCAnnotation ast, JavacNode annotationNode) {
		
		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
		Type type = clazz.sym.type;
		JCClassDecl composite =HandleComposite.getComposite(((ClassType)type).toString());
		
		JavacNode fieldnode=createParentField(annotationNode,maker,composite);
		createGetParent(annotationNode,maker,composite,fieldnode);
		createConstructor(annotationNode,maker,composite,fieldnode);
	}
	private void createConstructor(JavacNode annotationNode, JavacTreeMaker maker, JCClassDecl composite,JavacNode fieldnode) {
		// TODO Auto-generated method stub
		List<JCVariableDecl> parameters = null;
		ListBuffer<JCExpression> thisargs=new ListBuffer<JCExpression>();
		for (JavacNode node : annotationNode.up().down()) {
			if(node.getKind().equals(Kind.METHOD)){
				JCMethodDecl method = ((JCMethodDecl)node.get());
				if(method.restype==null ){
					if(method.mods.flags==Flags.PRIVATE){
						parameters = method.getParameters();	
					}else{
						//erro
						node.addError("The constructor modifier must be private.");
					}
					
				}
				
			}	
		}
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
		injectMethod(annotationNode.up(),constructor);
	}
	private void createGetParent(JavacNode node, JavacTreeMaker maker, JCClassDecl composite, JavacNode fieldnode) {
		JCReturn statement= maker.Return(maker.Ident(node.toName(fieldnode.getName())));
		JCBlock body= maker.Block(0, List.<JCStatement>of(statement));
		JCMethodDecl getParent=maker.MethodDef(maker.Modifiers(Flags.PUBLIC),node.toName("getParent") , maker.Ident(composite.name), 
				List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(),body, null);
		injectMethod(node.up(),getParent);
		
	}
	private JavacNode createParentField(JavacNode node, JavacTreeMaker maker, JCClassDecl composite) {
		// TODO Auto-generated method stub
		JCVariableDecl field= maker.VarDef(maker.Modifiers(Flags.PRIVATE), node.toName("Parent"), maker.Ident(composite.name), null);
		return injectField(node.up(),field);
		
	}
	
	
}