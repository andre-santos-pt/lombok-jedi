package lombok.javac.handlers;
import static lombok.javac.Javac.CTC_BOOLEAN;
import static lombok.javac.Javac.CTC_BOT;
import static lombok.javac.Javac.CTC_BYTE;
import static lombok.javac.Javac.CTC_CHAR;
import static lombok.javac.Javac.CTC_DOUBLE;
import static lombok.javac.Javac.CTC_FLOAT;
import static lombok.javac.Javac.CTC_INT;
import static lombok.javac.Javac.CTC_LESS_THAN;
import static lombok.javac.Javac.CTC_LONG;
import static lombok.javac.Javac.CTC_PLUS;
import static lombok.javac.Javac.CTC_SHORT;
import static lombok.javac.Javac.CTC_VOID;
import static lombok.javac.handlers.JavacHandlerUtil.findAnnotations;
import static lombok.javac.handlers.JavacHandlerUtil.injectField;
import static lombok.javac.handlers.JavacHandlerUtil.injectMethod;
import static lombok.javac.handlers.JavacHandlerUtil.injectType;
import static lombok.javac.handlers.JavacHandlerUtil.recursiveSetGeneratedBy;
import static lombok.javac.handlers.JavacHandlerUtil.removePrefixFromField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

import lombok.Observable;
import lombok.ObserverNotify;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import lombok.javac.JavacTreeMaker.TypeTag;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;


@ProviderFor(JavacAnnotationHandler.class)
@HandlerPriority(20)
@ResolutionResetNeeded
public class HandleObserverNotify extends JavacAnnotationHandler<ObserverNotify> {
	
	@Override public void handle(AnnotationValues<ObserverNotify> annotation, JCAnnotation ast, JavacNode node) {
		
			JCVariableDecl var=(JCVariableDecl) node.up().get();
			if(var.mods.flags!=Flags.FINAL){
				var.mods.flags=var.mods.flags|Flags.FINAL;
				node.up().rebuild();
			}
				
			
	}
}
