/*
 * Copyright (C) 2009-2014 The Project Lombok Authors.
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

import static lombok.javac.Javac.CTC_BOT;
import static lombok.javac.Javac.CTC_EQUAL;
import static lombok.javac.handlers.JavacHandlerUtil.injectField;
import static lombok.javac.handlers.JavacHandlerUtil.injectMethod;
import static lombok.javac.handlers.JavacHandlerUtil.recursiveSetGeneratedBy;
import static lombok.javac.handlers.JavacHandlerUtil.removePrefixFromField;

import lombok.AccessLevel;
import lombok.Singleton;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.HandleConstructor;
import lombok.javac.handlers.HandleData;
import lombok.javac.handlers.JavacHandlerUtil.FieldAccess;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

/**
 * Handles the {@code lombok.Getter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleSingleton extends JavacAnnotationHandler<Singleton> {

	private static final List<JCExpression> NIL_EXPRESSION = List.nil();

	
	/**
	 * Generates a getter on the stated field.
	 * 
	 * Used by {@link HandleData}.
	 * 
	 * The difference between this call and the handle method is as follows:
	 * 
	 * If there is a {@code lombok.Getter} annotation on the field, it is used and the
	 * same rules apply (e.g. warning if the method already exists, stated access level applies).
	 * If not, the getter is still generated if it isn't already there, though there will not
	 * be a warning if its already there. The default access level is used.
	 * 
	 * @param fieldNode The node representing the field you want a getter for.
	 * @param pos The node responsible for generating the getter (the {@code @Data} or {@code @Getter} annotation).
	 */

	
	@Override public void handle(AnnotationValues<Singleton> annotation, JCAnnotation ast, JavacNode node) {
		boolean haspublic = false;
		JavacTreeMaker maker = node.up().getTreeMaker();
		for (JavacNode subnode : node.up().down()) {
			if(subnode.getKind().equals(Kind.METHOD)){
 				JCMethodDecl method = ((JCMethodDecl)subnode.get());
 				if(method.restype==null ){
 					if(method.mods.flags==Flags.PUBLIC){
 						haspublic=true;
 						subnode.addError("Class annotated with @Singleton cannot have a public constructor.");
 					}
 					
 				}
 					
 			}	
		}
		if(!haspublic){
			JCClassDecl clazz = (JCClassDecl)node.up().get();
			JavacNode fieldNode = createLocalField(node, maker, clazz);
			createConstructor(node, maker);
			createGetMethod(node, maker, clazz, fieldNode);	
		}else{
			
		}
		
		
		
	}


	private JavacNode createLocalField(JavacNode node, JavacTreeMaker maker, JCClassDecl clazz) {
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.STATIC), node.toName("_instance"),maker.Ident(clazz.name), null);
		//JCVariableDecl uncleanField = maker.VarDef(maker.Modifiers(Flags.PRIVATE), x.name, cloneType(maker, x.defs, (JCTree)ast, node.getContext()), null);
		//JCVariableDecl uncleanField = maker.VarDef(maker.Modifiers(Flags.PRIVATE), node.toName("$lombokUnclean"), maker.TypeIdent(Javac.CTC_BOOLEAN), null);
		JavacNode fieldNode = injectField(node.up(), field);
		return fieldNode;
	}


	private void createConstructor(JavacNode node, JavacTreeMaker maker) {
		JCMethodDecl constructor = HandleConstructor.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), node.up(), List.<JavacNode>nil(), null, node);
		constructor.mods=maker.Modifiers(Flags.PRIVATE);
		injectMethod(node.up(), constructor);
	}


	private void createGetMethod(JavacNode node, JavacTreeMaker maker, JCClassDecl x, JavacNode fieldNode) {
		JCVariableDecl field = (JCVariableDecl)fieldNode.get();
		Name fieldName = removePrefixFromField(fieldNode);
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
		JCExpression cond = maker.Binary(CTC_EQUAL, maker.Ident(field.name), maker.Literal(CTC_BOT, null));
		
	
		
//		JCMethodInvocation newCall = maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(x.name), node.up().toName("new")), NIL_EXPRESSION);
		
		JCNewClass newCall = maker.NewClass(null, List.<JCExpression>nil(), maker.Ident(x.name), NIL_EXPRESSION, null);
		
		JCAssign assign = maker.Assign(maker.Ident(fieldName), newCall);
		//JCBlock then = maker.Block(0, assigns);
		JCBlock then = maker.Block(0, List.<JCStatement>of(maker.Exec(assign)));
		JCIf ifStat = maker.If(cond, then, null);
		
		statements.add(ifStat);
		statements.add(maker.Return(maker.Ident(field.name)));
		//assigns.append(maker.Exec(assign));
		//statements.append(maker.Exec(assign));

		//statements.append(maker.Exec(assign));
		JCBlock body = maker.Block(0, statements.toList());
		//JCVariableDecl field = (JCVariableDecl) fieldNode.get();

		
		JCMethodDecl decl = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC|Flags.STATIC|Flags.SYNCHRONIZED), node.toName("getInstance") , maker.Ident(x.name),
				List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), body, null), node.up().get(), node.getContext());
		
		injectMethod(node.up(), decl);
	}
}
