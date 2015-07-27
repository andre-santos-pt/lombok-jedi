/*
 * Copyright 2009-2014 The Project Lombok Authors.
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
import static lombok.javac.Javac.CTC_EQUAL;
import lombok.AccessLevel;
import lombok.Observable;
import lombok.Singleton;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
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
@ResolutionResetNeeded
public class HandleSingleton extends JavacAnnotationHandler<Singleton> {

	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	private static final List<JCVariableDecl> NIL_VARIABLEDECL= List.nil();
	
	@Override public void handle(AnnotationValues<Singleton> annotation, JCAnnotation ast, JavacNode node) {
		boolean haspublic = false;
		JavacTreeMaker maker = node.up().getTreeMaker();
		if(((JCClassDecl)node.up().get()).sym.isInterface()){
			node.addError("@Singleton can not be used on Interfaces.");
		}
		if(JediJavacUtil.isAbstractType(node.up())){
			node.addError("@Singleton can not be used on Abstract classes.");
		}
		for (JavacNode subnode : node.up().down()) {
			if(subnode.getKind().equals(Kind.METHOD)){
 				JCMethodDecl method = ((JCMethodDecl)subnode.get());
 				if(method.restype==null ){
 					if(method.mods.flags==Flags.PUBLIC){
 						haspublic=true;
 						subnode.addError("Class annotated with @Singleton cannot any public constructor.");
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
			JavacNode fieldNode = createLocalField(node, maker, clazz,fieldName,Singleton.class.getName());
			createConstructor(node, maker,Singleton.class.getName());
			createGetMethod(node, maker, clazz, fieldNode,methodname,Singleton.class.getName());	
		}
	}


	private JavacNode createLocalField(JavacNode node, JavacTreeMaker maker, JCClassDecl clazz, String fieldName,String annotationName) {
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.STATIC), node.toName(fieldName),maker.Ident(clazz.name), null);
		JavacNode fieldNode = JediJavacUtil.injectField(node.up(), field,annotationName);
		return fieldNode;
	}


	public static void createConstructor(JavacNode node, JavacTreeMaker maker,String annotationName) {
		JCMethodDecl constructor = HandleConstructor.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), node.up(), List.<JavacNode>nil(), null, node);
		constructor.mods=maker.Modifiers(Flags.PRIVATE);
		JediJavacUtil.injectMethod(node.up(), constructor,annotationName);
	}


	private void createGetMethod(JavacNode node, JavacTreeMaker maker,JCClassDecl clazz, JavacNode fieldNode,String methodName,String annotationName) {
		JCVariableDecl field = (JCVariableDecl)fieldNode.get();
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
		JCExpression cond = maker.Binary(CTC_EQUAL, maker.Ident(field.name), maker.Literal(CTC_BOT, null));
		JCNewClass newCall = maker.NewClass(null, NIL_EXPRESSION, maker.Ident(clazz.name), NIL_EXPRESSION, null);
		JCAssign assign = maker.Assign(maker.Ident(field.getName()), newCall);
		JCBlock then = maker.Block(0, List.<JCStatement>of(maker.Exec(assign)));
		JCIf ifStat = maker.If(cond, then, null);
		
		statements.add(ifStat);
		statements.add(maker.Return(maker.Ident(field.name)));
		JCBlock body = maker.Block(0, statements.toList());

		
	JCMethodDecl decl=JediJavacUtil.createMethod(maker,maker.Modifiers(Flags.PUBLIC|Flags.STATIC|Flags.SYNCHRONIZED),node.toName(methodName),maker.Ident(clazz.name),NIL_VARIABLEDECL,body);
		JediJavacUtil.injectMethod(node.up(), decl,annotationName);
	}
}
