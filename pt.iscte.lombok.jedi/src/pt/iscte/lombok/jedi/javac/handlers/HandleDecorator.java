package pt.iscte.lombok.jedi.javac.handlers;

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

import static lombok.javac.Javac.*;
import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import lombok.AccessLevel;
import lombok.Decorator;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.JavacTreeMaker.TypeTag;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
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
		System.out.println("correu decorator");
		Decorator annotationInstance = annotation.getInstance();
		System.out.println("correu decorator");
		Class<?> instancetype = annotationInstance.interfaceClass();
		System.out.println("correu decorator");
		String classname = "Decorator"; //annotationInstance.abstractClassName();
		System.out.println("correu decorator");
		JCClassDecl annotatedclass = (JCClassDecl) node.up().get();
		
		if (Modifier.isInterface(instancetype.getModifiers())) {
			JavacNode clazznode = createInnerAbstractClass(node, maker,instancetype,classname);
			
			JavacNode fieldNode = createLocalFieldOnInnerClass(node, maker, instancetype, clazznode);
			
			handleConstructor(clazznode, maker, instancetype, fieldNode);
			handleMethods(clazznode, maker, instancetype, fieldNode);
		} else {
			node.up().addError("The @Decorator's attribute must be an Interface.");
		}
	}
	
	private JavacNode createLocalFieldOnInnerClass(JavacNode node, JavacTreeMaker maker, Class<?> instancetype, JavacNode clazznode) {
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE | Flags.FINAL), node.toName("_instance"), maker.Ident(node.toName(instancetype.getSimpleName())), null);
		JavacNode fieldNode = injectField(clazznode, field);
		return fieldNode;
	}
	
	private JavacNode createInnerAbstractClass(JavacNode node, JavacTreeMaker maker, Class<?> instancetype, String classname) {
		JCClassDecl clazz;
		JavacNode clazznode;
		if(classname.equals("")){
			clazz = maker.ClassDef(maker.Modifiers(Flags.ABSTRACT | Flags.PUBLIC), node.toName(instancetype.getName()+"Decorator"), List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), List.<JCTree>nil());
			clazz.implementing=List.<JCTree.JCExpression>of(maker.Ident(node.toName(instancetype.getSimpleName())));
		clazznode = injectType(node.up(), clazz);	
		}else{
			clazz = maker.ClassDef(maker.Modifiers(Flags.ABSTRACT | Flags.PUBLIC), node.toName(classname), List.<JCTypeParameter>nil(), null, List.<JCExpression>nil(), List.<JCTree>nil());
			clazz.implementing=List.<JCTree.JCExpression>of(maker.Ident(node.toName(instancetype.getSimpleName())));
		clazznode = injectType(node.up(), clazz);
		}
		
	
		return clazznode;
	}
	
	private void handleConstructor(JavacNode node, JavacTreeMaker maker, Class<?> instancetype, JavacNode fieldNode) {
		Name fieldName = removePrefixFromField(fieldNode);
		JCMethodDecl constructor = JediJavacUtil.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), node.up(), List.<JavacNode>nil(), null, node);
		constructor.mods = maker.Modifiers(Flags.PUBLIC);
		Name argname = node.toName(instancetype.getSimpleName().toLowerCase());
		JCVariableDecl arg = maker.VarDef(maker.Modifiers(0), argname, maker.Ident(node.toName(instancetype.getSimpleName())), null);
		constructor.params = List.<JCVariableDecl>of(arg);
		JCAssign assign = maker.Assign(maker.Ident(fieldName), maker.Ident(argname));
		JCBlock constrbody = maker.Block(0, List.<JCStatement>of(maker.Exec(assign)));
		constructor.body = constrbody;
		injectMethod(node.up(), constructor);
	}
	
	private void handleMethods(JavacNode node, JavacTreeMaker maker, Class<?> instancetype, JavacNode fieldNode) {
		Name fieldName = removePrefixFromField(fieldNode);
		Class<?>[] interfaces = instancetype.getInterfaces();
		if (interfaces.length > 0) for (Class<?> i : interfaces) {
			handleMethods(node, maker, i, fieldNode);
		}
		for (Method m : instancetype.getDeclaredMethods()) {
			if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !methodManuallyExists(m, node, maker)) {
				ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
				TypeTag temp = handlePrimitiveType(m.getReturnType().getName());
				JCExpression returnType;
				if (temp == null) {
					returnType = handleArrayType(node, maker, m.getReturnType());
				} else {
					returnType = maker.TypeIdent(temp);
				}
				ListBuffer<JCVariableDecl> arguments = new ListBuffer<JCVariableDecl>();
				ListBuffer<JCExpression> parameters = new ListBuffer<JCExpression>();
				int i = 0;
				String argname = "";
				for (Class<?> o : m.getParameterTypes()) {
					i++;
					temp = handlePrimitiveType(o.getName());
					argname = "arg" + i;
					if (temp == null) {
						JCExpression type = handleArrayType(node, maker, o);
						arguments.add(maker.VarDef(maker.Modifiers(0), node.toName(argname), type, null));
						parameters.add(maker.Ident(node.toName(argname)));
					} else {
						arguments.add(maker.VarDef(maker.Modifiers(0), node.toName(argname), maker.TypeIdent(temp), null));
						parameters.add(maker.Ident(node.toName(argname)));
					}
					
				}
				JCMethodInvocation newCall = maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(fieldName), node.up().toName(m.getName())), parameters.toList());
				if (handlePrimitiveType(m.getReturnType().getName()) != null && handlePrimitiveType(m.getReturnType().getName()).equals(CTC_VOID)) {
					statements.add(maker.Exec(newCall));
				} else {
					statements.add(maker.Return(newCall));
				}
				JCBlock body = maker.Block(0, statements.toList());
				JCMethodDecl decl = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC), node.toName(m.getName()), returnType, List.<JCTypeParameter>nil(), arguments.toList(), List.<JCExpression>nil(), body, null), node.up().get(), node.getContext());
				injectMethod(node.up(), decl);
			}
		}
	}
	
	private boolean methodManuallyExists(Method m, JavacNode node, JavacTreeMaker maker) {
		JCClassDecl annotatedclass = (JCClassDecl) node.up().get();
		for (JCTree member : annotatedclass.getMembers()) {
			if (member.getKind() == com.sun.source.tree.Tree.Kind.METHOD) {
				JCMethodDecl method = (JCMethodDecl) member;
				if (!method.getName().equals(node.toName("<init>"))) {
				if (m.getName().equals(method.getName().toString()) && parametersEquals(node, maker, m.getParameterTypes(), method.getParameters())) {
						return true;
					}
					
				}
			}
		}
		return false;
	}
	
	private boolean parametersEquals(JavacNode node, JavacTreeMaker maker, Class<?>[] parameterTypes, List<JCVariableDecl> list) {
		if (list.size() == parameterTypes.length) {
			for (int i = 0; i < list.size(); i++) {
				String k = handleArrayType(node, maker, parameterTypes[i]).toString();
				String p = list.get(i).getType().toString();
				if (!k.equals(p)) {
					return false;
				} else {
					
				}
			}
			return true;
			
		}
		return false;
	}
	
	private JCExpression handleArrayType(JavacNode node, JavacTreeMaker maker, Class<?> clazz) {
		int n = 0;
		while (clazz.isArray()) {
			clazz = clazz.getComponentType();
			n++;
		}
		JCExpression type = JediJavacUtil.genTypeRef(node.up(), clazz.getName());
		
		while (n > 0) {
			type = maker.TypeArray(type);
			n--;
		}
		return type;
	}
}
