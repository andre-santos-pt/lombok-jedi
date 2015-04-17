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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import lombok.VisitableChildren;
import lombok.VisitableNode;
import lombok.VisitableType;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionResetNeeded;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

@ProviderFor(JavacAnnotationHandler.class)
@ResolutionResetNeeded
@HandlerPriority(1)
public class HandleVisitableChildren extends JavacAnnotationHandler<VisitableChildren> {
	
	private static Map<String, List<JCVariableDecl>> map = new HashMap<String, List<JCVariableDecl>>();
	
	@Override public void handle(AnnotationValues<VisitableChildren> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode typeNode = annotationNode.up();
		JCVariableDecl field = (JCVariableDecl) typeNode.get();
		Types types = Types.instance(typeNode.getAst().getContext());
		JCClassDecl parentClass = (JCClassDecl) typeNode.up().get();
		boolean isVisitableNode=false;
//		Types types = Types.instance(typeNode.getContext());
		for (JavacNode subnode : typeNode.up().down()) {
			if(subnode.getKind()==Kind.ANNOTATION){
				JCAnnotation ann =(JCAnnotation)subnode.get();
				if(ann.annotationType.toString().equals("VisitableNode")){
					isVisitableNode=true;
				}
			}
		}
		if(isVisitableNode){
			List<Type> closure = types.closure(field.sym.type);
			if (!HandleCompositeChildren.iscollection(closure)) {
		//	if(!field.sym.type.tsym.toString().equals("java.util.List")){
			if(field.sym.type.getTypeArguments().size()>0){
				typeNode.addError("The field type must be derived from collection");
				}else{
					
					annotation.setError(null, "only on List");
					ClassType ct = (ClassType) field.sym.type;
					VisitableNode ann = ct.tsym.getAnnotation(VisitableNode.class);
					if (ann == null) {
						typeNode.addError("The type of the field must be annotated with @" + VisitableNode.class.getSimpleName() );
					}		
				}
				
			}else{
				if(field.sym.type.getTypeArguments().size()>0){
					Type collectionType = field.sym.type.getTypeArguments().get(0);
					ClassType ct = (ClassType) collectionType;
					List<Type> closure2 = types.closure(ct);
					VisitableType annVisitType = null;
					for(Type st : closure2) {
						annVisitType = st.tsym.getAnnotation(VisitableType.class);
						if(annVisitType != null)
							break;
					}
					
					if (annVisitType == null) {
						typeNode.addError("The type argument of this Collection must be annotated with  @ " + VisitableNode.class.getSimpleName());
					}		
				}
				
			}
				
					
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
	
	static List<JCVariableDecl> getChildrenVariables(String className) {
		return map.containsKey(className) ? map.get(className) : Collections.<JCVariableDecl>emptyList();
	}
	
	static boolean hasChildren(String className) {
		return map.containsKey(className);
	}
	
}