package lombok.javac.handlers;

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

import static lombok.javac.Javac.CTC_BOOLEAN;
import static lombok.javac.Javac.CTC_BYTE;
import static lombok.javac.Javac.CTC_CHAR;
import static lombok.javac.Javac.CTC_DOUBLE;
import static lombok.javac.Javac.CTC_FLOAT;
import static lombok.javac.Javac.CTC_INT;
import static lombok.javac.Javac.CTC_LONG;
import static lombok.javac.Javac.CTC_SHORT;
import static lombok.javac.Javac.CTC_VOID;

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
import lombok.CompositeChildren;
import lombok.Singleton;
import lombok.Wrapper;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;
import lombok.javac.ResolutionResetNeeded;
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


@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(12)
@ResolutionResetNeeded
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
		Object obj = annotation.getActualExpression("classType");

		JCFieldAccess field = (JCFieldAccess) obj;
		Type type = field.selected.type;
		
		JCClassDecl cl= (JCClassDecl) node.up().get();
		Wrapper annotationInstance=  annotation.getInstance();
		String fieldName = annotationInstance.fieldName();
		if(fieldName.equals("")||fieldName==null){
			fieldName="_instance";
		}
		if(!cl.sym.isInterface()){
			JavacNode fieldNode = createLocalField(node, maker, type,fieldName,Wrapper.class.getName());
			
			if(!ConstructorExists(node.up(),maker,type,fieldNode))
			handleConstructor(node, maker, type, fieldNode,Wrapper.class.getName());
			handleMethods(node.up(), maker, type, fieldNode,true,Wrapper.class.getName());
		}else{
			node.up().addError("The annotation @Wrapper can not be used on a Interface.");
		}
		
		
		
	}

	private boolean ConstructorExists(JavacNode classNode, JavacTreeMaker maker,
			Type classtype,JavacNode fieldNode) {
		JCMethodDecl method;
		JCVariableDecl var= (JCVariableDecl)fieldNode.get();
		for (JavacNode subnode : classNode.down()) {
			if(subnode.getKind()== Kind.METHOD){
				method=(JCMethodDecl)subnode.get();
				if (method.getName().toString().equals("<init>")) {
					if(JediJavacUtil.parametersEquals(method.getParameters(), List.<JCVariableDecl>of(var))){
						return true;	
					}
				}
			}
				
		}
		return false;
	}

	private JavacNode createLocalField(JavacNode node, JavacTreeMaker maker,
			Type instancetype, String fieldName,String annotationName) {
		JCVariableDecl field = maker.VarDef(
				maker.Modifiers(Flags.FINAL),
				node.toName(fieldName),
				maker.Ident(node.toName(instancetype.toString())), null);
		JavacNode fieldNode = JediJavacUtil.injectField(node.up(), field,annotationName);
		return fieldNode;
	}

	private void handleConstructor(JavacNode node, JavacTreeMaker maker,
			Type instancetype, JavacNode fieldNode,String annotationName) {
		Name fieldName = JediJavacUtil.removePrefixFromField(fieldNode);
		JCMethodDecl constructor = JediJavacUtil.createConstructor(
				AccessLevel.PACKAGE, List.<JCAnnotation> nil(), node.up(),
				List.<JavacNode> nil(), null, node);
		constructor.mods = maker.Modifiers(Flags.PUBLIC);
		String argnameString =instancetype.toString().toLowerCase();
		Name argname = node.toName(JediJavacUtil.removePrefixFromString(argnameString));
		JCVariableDecl arg = maker.VarDef(maker.Modifiers(0), argname,
				maker.Ident(node.toName(instancetype.toString())), null);
		constructor.params = List.<JCVariableDecl> of(arg);
		JCAssign assign = maker.Assign(maker.Ident(fieldName),
				maker.Ident(argname));
		JCBlock constrbody = maker.Block(0,
				List.<JCStatement> of(maker.Exec(assign)));
		constructor.body = constrbody;

		JediJavacUtil.injectMethod(node.up(), constructor,annotationName);
	}

	public static void handleMethods(JavacNode node, JavacTreeMaker maker,
			Type classtype, JavacNode fieldNode, boolean withBody,String annotationName) {
		ListBuffer<JCVariableDecl> parameters;
		ListBuffer<JCExpression> arguments;
		Type retn;
		boolean useReturn;
		for (Symbol member : classtype.tsym.getEnclosedElements()) {
			if (member.isConstructor())
				continue;
			
			if (member.getKind().equals(ElementKind.METHOD)) {
				ExecutableElement exElem = (ExecutableElement) member;
				if(exElem.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC)){
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
							Name fieldName = node.toName(fieldNode.getName());
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
				
						JediJavacUtil.injectMethod(node, method,annotationName);	
					}	
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
					.toString());
			JCExpression type = null;
			try {
				type = JavacResolution.typeToJCTree((Type) param,
						node.getAst(), true);
			} catch (TypeNotConvertibleException e) {
				type=maker.Ident(node.toName(param.toString()));
				
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
							&& JediJavacUtil.parametersEquals(
									parameters,
									method.getParameters())) {
						return true;
					}

				}
			}
		}
		return false;
	}



	


}
