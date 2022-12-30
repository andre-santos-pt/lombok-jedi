package lombok.javac.handlers;

import lombok.Flyweight;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionResetNeeded;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

@ResolutionResetNeeded
@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(20)
public class HandleFlyweightObject extends JavacAnnotationHandler<Flyweight.Object> {
	@Override public void handle(AnnotationValues<Flyweight.Object> annotation, JCAnnotation ast, JavacNode node) {
		
		JCVariableDecl var=(JCVariableDecl) node.up().get();
		if(var.mods.flags!=Flags.FINAL){
			var.mods.flags=var.mods.flags|Flags.FINAL;
			node.up().rebuild();
		}
			
		
}
}
