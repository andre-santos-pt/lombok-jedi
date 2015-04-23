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
import static lombok.javac.handlers.JavacHandlerUtil.recursiveSetGeneratedBy;
import static lombok.javac.handlers.JavacHandlerUtil.removePrefixFromField;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import lombok.AccessLevel;
import lombok.Wrapper;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;
import lombok.javac.JavacResolution.TypeNotConvertibleException;
import lombok.javac.JavacTreeMaker;
import lombok.javac.JavacTreeMaker.TypeTag;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
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
public class HandleWrapper extends JavacAnnotationHandler<Wrapper> {

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

	@Override
	public void handle(AnnotationValues<Wrapper> annotation, JCAnnotation ast,
			JavacNode node) {
		JavacTreeMaker maker = node.up().getTreeMaker();
		Object obj = annotation.getActualExpression("value");

		JCFieldAccess field = (JCFieldAccess) obj;
		Type type = field.selected.type;
		
		JCClassDecl cl= (JCClassDecl) node.up().get();
		
		if(!cl.sym.isInterface()){
			JavacNode fieldNode = createLocalField(node, maker, type);
			handleConstructor(node, maker, type, fieldNode);
			handleMethods(node.up(), maker, type, fieldNode,true);
		}else{
			node.up().addError("The annotation @Wrapper can not be used on a Interface.");
		}
		
		
		
	}

	private JavacNode createLocalField(JavacNode node, JavacTreeMaker maker,
			Type instancetype) {
		JCVariableDecl field = maker.VarDef(
				maker.Modifiers(Flags.FINAL),
				node.toName("wrapTarget"),
				maker.Ident(node.toName(instancetype.toString())), null);
		JavacNode fieldNode = injectField(node.up(), field);
		return fieldNode;
	}

	private void handleConstructor(JavacNode node, JavacTreeMaker maker,
			Type instancetype, JavacNode fieldNode) {
		Name fieldName = removePrefixFromField(fieldNode);
		JCMethodDecl constructor = JediJavacUtil.createConstructor(
				AccessLevel.PACKAGE, List.<JCAnnotation> nil(), node.up(),
				List.<JavacNode> nil(), null, node);
		constructor.mods = maker.Modifiers(Flags.PUBLIC);
		Name argname = node.toName(instancetype.toString().toLowerCase());
		JCVariableDecl arg = maker.VarDef(maker.Modifiers(0), argname,
				maker.Ident(node.toName(instancetype.toString())), null);
		constructor.params = List.<JCVariableDecl> of(arg);
		JCAssign assign = maker.Assign(maker.Ident(fieldName),
				maker.Ident(argname));
		JCBlock constrbody = maker.Block(0,
				List.<JCStatement> of(maker.Exec(assign)));
		constructor.body = constrbody;

		injectMethod(node.up(), constructor);
	}

	public static void handleMethods(JavacNode node, JavacTreeMaker maker,
			Type classtype, JavacNode fieldNode, boolean withBody) {
		ListBuffer<JCVariableDecl> parameters;
		ListBuffer<JCExpression> arguments;
		Type retn;
		boolean useReturn;
		
		for (Symbol member : classtype.tsym.getEnclosedElements()) {

			if (member.isConstructor())
				continue;
			ExecutableElement exElem = (ExecutableElement) member;
			if (member.getKind().equals(ElementKind.METHOD) && exElem.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC)) {
				useReturn = exElem.getReturnType().getKind() != TypeKind.VOID;
				retn = (Type) exElem.getReturnType();
				parameters = new ListBuffer<JCVariableDecl>();
				arguments = new ListBuffer<JCExpression>();
				drillIntoMethod(node,maker,classtype,member,exElem,parameters,arguments);
				
				if(!methodManuallyExists( member.name.toString(),parameters.toList(),node, maker)){
					JCExpression returnType=null;
					try {
						returnType = JavacResolution.typeToJCTree((Type) retn, node.getAst(), true);
					} catch (TypeNotConvertibleException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					JCBlock body=null;
					if(withBody){
						Name fieldName = removePrefixFromField(fieldNode);
					ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
					 JCMethodInvocation newCall = maker.Apply(NIL_EXPRESSION,
					 maker.Select(maker.Ident(fieldName),  member.name),
					arguments.toList());
					 if(useReturn){
						 stats.add(maker.Return(newCall));
					 }else{
						 stats.add(maker.Exec(newCall));	 
					 }
					
					body = maker.Block(0, stats.toList());
					}
					JCMethodDecl method= maker.MethodDef(maker.Modifiers(Flags.PUBLIC),  member.name, returnType, List.<JCTypeParameter>nil(), parameters.toList(), List.<JCExpression>nil(), body, null);
			
					injectMethod(node, method);	
				}
				
			}
		}
		}

	public static void drillIntoMethod(JavacNode node, JavacTreeMaker maker, Type classtype, Symbol member, ExecutableElement exElem, ListBuffer<JCVariableDecl> parameters, ListBuffer<JCExpression> arguments) {
		JavacTypes types = node.getTypesUtil();
		ClassType ct;
		if (classtype instanceof ClassType) {
			ct = (ClassType) classtype;

		} else {
			return;
		}
		ExecutableType methodType = (ExecutableType) types.asMemberOf(
				ct, member);
		getParameters(node, maker, exElem, methodType, parameters,
				arguments);
		
	}

	public static void getParameters(JavacNode node, JavacTreeMaker maker,
			ExecutableElement exElem, ExecutableType methodType,
			ListBuffer<JCVariableDecl> parameters,
			ListBuffer<JCExpression> arguments) {
		java.util.List<? extends VariableElement> paramList = exElem
				.getParameters();
		int i = 0;
		for (TypeMirror param : methodType.getParameterTypes()) {
				Name name = node.toName(paramList.get(i)
					.getSimpleName().toString());
			JCExpression type = null;
			try {
				type = JavacResolution.typeToJCTree((Type) param,
						node.getAst(), true);
			} catch (TypeNotConvertibleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			parameters.append(maker.VarDef(maker.Modifiers(0), name,
					type, null));

			arguments.append(maker.Ident(name));
			i++;
		}
	}

	private static int getModifiers(Set<javax.lang.model.element.Modifier> modifiers) {
		int value = 0;
		for (javax.lang.model.element.Modifier modifier : modifiers) {
			switch (modifier) {
			case PUBLIC:
				value = value | Flags.PUBLIC;
				break;
			case PROTECTED:
				value = value | Flags.PROTECTED;
				break;
			case PRIVATE:
				value = value | Flags.PRIVATE;
				break;
			case ABSTRACT:
				value = value | Flags.ABSTRACT;
				break;
			case STATIC:
				value = value | Flags.STATIC;
				break;
			case FINAL:
				value = value | Flags.FINAL;
				break;
			case TRANSIENT:
				value = value | Flags.TRANSIENT;
				break;
			case VOLATILE:
				value = value | Flags.VOLATILE;
				break;
			case SYNCHRONIZED:
				value = value | Flags.SYNCHRONIZED;
				break;
			case NATIVE:
				value = value | Flags.NATIVE;
				break;
			case STRICTFP:
				value = value | Flags.STRICTFP;
				break;
			}
		}
		return value;
	}

	private static boolean methodManuallyExists(String methodname ,List<JCVariableDecl>parameters, JavacNode node,
			JavacTreeMaker maker) {
		JCClassDecl annotatedclass = (JCClassDecl) node.get();
		for (JCTree member : annotatedclass.getMembers()) {
			if (member.getKind() == com.sun.source.tree.Tree.Kind.METHOD) {
				JCMethodDecl method = (JCMethodDecl) member;
				if (!method.getName().equals(node.toName("<init>"))) {
					if (methodname.equals(method.getName().toString())
							&& parametersEquals(
									parameters,
									method.getParameters())) {
						return true;
					}

				}
			}
		}
		return false;
	}

	public static boolean parametersEquals(
			 List<JCVariableDecl> parameterTypes, List<JCVariableDecl> list) {
		if (list.size() == parameterTypes.size()) {
			for (int i = 0; i < list.size(); i++) {
				String k = parameterTypes.get(i).getType().toString();
				String p = list.get(i).getType().toString();
				if (!k.equals(p)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


}
