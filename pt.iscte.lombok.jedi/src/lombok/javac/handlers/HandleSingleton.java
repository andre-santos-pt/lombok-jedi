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
package lombok.javac.handlers;

import static lombok.javac.Javac.CTC_BOT;
import static lombok.javac.Javac.CTC_EQUAL;
import static lombok.javac.handlers.JavacHandlerUtil.injectField;
import static lombok.javac.handlers.JavacHandlerUtil.injectMethod;
import static lombok.javac.handlers.JavacHandlerUtil.recursiveSetGeneratedBy;
import static lombok.javac.handlers.JavacHandlerUtil.removePrefixFromField;
import lombok.AccessLevel;
import lombok.Observable;
import lombok.Singleton;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.HandleConstructor;
import lombok.javac.handlers.HandleData;

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


@ProviderFor(JavacAnnotationHandler.class)
public class HandleSingleton extends JavacAnnotationHandler<Singleton> {

	private static final List<JCExpression> NIL_EXPRESSION = List.nil();

	
	
	@Override public void handle(AnnotationValues<Singleton> annotation, JCAnnotation ast, JavacNode node) {
		boolean haspublic = false;
		JavacTreeMaker maker = node.up().getTreeMaker();
		if(((JCClassDecl)node.up().get()).sym.isInterface()){
			node.addError("@Singleton can not be used on Interfaces.");
		}
		if(HandleVisitableNode.isAbstractType(node.up())){
			node.addError("@Singleton can not be used on Abstract classes.");
		}
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
			Singleton annotationInstance=  annotation.getInstance();
			String methodname = annotationInstance.methodName();
			if("".equals(methodname)){
				methodname="getInstance";
			}
			String fieldName = annotationInstance.fieldName();
			if(fieldName.equals("")||fieldName==null){
				fieldName="_instance";
			}
			JCClassDecl clazz = (JCClassDecl)node.up().get();
			JavacNode fieldNode = createLocalField(node, maker, clazz,fieldName);
			createConstructor(node, maker);
			createGetMethod(node, maker, clazz, fieldNode,methodname);	
		}else{
			
		}
		
		
		
	}


	private JavacNode createLocalField(JavacNode node, JavacTreeMaker maker, JCClassDecl clazz, String fieldName) {
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.STATIC), node.toName(fieldName),maker.Ident(clazz.name), null);
		//JCVariableDecl uncleanField = maker.VarDef(maker.Modifiers(Flags.PRIVATE), x.name, cloneType(maker, x.defs, (JCTree)ast, node.getContext()), null);
		//JCVariableDecl uncleanField = maker.VarDef(maker.Modifiers(Flags.PRIVATE), node.toName("$lombokUnclean"), maker.TypeIdent(Javac.CTC_BOOLEAN), null);
		JavacNode fieldNode = injectField(node.up(), field);
		return fieldNode;
	}


	public static void createConstructor(JavacNode node, JavacTreeMaker maker) {
		JCMethodDecl constructor = HandleConstructor.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), node.up(), List.<JavacNode>nil(), null, node);
		constructor.mods=maker.Modifiers(Flags.PRIVATE);
		injectMethod(node.up(), constructor);
	}


	private void createGetMethod(JavacNode node, JavacTreeMaker maker, JCClassDecl x, JavacNode fieldNode,String methodname) {
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

		
		JCMethodDecl decl = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC|Flags.STATIC|Flags.SYNCHRONIZED), node.toName(methodname) , maker.Ident(x.name),
				List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), body, null), node.up().get(), node.getContext());
		
		injectMethod(node.up(), decl);
	}
}
