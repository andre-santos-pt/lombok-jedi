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

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import lombok.Visitor;
import lombok.Visitor.Children;
import lombok.Visitor.Node;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionResetNeeded;
import org.mangosdk.spi.ProviderFor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ProviderFor(JavacAnnotationHandler.class)
@ResolutionResetNeeded
@HandlerPriority(1)
public class HandleVisitableChildren extends JavacAnnotationHandler<Children> {
	
	private static Map<String, List<JCVariableDecl>> map = new HashMap<String, List<JCVariableDecl>>();
	
	@Override public void handle(AnnotationValues<Children> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JCVariableDecl field = (JCVariableDecl) typeNode.get();
		
		JCClassDecl parentClass = (JCClassDecl) typeNode.up().get();
		boolean isVisitableNode=false;
		Types types = Types.instance(typeNode.getAst().getContext());
		Type type = parentClass.sym.type;
		
		List<Type> closure = types.closure(type);

		for(Type s : closure) {
			ClassType ct = (ClassType) s;
			Visitor.Node ann = ct.tsym.getAnnotation(Visitor.Node.class);
		
			if(ann != null) {
				isVisitableNode=true;
			}
		}
		if(isVisitableNode){
			checkFieldCompatibility(typeNode, field);
				
					
			String className = parentClass.sym.type.toString();
			
			List<JCVariableDecl> list = map.get(className);
			if(list == null) {
				list = new ArrayList<JCVariableDecl>();
				map.put(className, list);
			}
			list.add(field);	
		}else{
			typeNode.up().addError("Only a class annotated with @VisitableNode can have a field annotated with @VisitableChildren");
		}
		
	}
	private void checkFieldCompatibility(JavacNode fieldNode, JCVariableDecl field){
		Types types = Types.instance(fieldNode.getAst().getContext());
		List<Type> parameterTypes = field.sym.type.getTypeArguments();
		Type type;	
		if(parameterTypes.size()>0){
				 type = parameterTypes.get(0);	
			}else{
				 type =field.sym.type;
			}
			
		Node containsAnnotation = type.tsym.getAnnotation(Visitor.Node.class);
			List<Type> closure = types.closure(field.sym.type);
			
			
				if (!HandleCompositeChildren.iscollection(closure)) {
					if (parameterTypes.size() != 0) {
						fieldNode.addError("This Type is not a subtype "+Collection.class.getSimpleName()+".");	
					} else {
						if (containsAnnotation == null) {
							fieldNode.addError("This type must be a class annotated with @"+Visitor.Node.class.getSimpleName());
							//fieldNode.addError("The type argument of this Collection must be annotated with  @ " + CompositeComponent.class.getSimpleName());
							} 
						
					}
					
				} else {
					if (parameterTypes.size() > 0) {
						if (parameterTypes.size() !=1) {
							fieldNode.addError("The type "+type.tsym.toString()+" cannot have more than one type argument.");
						}else{
							if (containsAnnotation == null) {
								fieldNode.addError("The type argument of this Collection must be annotated with  @ " + Visitor.Node.class.getSimpleName());
							}
						}
						
					}else{
						fieldNode.addError("The Type Arguments must be defined for this field.");
					}
				}
			
	}

	
	static List<JCVariableDecl> getChildrenVariables(String className) {
		return map.containsKey(className) ? map.get(className) : Collections.<JCVariableDecl>emptyList();
	}
	
	static boolean hasChildren(String className) {
		return map.containsKey(className);
	}
	
}
