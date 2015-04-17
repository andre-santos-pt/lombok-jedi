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
 
 import java.util.HashMap;
import java.util.Map;

import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import pt.iscte.lombok.jedi.Composite;
import pt.iscte.lombok.jedi.CompositeChildren;
import pt.iscte.lombok.jedi.CompositeComponent;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.util.List;
@HandlerPriority(8)
 @ProviderFor(JavacAnnotationHandler.class)
 public class HandleComposite extends JavacAnnotationHandler<Composite> {
 	
 	private static Map<String,JCClassDecl> subtypes = new HashMap<String, JCClassDecl>();
 	
 	public static JCClassDecl getComposite(String rootNode) {
 		if(!subtypes.containsKey(rootNode))
 			return null;
 		else
 			return subtypes.get(rootNode);
 	}
 	
 	@Override public void handle(AnnotationValues<Composite> annotation, JCAnnotation ast, JavacNode annotationNode) {
 		
 		JavacNode typeNode = annotationNode.up();
 		
 		
 		JCClassDecl clazz = (JCClassDecl) annotationNode.up().get();
 		boolean hasAnnotation = false;
		for (JavacNode subnode : annotationNode.up().down()) {
			if(subnode.getKind().equals(Kind.FIELD)){
				for (JavacNode fieldannotations : subnode.down()) {
					if(fieldannotations.getKind().equals(Kind.ANNOTATION)){
						JCAnnotation ann= (JCAnnotation)fieldannotations.get();
						if(ann.type.toString().equals(CompositeChildren.class.getName())){
							hasAnnotation=true;
						}
						

						
					}
					
					
				}
				
			}
		}
 		if(hasAnnotation){
 	 		Types types = Types.instance(typeNode.getAst().getContext());
 	 		Type type = clazz.sym.type;
 	 	
 	 		List<Type> closure = types.closure(type);
 	 		
 	 		for(Type s : closure) {
 	 			ClassType ct = (ClassType) s;
 	 			CompositeComponent ann = ct.tsym.getAnnotation(CompositeComponent.class);
 	 			if(ann != null) {
 	 			
 	 				subtypes.put(ct.toString(), clazz);
 	 				
 	 			}
 	 		}
 		}else{
 			annotationNode.addError("Class must contain a field annotated with @CompositeChildren");
 		}

 	}

 	
 }