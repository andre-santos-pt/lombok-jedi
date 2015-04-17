package pt.iscte.lombok.jedi.javac.handlers;
import static lombok.javac.Javac.CTC_BOT;
import static lombok.javac.Javac.CTC_EQUAL;
import static lombok.javac.Javac.CTC_NOT;
import static lombok.javac.handlers.JavacHandlerUtil.findAnnotations;
import static lombok.javac.handlers.JavacHandlerUtil.injectField;
import static lombok.javac.handlers.JavacHandlerUtil.injectMethod;
import static lombok.javac.handlers.JavacHandlerUtil.recursiveSetGeneratedBy;

import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Flyweight;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.HandleConstructor;
import lombok.javac.handlers.HandleConstructor.SkipIfConstructorExists;
import lombok.javac.handlers.HandleEqualsAndHashCode;
import lombok.javac.handlers.HandleFieldDefaults;
import lombok.javac.handlers.HandleGetter;
import lombok.javac.handlers.HandleToString;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Handles the {@code lombok.Getter} annotation for javac.
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleFlyweight extends JavacAnnotationHandler<Flyweight> {
	
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	public JCExpression handlePrimitiveType(JavacNode node, JavacTreeMaker maker, JCTree treetype){
		String type=treetype.toString();
				
				if(int.class.getName().contains(type)){
					return handleArrayType(node,maker,Integer.class);
				}
				
				if(double.class.getName().contains(type)){
					return handleArrayType(node,maker,Double.class);
				}
				
				if(float.class.getName().contains(type)){
					return handleArrayType(node,maker,Float.class);
				}
				
				if(short.class.getName().contains(type)){
					return handleArrayType(node,maker,Short.class);
				}
				
				if(byte.class.getName().contains(type)){
					return handleArrayType(node,maker,Byte.class);
				}
				
				if(long.class.getName().contains(type)){
					return handleArrayType(node,maker,Long.class);
				}
				
				if(boolean.class.getName().contains(type)){
					return handleArrayType(node,maker,Boolean.class);
				}
				
				if(char.class.getName().contains(type)){
					return handleArrayType(node,maker,Character.class);
				}
				
				return (JCExpression)treetype ;
	}


	@Override public void handle(AnnotationValues<Flyweight> annotation, JCAnnotation ast, JavacNode node) {
		

		JavacTreeMaker maker = node.up().getTreeMaker();
		if(annotation.getInstance().factory()){
			ListBuffer<JCVariableDecl> flyweightIntrinsic = new ListBuffer<JCVariableDecl>();
			notificationValitations(flyweightIntrinsic, maker, node);
			if(annotation.getInstance().factoryType()==0){
				JCBlock body = defineSecondaryFactory(node, maker,flyweightIntrinsic);
				JCMethodDecl verifywiththis = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC|Flags.STATIC), node.toName("getInstance") ,maker.Ident(((JCClassDecl)node.up().get()).name),
						List.<JCTypeParameter>nil(),flyweightIntrinsic.toList(), List.<JCExpression>nil(), body, null), node.up().get(), node.up().getContext());
						injectMethod(node.up(), verifywiththis);
			}else{
				createField(node, maker, flyweightIntrinsic);
				createConstructor(node,maker,flyweightIntrinsic);
				ListBuffer<JCStatement> statements= new ListBuffer<JCStatement>();

				creatingIfLevels(node, flyweightIntrinsic, maker, statements);
				gettingObject(node, flyweightIntrinsic.toList(), maker, statements);
				
				//verify that sends the subject to the observer
				JCBlock body=maker.Block(0, statements.toList());
					
				JCMethodDecl verifywiththis = recursiveSetGeneratedBy(maker.MethodDef(maker.Modifiers(Flags.PUBLIC|Flags.STATIC), node.toName("getInstance") ,maker.Ident(((JCClassDecl)node.up().get()).name),
				List.<JCTypeParameter>nil(),flyweightIntrinsic.toList(), List.<JCExpression>nil(), body, null), node.up().get(), node.up().getContext());
				injectMethod(node.up(), verifywiththis);	
			}
			
		}
		

		new HandleFieldDefaults().generateFieldDefaultsForType(node.up(), node, AccessLevel.PRIVATE, true, true);
		
		// TODO move this to the end OR move it to the top in eclipse.
		new HandleConstructor().generateAllArgsConstructor(node.up(), AccessLevel.PUBLIC, "", SkipIfConstructorExists.YES, node);
		new HandleGetter().generateGetterForType(node.up(), node, AccessLevel.PUBLIC, true);
		new HandleEqualsAndHashCode().generateEqualsAndHashCodeForType(node.up(), node);
		new HandleToString().generateToStringForType(node.up(), node);
	}


	private void createField(JavacNode node, JavacTreeMaker maker, ListBuffer<JCVariableDecl> flyweightIntrinsic) {
		JCExpression hashmap=definemap(flyweightIntrinsic.toList(),flyweightIntrinsic.size()-1,node,maker,true);
		
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression>nil(), hashmap, NIL_EXPRESSION, null);
		//defining the field 
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.FINAL|Flags.STATIC), node.toName("flyweights"),hashmap,fieldinit);
		//injecting the field
		JavacNode fieldNode = injectField(node.up(), field);
	}


	private JCBlock defineSecondaryFactory(JavacNode node, JavacTreeMaker maker, ListBuffer<JCVariableDecl> flyobjects) {
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>(); 
		ListBuffer<JCStatement> statements= new ListBuffer<JCStatement>();
		args.add(maker.Ident(((JCClassDecl) node.up().get()).name));
		args.add(maker.Ident(((JCClassDecl) node.up().get()).name));
		JCExpression hashfield= maker.TypeApply(handleArrayType(node, maker, java.util.HashMap.class),args.toList());
		
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression>nil(), hashfield, NIL_EXPRESSION, null);
		//defining the field 
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.FINAL|Flags.STATIC), node.toName("flyweights"),hashfield,fieldinit);
		//injecting the field
		JavacNode fieldNode = injectField(node.up(), field);
		
		
		//JCExpression getmapvalue = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName("flyweights")),flyobjects.size()-1,flyobjects.size(),null);
		
		args = new ListBuffer<JCExpression>(); 
		for (JCVariableDecl vars : flyobjects) {
			args=args.prepend(maker.Ident(vars.name));
		}
		JCNewClass newCall = maker.NewClass(null, List.<JCExpression>nil(),maker.Ident(((JCClassDecl) node.up().get()).name), args.toList(), null);
		JCVariableDecl local = maker.VarDef(maker.Modifiers(0), node.toName("temp"),maker.Ident(((JCClassDecl) node.up().get()).name),newCall);

		statements.add(local);
		JCExpression conditionvalue= maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(node.toName("flyweights")) ,node.toName("containsKey")), List.<JCExpression>of(maker.Ident(local.name)));
		JCExpression cond = maker.Unary(CTC_NOT, conditionvalue);
		ListBuffer<JCStatement> body= new ListBuffer<JCStatement>();
		args = new ListBuffer<JCExpression>(); 
		args.add(maker.Ident(local.name));
		args.add(maker.Ident(local.name));
		JCMethodInvocation addcall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(node.toName("flyweights")) ,node.toName("put")),args.toList());
		body.add(maker.Exec(addcall));
		body.add(maker.Return(maker.Ident(local.name)));
		JCBlock then = maker.Block(0, body.toList());
		JCExpression getcall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(node.toName("flyweights")) ,node.toName("get")),List.<JCExpression>of(maker.Ident(local.name)));
		JCStatement elsepart = maker.Return(getcall);
		statements.add(maker.If(cond, then, elsepart));
		return maker.Block(0, statements.toList());
		
			
	}


	private void createConstructor(JavacNode node, JavacTreeMaker maker, ListBuffer<JCVariableDecl> flyweightIntrinsic) {
		JCMethodDecl constructor = HandleConstructor.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), node.up(), List.<JavacNode>nil(), null, node);
		constructor.mods=maker.Modifiers(Flags.PRIVATE);
		ListBuffer<JCVariableDecl> params= new ListBuffer<JCVariableDecl> ();
		ListBuffer<JCStatement> body= new ListBuffer<JCStatement> ();
		int i=0;
		for (JCVariableDecl var : flyweightIntrinsic) {
			
			JCVariableDecl temp=maker.VarDef(maker.Modifiers(0), node.toName("param_"+var.name), var.vartype, null);
			params=params.prepend(temp);
			JCAssign assign= maker.Assign(maker.Ident(var.name), maker.Ident(node.toName("param_"+var.name)));
			body.add(maker.Exec(assign));
			i++;
		}
		constructor.params=params.toList();
		
		JCBlock bodblock=maker.Block(0, body.toList());
		constructor.body=bodblock;
		injectMethod(node.up(), constructor);
		
	}



	private void gettingObject(JavacNode node, List<JCVariableDecl> flyobjects, JavacTreeMaker maker, ListBuffer<JCStatement> statements) {
		ListBuffer<JCStatement> body= new ListBuffer<JCStatement>();
		ListBuffer<JCExpression> args= new ListBuffer<JCExpression>();
		JCExpression getmapvalue = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName("flyweights")),flyobjects.size()-1,flyobjects.size(),null);
		JCVariableDecl local = maker.VarDef(maker.Modifiers(0), node.toName("o"),maker.Ident(((JCClassDecl) node.up().get()).name),getmapvalue);
		statements.add(local);
		for (JCVariableDecl vars : flyobjects) {
			args=args.prepend(maker.Ident(vars.name));
		}
		JCNewClass newCall = maker.NewClass(null, List.<JCExpression>nil(),maker.Ident(((JCClassDecl) node.up().get()).name), args.toList(), null);
		args= new ListBuffer<JCExpression>();
		JCAssign assign = maker.Assign(maker.Ident(local.name), newCall);
		body.add(maker.Exec(assign));
		JCExpression localinit = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName("flyweights")),flyobjects.size()-1,flyobjects.size()-1,null);
		args.add(maker.Ident(flyobjects.get(0).name));
		args.add(maker.Ident(local.name));
		JCMethodInvocation putinvocation;
		if(flyobjects.size()-1==0){
			 putinvocation= maker.Apply(NIL_EXPRESSION,  maker.Select(maker.Ident(node.toName("flyweights")) ,node.toName("put")), args.toList());	
		}else{
			 putinvocation= maker.Apply(NIL_EXPRESSION,  maker.Select(localinit ,node.toName("put")), args.toList());
		}
		body.add(maker.Exec(putinvocation ));
		JCExpression condition=maker.Binary(CTC_EQUAL, maker.Ident(local.name), maker.Literal(CTC_BOT, null));
		JCBlock then = maker.Block(0, body.toList());
		JCIf ife = maker.If(condition,then, null);
		statements.add(ife);
		JCExpression getvaluetoreturn = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName("flyweights")),flyobjects.size()-1,flyobjects.size(),null);
		statements.add(maker.Return(getvaluetoreturn));
	}


	private void creatingIfLevels(JavacNode node, ListBuffer<JCVariableDecl> flyobjects, JavacTreeMaker maker, ListBuffer<JCStatement> statements) {
		for(int i=0; i<flyobjects.size()-1;i++){
			JCStatement ife = defineMapLevels(node,maker,flyobjects.toList(),maker.Ident(node.toName("flyweights")),flyobjects.size()-1,i);
			statements.add(ife);
		}
	}

	private JCStatement defineMapLevels(JavacNode node, JavacTreeMaker maker, List<JCVariableDecl> list, JCIdent local, int current,int count) {
		ListBuffer<JCExpression> args= new ListBuffer<JCExpression>();
		JCMethodInvocation containscall;
		JCExpression caller;
		JCExpression hashmap=definemap(list, current-count-1, node, maker, true);
		if(count==0){
			caller=local;
			 containscall=maker.Apply(NIL_EXPRESSION, maker.Select(caller ,node.toName("containsKey")), List.<JCExpression>of(maker.Ident(list.get(current).name)));//maker.Ident(list.get(current).name))
			 args.add(maker.Ident(list.get(current).name));
		}else{
			//get()loops
			caller= callingsubmap(node,maker,list,local,current,count, null);
			 containscall=maker.Apply(NIL_EXPRESSION, maker.Select(caller ,node.toName("containsKey")), List.<JCExpression>of(maker.Ident(list.get(current-count).name)));//maker.Ident(list.get(current).name))
			 args.add(maker.Ident(list.get(current-count).name));
		}
		JCUnary cond =maker.Unary(CTC_NOT, containscall);

		args.add(maker.NewClass(null, List.<JCExpression>nil(), hashmap, NIL_EXPRESSION, null));
		
		JCMethodInvocation putinvocation= maker.Apply(NIL_EXPRESSION,  maker.Select(caller ,node.toName("put")), args.toList());
		JCStatement body=maker.Exec(putinvocation);
		
		JCIf ife = maker.If(cond,body, null);
		return ife;
	}
	private JCMethodInvocation callingsubmap(JavacNode node, JavacTreeMaker maker, List<JCVariableDecl> list, JCIdent local, int current,int count,JCMethodInvocation expr){
		JCExpression arg=maker.Ident(list.get(current).name);
		JCMethodInvocation putcall;
		if(expr==null){
			 putcall =maker.Apply(NIL_EXPRESSION,  maker.Select(local ,node.toName("get")), List.<JCExpression>of(arg));
		}else{
			 putcall =maker.Apply(NIL_EXPRESSION,  maker.Select(expr ,node.toName("get")), List.<JCExpression>of(arg));
		}

		if( ((list.size())-current)>=count){
			
			return putcall;
		}else{
			return callingsubmap(node,maker,list,local, current-1,count,putcall);	
		}
		
	}
	private void notificationValitations(ListBuffer<JCVariableDecl> shareable, JavacTreeMaker maker, JavacNode node) {
		List<JCAnnotation> notifyannotations;
		JCVariableDecl var;
				for (JavacNode children : node.up().down()) {
			if(children.getKind().equals(lombok.core.AST.Kind.FIELD) ){
				notifyannotations = findAnnotations(children, Pattern.compile("^(?:FlyweightObject)$", Pattern.CASE_INSENSITIVE));	
				if(notifyannotations.size()>0){
					var =(JCVariableDecl)children.get();
						//shareable=shareable.prepend(maker.VarDef(maker.Modifiers(0),var.name ,(JCExpression)var.getType(), null));
					shareable=shareable.prepend(maker.VarDef(maker.Modifiers(0),var.name ,handlePrimitiveType(node,maker,var.getType()), null));
				}
			}	
		}
				
	}
	private JCExpression definemap(List<JCVariableDecl> flyobjects, int i, JavacNode node, JavacTreeMaker maker,boolean test){
		ListBuffer<JCExpression> typeargs = new ListBuffer<JCExpression>();
		typeargs.add(flyobjects.get(i).vartype);
		if(i>(test ? 0 : 1)){
			typeargs.add(definemap(flyobjects,i-1,node,maker,test));
		}else{			
			if(test)
				typeargs.add(maker.Ident(((JCClassDecl) node.up().get()).name));
			else
				typeargs.add(flyobjects.get(0).vartype);
		}
		return maker.TypeApply(handleArrayType(node, maker, java.util.HashMap.class),typeargs.toList());
	}

	private JCExpression handleArrayType(JavacNode node, JavacTreeMaker maker, Class<?> clazz) {
		int n = 0;
		while(clazz.isArray()) {
			clazz = clazz.getComponentType();
			n++;
		}
		
		//		String typeName = clazz.isArray() ? clazz.getComponentType().getName() : ;
		JCExpression type = JediJavacUtil.genTypeRef(node.up(), clazz.getName());
		
		while(n > 0) {
			type = maker.TypeArray(type);
			n--;
		}
		
		//		if(clazz.isArray())
		//			type = maker.TypeArray(type);
		return type;
	}	
}


