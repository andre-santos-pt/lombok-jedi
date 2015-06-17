package lombok.javac.handlers;
import lombok.Observable;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.ResolutionResetNeeded;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;

import com.sun.tools.javac.tree.JCTree.JCAnnotation;

import com.sun.tools.javac.tree.JCTree.JCVariableDecl;



@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(20)
@ResolutionResetNeeded
public class HandleObserverNotify extends JavacAnnotationHandler<Observable.Notify> {
	
	@Override public void handle(AnnotationValues<Observable.Notify> annotation, JCAnnotation ast, JavacNode node) {
		
			JCVariableDecl var=(JCVariableDecl) node.up().get();
			if(var.mods.flags!=Flags.FINAL){
				var.mods.flags=var.mods.flags|Flags.FINAL;
				node.up().rebuild();
			}
				
			
	}
}
