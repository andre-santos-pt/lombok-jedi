package lombok.javac.handlers;


import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import lombok.AccessLevel;
import lombok.Flyweight;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import org.mangosdk.spi.ProviderFor;

import java.util.regex.Pattern;

import static lombok.javac.Javac.*;

@ResolutionResetNeeded
@ProviderFor(JavacAnnotationHandler.class)
public class HandleFlyweight extends JavacAnnotationHandler<Flyweight> {
	
	private static final List<JCExpression> NIL_EXPRESSION = List.nil();
	public JCExpression handlePrimitiveType(JavacNode node, JavacTreeMaker maker, JCTree treetype){
		String type=treetype.toString();
				
				if(int.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Integer.class);
				}
				
				if(double.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Double.class);
				}
				
				if(float.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Float.class);
				}
				
				if(short.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Short.class);
				}
				
				if(byte.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Byte.class);
				}
				
				if(long.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Long.class);
				}
				
				if(boolean.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Boolean.class);
				}
				
				if(char.class.getName().contains(type)){
					return JediJavacUtil.handleArrayType(node,maker,Character.class);
				}
				
				return (JCExpression)treetype ;
	}


	@Override public void handle(AnnotationValues<Flyweight> annotation, JCAnnotation ast, JavacNode node) {
		String annotationName=Flyweight.class.getName();
		JavacTreeMaker maker = node.up().getTreeMaker();
		JCClassDecl classdecl=(JCClassDecl)node.up().get();
		if(classdecl.sym.isInterface()){
			node.addError("@Flyweight can not be used on Interfaces.");
		}
		if(JediJavacUtil.isAbstractType(node.up())){
			node.addError("@Flyweight can not be used on Abstract classes.");
		}
		for (JavacNode subnode : node.up().down()) {
			if(subnode.getKind().equals(Kind.METHOD)){
 				JCMethodDecl method = ((JCMethodDecl)subnode.get());
 				if(method.restype==null ){
 					if(method.mods.flags==Flags.PUBLIC){
 						subnode.addError("Class annotated with @Flyweight cannot have a public constructor.");
 					}
 					
 				}
 					
 			}	
		}
		Flyweight annotationInstance=  annotation.getInstance();
		String methodname = annotationInstance.methodName();
		if("".equals(methodname)){
			methodname="getInstance";
		}
		String fieldName = annotationInstance.fieldName();
		if(fieldName.equals("")||fieldName==null){
			fieldName="_instance";
		}
		if(annotationInstance.factory()){
			ListBuffer<JCVariableDecl> flyweightIntrinsic = new ListBuffer<JCVariableDecl>();
			notificationValitations(flyweightIntrinsic, maker, node);
			JCBlock body = null;
			
			switch(annotationInstance.factoryType()){
			case 0:
				body = defineSecondaryFactory(node, maker,flyweightIntrinsic,fieldName,annotationName);
			break;
			case 1:
				createFieldMapOfMaps(node, maker, flyweightIntrinsic,fieldName,annotationName);
				createConstructor(node,maker,flyweightIntrinsic,annotationName);
				ListBuffer<JCStatement> statements= new ListBuffer<JCStatement>();

				creatingIfLevels(node, flyweightIntrinsic, maker, statements,fieldName);
				gettingObject(node, flyweightIntrinsic.toList(), maker, statements,fieldName);
				body=maker.Block(0, statements.toList());
			break;
			default:
				node.addError("The value of factoryType can only be 0 or 1");
			break;
			}
			JCMethodDecl verifywiththis = JediJavacUtil.createMethod(maker, maker.Modifiers(Flags.PUBLIC|Flags.STATIC), node.toName(methodname), maker.Ident(classdecl.name), flyweightIntrinsic.toList().reverse(), body);
			JediJavacUtil.injectMethod(node.up(), verifywiththis,annotationName);	
			
		}
		
		new HandleGetter().generateGetterForType(node.up(), node, AccessLevel.PUBLIC, true);
		new HandleEqualsAndHashCode().generateEqualsAndHashCodeForType(node.up(), node);
		new HandleToString().generateToStringForType(node.up(), node);
	}


	private void createFieldMapOfMaps(JavacNode node, JavacTreeMaker maker, ListBuffer<JCVariableDecl> flyweightIntrinsic,String fieldName,String annotationName) {
		JCExpression hashmap=definemap(flyweightIntrinsic.toList(),flyweightIntrinsic.size()-1,node,maker,true);
		
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression>nil(), hashmap, NIL_EXPRESSION, null);
		//defining the field 
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.FINAL|Flags.STATIC), node.toName(fieldName),hashmap,fieldinit);
		//injecting the field
		JavacNode fieldNode = JediJavacUtil.injectField(node.up(), field,annotationName);
	}


	private JCBlock defineSecondaryFactory(JavacNode node, JavacTreeMaker maker, ListBuffer<JCVariableDecl> flyobjects,String fieldName,String annotationName) {
		
		
		createFieldofMap(node, maker, fieldName,annotationName);
		createConstructor(node,maker,flyobjects,annotationName);
		
		//JCExpression getmapvalue = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName("flyweights")),flyobjects.size()-1,flyobjects.size(),null);
		ListBuffer<JCStatement> statements = creatingFactory(node, maker,
				flyobjects,fieldName);
		return maker.Block(0, statements.toList());
		
			
	}


	private ListBuffer<JCStatement> creatingFactory(JavacNode node,
			JavacTreeMaker maker, ListBuffer<JCVariableDecl> flyobjects,String fieldName) {
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>(); 
		ListBuffer<JCStatement> statements= new ListBuffer<JCStatement>();
		args = new ListBuffer<JCExpression>(); 
		for (JCVariableDecl vars : flyobjects) {
			args=args.prepend(maker.Ident(vars.name));
		}
		JCNewClass newCall = maker.NewClass(null, List.<JCExpression>nil(),maker.Ident(((JCClassDecl) node.up().get()).name), args.toList(), null);
		JCVariableDecl local = maker.VarDef(maker.Modifiers(0), node.toName("temp"),maker.Ident(((JCClassDecl) node.up().get()).name),newCall);

		statements.add(local);
		JCExpression conditionvalue= maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(node.toName(fieldName)) ,node.toName("containsKey")), List.<JCExpression>of(maker.Ident(local.name)));
		JCExpression cond = maker.Unary(CTC_NOT, conditionvalue);
		ListBuffer<JCStatement> body= new ListBuffer<JCStatement>();
		args = new ListBuffer<JCExpression>(); 
		args.add(maker.Ident(local.name));
		args.add(maker.Ident(local.name));
		JCMethodInvocation addcall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(node.toName(fieldName)) ,node.toName("put")),args.toList());
		body.add(maker.Exec(addcall));
		body.add(maker.Return(maker.Ident(local.name)));
		JCBlock then = maker.Block(0, body.toList());
		JCExpression getcall=maker.Apply(NIL_EXPRESSION, maker.Select(maker.Ident(node.toName(fieldName)) ,node.toName("get")),List.<JCExpression>of(maker.Ident(local.name)));
		JCStatement elsepart = maker.Return(getcall);
		statements.add(maker.If(cond, then, elsepart));
		return statements;
	}


	private void createFieldofMap(JavacNode node, JavacTreeMaker maker,String fieldName,
			String annotationName) {
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>(); 
		args.add(maker.Ident(((JCClassDecl) node.up().get()).name));
		args.add(maker.Ident(((JCClassDecl) node.up().get()).name));
		JCExpression hashfield= maker.TypeApply(JediJavacUtil.handleArrayType(node, maker, java.util.HashMap.class),args.toList());
		
		JCNewClass fieldinit = maker.NewClass(null, List.<JCExpression>nil(), hashfield, NIL_EXPRESSION, null);
		//defining the field 
		JCVariableDecl field = maker.VarDef(maker.Modifiers(Flags.PRIVATE|Flags.FINAL|Flags.STATIC), node.toName(fieldName),hashfield,fieldinit);
		//injecting the field
		JavacNode fieldNode = JediJavacUtil.injectField(node.up(), field,annotationName);
	}


	private void createConstructor(JavacNode node, JavacTreeMaker maker, ListBuffer<JCVariableDecl> flyweightIntrinsic,String annotationName) {
		JCMethodDecl constructor = HandleConstructor.createConstructor(AccessLevel.PACKAGE, List.<JCAnnotation>nil(), node.up(), List.<JavacNode>nil(), false, node); //null
		constructor.mods=maker.Modifiers(Flags.PRIVATE);
		ListBuffer<JCVariableDecl> params= new ListBuffer<JCVariableDecl> ();
		ListBuffer<JCStatement> body= new ListBuffer<JCStatement> ();
		List<JCAnnotation> notifyannotations;
		JCVariableDecl var;
		for (JavacNode children : node.up().down()) {
			if(children.getKind().equals(lombok.core.AST.Kind.FIELD) ){
				notifyannotations = JediJavacUtil.findAnnotations(children, Pattern.compile("^(?:Object)$", Pattern.CASE_INSENSITIVE));	
				if(notifyannotations.size()>0){
					var =(JCVariableDecl)children.get();
					JCVariableDecl temp=maker.VarDef(maker.Modifiers(0), node.toName("param_"+var.name), var.vartype, null);
					params=params.append(temp);
					JCAssign assign= maker.Assign(maker.Ident(var.name), maker.Ident(node.toName("param_"+var.name)));
					body.add(maker.Exec(assign));
				}
			}	
		}
	
		constructor.params=params.toList();
		
		JCBlock bodblock=maker.Block(0, body.toList());
		constructor.body=bodblock;
		JediJavacUtil.injectMethod(node.up(), constructor,annotationName);
		
	}



	private void gettingObject(JavacNode node, List<JCVariableDecl> flyobjects, JavacTreeMaker maker, ListBuffer<JCStatement> statements,String fieldName) {
		ListBuffer<JCStatement> body= new ListBuffer<JCStatement>();
		ListBuffer<JCExpression> args= new ListBuffer<JCExpression>();
		JCExpression getmapvalue = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName(fieldName)),flyobjects.size()-1,flyobjects.size(),null);
		JCVariableDecl local = maker.VarDef(maker.Modifiers(0), node.toName("o"),maker.Ident(((JCClassDecl) node.up().get()).name),getmapvalue);
		statements.add(local);
		for (JCVariableDecl vars : flyobjects) {
			args=args.prepend(maker.Ident(vars.name));
		}
		JCNewClass newCall = maker.NewClass(null, List.<JCExpression>nil(),maker.Ident(((JCClassDecl) node.up().get()).name), args.toList(), null);
		args= new ListBuffer<JCExpression>();
		JCAssign assign = maker.Assign(maker.Ident(local.name), newCall);
		body.add(maker.Exec(assign));
		JCExpression localinit = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName(fieldName)),flyobjects.size()-1,flyobjects.size()-1,null);
		args.add(maker.Ident(flyobjects.get(0).name));
		args.add(maker.Ident(local.name));
		JCMethodInvocation putinvocation;
		if(flyobjects.size()-1==0){
			 putinvocation= maker.Apply(NIL_EXPRESSION,  maker.Select(maker.Ident(node.toName(fieldName)) ,node.toName("put")), args.toList());	
		}else{
			 putinvocation= maker.Apply(NIL_EXPRESSION,  maker.Select(localinit ,node.toName("put")), args.toList());
		}
		body.add(maker.Exec(putinvocation ));
		JCExpression condition=maker.Binary(CTC_EQUAL, maker.Ident(local.name), maker.Literal(CTC_BOT, null));
		JCBlock then = maker.Block(0, body.toList());
		JCIf ife = maker.If(condition,then, null);
		statements.add(ife);
		JCExpression getvaluetoreturn = callingsubmap(node,maker,flyobjects,maker.Ident(node.toName(fieldName)),flyobjects.size()-1,flyobjects.size(),null);
		statements.add(maker.Return(getvaluetoreturn));
	}


	private void creatingIfLevels(JavacNode node, ListBuffer<JCVariableDecl> flyobjects, JavacTreeMaker maker, ListBuffer<JCStatement> statements,String fieldName) {
		for(int i=0; i<flyobjects.size()-1;i++){
			JCStatement ife = defineMapLevels(node,maker,flyobjects.toList(),maker.Ident(node.toName(fieldName)),flyobjects.size()-1,i);
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
				notifyannotations = JediJavacUtil.findAnnotations(children, Pattern.compile("^(?:Object)$", Pattern.CASE_INSENSITIVE));	
				if(notifyannotations.size()>0){
					var =(JCVariableDecl)children.get();
						//shareable=shareable.prepend(maker.VarDef(maker.Modifiers(0),var.name ,(JCExpression)var.getType(), null));
					shareable=shareable.prepend(maker.VarDef(maker.Modifiers(0),var.name ,handlePrimitiveType(node,maker,var.getType()), null));
				}
			}	
		}
				if(shareable.size()<1){
					node.up().up().addError("Flyweight must contain 1 or more @Flyweight.Objects");
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
		return maker.TypeApply(JediJavacUtil.handleArrayType(node, maker, java.util.HashMap.class),typeargs.toList());
	}


}


