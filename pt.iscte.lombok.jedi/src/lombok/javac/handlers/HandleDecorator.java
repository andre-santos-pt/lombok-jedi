package lombok.javac.handlers;

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

import static lombok.javac.Javac.CTC_BOOLEAN;
import static lombok.javac.Javac.CTC_BYTE;
import static lombok.javac.Javac.CTC_CHAR;
import static lombok.javac.Javac.CTC_DOUBLE;
import static lombok.javac.Javac.CTC_FLOAT;
import static lombok.javac.Javac.CTC_INT;
import static lombok.javac.Javac.CTC_LONG;
import static lombok.javac.Javac.CTC_SHORT;
import static lombok.javac.Javac.CTC_VOID;
import static lombok.javac.handlers.JavacHandlerUtil.injectField;
import static lombok.javac.handlers.JavacHandlerUtil.injectMethod;
import static lombok.javac.handlers.JavacHandlerUtil.injectType;
import static lombok.javac.handlers.JavacHandlerUtil.recursiveSetGeneratedBy;
import static lombok.javac.handlers.JavacHandlerUtil.removePrefixFromField;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;

import lombok.AccessLevel;
import lombok.Decorator;
import lombok.VisitableType;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.JavacTreeMaker.TypeTag;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;


@ProviderFor(JavacAnnotationHandler.class) 
@HandlerPriority(12)
public class HandleDecorator extends JavacAnnotationHandler<Decorator> {
	
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	
	public TypeTag handlePrimitiveType(String type) {
		
		if (type.equals(int.class.getName())) {
			return CTC_INT;
		}
		
		if (type.equals(double.class.getName())) {
			return CTC_DOUBLE;
		}
		
		if (type.equals(float.class.getName())) {
			return CTC_FLOAT;
		}
		
		if (type.equals(short.class.getName())) {
			return CTC_SHORT;
		}
		
		if (type.equals(byte.class.getName())) {
			return CTC_BYTE;
		}
		
		if (type.equals(long.class.getName())) {
			return CTC_LONG;
		}
		
		if (type.equals(boolean.class.getName())) {
			return CTC_BOOLEAN;
		}
		
		if (type.equals(char.class.getName())) {
			return CTC_CHAR;
		}
		if (type.equals("void")) {
			return CTC_VOID;
		}
		
		return null;
	}
	
	@Override public void handle(AnnotationValues<Decorator> annotation, JCAnnotation ast, JavacNode node) {
		JavacTreeMaker maker = node.up().getTreeMaker();
		Decorator annotationInstance = annotation.getInstance();
		String classname = annotationInstance.abstractClassName();
		String fieldname = annotationInstance.fieldName();
		JCClassDecl annotatedclass = (JCClassDecl) node.up().get();
		
		if (annotatedclass.sym.isInterface()) {
			JavacNode clazznode = createInnerAbstractClass(node, maker,annotatedclass,classname);
			
			JavacNode fieldNode = createLocalFieldOnInnerClass(node, maker, annotatedclass,fieldname, clazznode);
			
			handleConstructor(clazznode, maker, annotatedclass, fieldNode);
			handleMethods(clazznode, maker, annotatedclass,fieldNode);
		} else {
			node.up().addError("Only Interfaces can be annotated with @Decorator");
		}
	}
	
	private JavacNode createLocalFieldOnInnerClass(JavacNode node, JavacTreeMaker maker, JCClassDecl instancetype,String fieldname, JavacNode clazznode) {
		
				JCVariableDecl field;
				if(fieldname.equals("")){
			 field = maker.VarDef(maker.Modifiers(Flags.PRIVATE | Flags.FINAL), node.toName("_instance"),maker.Ident(instancetype.name), null);
		}else{
			 field = maker.VarDef(maker.Modifiers(Flags.PRIVATE | Flags.FINAL), node.toName(fieldname), maker.Ident(instancetype.name), null);
		}
		JavacNode fieldNode = injectField(clazznode, field);
		return fieldNode;
	}
	
	private JavacNode createInnerAbstractClass(JavacNode node, JavacTreeMaker maker,JCClassDecl clazz, String classname) {
		JavacNode clazznode;
		JCClassDecl abst;
		if(classname.equals("")){
			abst = maker.ClassDef(maker.Modifiers(Flags.ABSTRACT | Flags.PUBLIC), node.toName(maker.Ident(clazz.name).toString()+"Decorator"), List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), List.<JCTree>nil());
			abst.implementing=List.<JCTree.JCExpression>of(maker.Ident(clazz.name));
		clazznode = injectType(node.up(), abst);	
		}else{
			abst = maker.ClassDef(maker.Modifiers(Flags.ABSTRACT | Flags.PUBLIC), node.toName(classname), List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), List.<JCTree>nil());
			abst.implementing=List.<JCTree.JCExpression>of(maker.Ident(clazz.name));
		clazznode = injectType(node.up(), abst);
		}
		
	
		return clazznode;
	}
	
	private void handleConstructor(JavacNode node, JavacTreeMaker maker, JCClassDecl instancetype, JavacNode fieldNode) {
		Name fieldName = removePrefixFromField(fieldNode);
		JCMethodDecl constructor = JediJavacUtil.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), node, List.<JavacNode>nil(), null, node);
		constructor.mods = maker.Modifiers(Flags.PUBLIC);
		Name argname = node.toName(instancetype.name.toString().toLowerCase());
		JCVariableDecl arg = maker.VarDef(maker.Modifiers(0), argname, maker.Ident(instancetype.name), null);
		constructor.params = List.<JCVariableDecl>of(arg);
		JCAssign assign = maker.Assign(maker.Ident(fieldName), maker.Ident(argname));
		JCBlock constrbody = maker.Block(0, List.<JCStatement>of(maker.Exec(assign)));
		constructor.body = constrbody;
		injectMethod(node, constructor);
	}
	
	private void handleMethods(JavacNode clazznode, JavacTreeMaker maker, JCClassDecl clazz, JavacNode fieldNode) {
		Name fieldName = removePrefixFromField(fieldNode);
		Types types = Types.instance(clazznode.getAst().getContext());
		
		Type type = clazz.sym.type;

		List<Type> closure = types.closure(type);
		int i=1;
		for (Type s : closure) {
			if(i<closure.size()){
					HandleWrapper.handleMethods(clazznode, maker, s, fieldNode,true);
			}
		
			i++;
		}

	}

}
