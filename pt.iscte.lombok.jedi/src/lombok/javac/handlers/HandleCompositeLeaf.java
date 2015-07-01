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

import static lombok.javac.Javac.CTC_BOT;

import java.util.HashMap;
import java.util.Map;

import lombok.Composite;
import lombok.Composite.Children;
import lombok.Composite.Component;
import lombok.Composite;
import lombok.core.AST.Kind;
import lombok.core.configuration.NullCheckExceptionType;
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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;


@HandlerPriority(10)
@ResolutionResetNeeded
@ProviderFor(JavacAnnotationHandler.class)
public class HandleCompositeLeaf extends JavacAnnotationHandler<Composite.Leaf> {



	@Override public void handle(AnnotationValues<Composite.Leaf> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JavacTreeMaker maker = typeNode.getTreeMaker();
		Types types = Types.instance(typeNode.getAst().getContext());
		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
		Type type = clazz.sym.type;
		if(!JediJavacUtil.isInterface(annotationNode.up())){
			List<Type> closure = types.closure(type);
			
			for (Type s : closure) {
				ClassType ct = (ClassType) s;
				Composite.Component ann = ct.tsym.getAnnotation(Composite.Component.class);
				if (ann != null) {
					injectOnConstructor(maker, annotationNode, s, annotationNode.up(), ct);
				}
				
			}	
		}
		


	}
	
	private void injectOnConstructor(JavacTreeMaker maker, JavacNode classnode, Type s, JavacNode typeNode, ClassType ct) {
		// TODO Auto-generated method stub
		
		boolean existsPublic=false;
		for (JavacNode node : classnode.up().down()) {
			if (node.getKind().equals(Kind.METHOD)) {
				JCMethodDecl method = ((JCMethodDecl) node.get());
				if (method.restype == null) {
					if(method.mods.flags==Flags.PUBLIC){
						existsPublic=true;
						String compositeName=getParent(ct.toString());
						String methodName=drillCompositeForMethodAddName(ct.toString());
						if(methodName==null){
							node.addError("The upperClass "+ct.toString()+" must contain a field annotated with "+Composite.Children.class);
						}else
						if (HandleCompositeChildren.hasParentArgument(method.getParameters(),compositeName )) {
							JCExpression cond = maker.Binary(lombok.javac.Javac.CTC_EQUAL, maker.Ident(method.params.get(0).name), maker.Literal(CTC_BOT, null));
							JCExpression exceptionType = JediJavacUtil.genTypeRef(node, NullPointerException.class.getName());
							JCExpression message = maker.Literal("This class must have a parent.");
							JCExpression exceptionInstance = maker.NewClass(null, List.<JCExpression>nil(), exceptionType, List.<JCExpression>of(message), null);
							JCStatement statement = maker.If(cond, maker.Throw(exceptionInstance), null);
							JCExpression addcall = maker.Apply(List.<JCExpression>nil(), maker.Select(maker.Ident(method.params.get(0).name), node.toName(methodName)), List.<JCExpression>of(maker.Ident(node.toName("this"))));
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
			classnode.up().addError("This Class must have a public constructor");
		}
	}
	
	private String getParent(String rootnode) {
		return HandleComposite.getComposite(rootnode).name.toString();
	}
	private String drillCompositeForMethodAddName(String rootnode){
		JCClassDecl clazz = HandleComposite.getComposite(rootnode);
		JCMethodDecl method;
		for (JCTree member : clazz.getMembers()) {
			
			if(member.getKind()==com.sun.source.tree.Tree.Kind.METHOD){
				method=((JCMethodDecl)member);

			for (JCStatement statement :method.getBody().stats ) {
				if(statement.getKind().equals(com.sun.source.tree.Tree.Kind.BLOCK)){
					for (JCStatement blockItem : ((JCBlock)statement).stats) {
						
						if(blockItem.getKind().equals(com.sun.source.tree.Tree.Kind.IF)){
							JCIf ifexpr=(JCIf)blockItem.getTree(); 
							if(ifexpr.thenpart.getKind().equals(com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT)){
							
							JCExpressionStatement call=(JCExpressionStatement)ifexpr.thenpart.getTree();

							if(call.expr.getKind().equals(com.sun.source.tree.Tree.Kind.METHOD_INVOCATION)){
								JCMethodInvocation methodName= (JCMethodInvocation)call.expr;
								
								return JediJavacUtil.removePrefixFromString(methodName.meth.toString());
							}
							}
							
						}
						
					}
				}
				
			}
			}
		}
		return null;
	}
}