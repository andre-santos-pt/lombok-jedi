package lombok.javac.handlers;

import lombok.FlyweightObject;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(20)
public class HandleFlyweightObject extends JavacAnnotationHandler<FlyweightObject> {
	@Override public void handle(AnnotationValues<FlyweightObject> annotation, JCAnnotation ast, JavacNode node) {
		
		JCVariableDecl var=(JCVariableDecl) node.up().get();
		if(var.mods.flags!=Flags.FINAL){
			var.mods.flags=var.mods.flags|Flags.FINAL;
			node.up().rebuild();
		}
			
		
}
}
