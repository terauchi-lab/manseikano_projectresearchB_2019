import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

public class MakeClassTable extends JavaParserBaseVisitor<String> {
  //クラステーブル
  public HashMap<String, Class> ct = new HashMap<String, Class>();
  //クラス名一時保管
  public Deque<String> clsSt = new ArrayDeque<String>();
  //引数の型を一時保管
  public HashMap<String, String> arg = new HashMap<String, String>();
  //コンストレイント一時保管
  public HashMap<String,Constraint> constraint = new HashMap<String,Constraint>();

  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();
    var c = new Class();
    ct.put(cName, c);

    //クラスを継承していたら親クラスを記録
    if(ctx.EXTENDS() != null){
      c.sClass = ctx.typeType().getText();
    }
    clsSt.addFirst(cName);
    visitChildren(ctx);
    clsSt.removeFirst();
    return null;
  }

  @Override
  public String visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String cName = clsSt.peekFirst();
    Class c = ct.get(cName);
    c.cons = new Constructor();

    //事前条件があれば記録
    if(ctx.pre != null && ctx.pre.delta() != null ){
      for (var loc : ctx.pre.delta().IDENTIFIER()) {
        c.cons.abstLocs.add(loc.getText());
      }
      c.cons.pre = new HashMap<String,Constraint>();
      constraint = c.cons.pre;
      visit(ctx.pre);
      constraint = null;
    }

    //事後条件があれば生成
    if(ctx.post != null){
      for (var loc: ctx.post.delta().IDENTIFIER()) {
        c.cons.bindLocs.add(loc.getText());
      }
      c.cons.post = new HashMap<String,Constraint>();
      constraint = c.cons.post;
      visit(ctx.post);
      constraint = null;
    }

    //返り値型を記録
    var returnType = ctx.refType().getText();
    c.cons.returnType = returnType;

    arg = c.cons.argType;
    visit(ctx.formalParameters());
    arg = null;
    return null;
  }

  @Override
  public String visitFormalParameter(JavaParser.FormalParameterContext ctx) {
    String id = ctx.variableDeclaratorId().getText();

    if(ctx.typeType().refType() != null){
      arg.put(id, ctx.typeType().refType().getText());
    }else{
      if(ctx.typeType().classOrInterfaceType() != null){
        arg.put(id, ctx.typeType().classOrInterfaceType().getText());
      }else{
        arg.put(id, ctx.typeType().primitiveType().getText());
      }
    }
    return null;
  }

  @Override
  public String visitConstraint(JavaParser.ConstraintContext ctx) {
    Constraint c = new Constraint();
    c.className = ctx.className.getText();

    //各変数の型をマップに追加
    for(int i=0; i<ctx.param().size(); i++){
      String id = ctx.param().get(i).IDENTIFIER().getText();
      if(ctx.param().get(i).refType() == null){
        c.fieldType.put(id, ctx.param().get(i).typeType().getText());
      }else{
        c.fieldType.put(id, ctx.param().get(i).refType().getText());
      }
    }

    //コンストレイント更新
    String location = ctx.IDENTIFIER().get(0).getText();
    constraint.put(location, c);
    return visitChildren(ctx);
  }

  @Override
  public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String cName = clsSt.peekFirst();
    Class c = ct.get(cName);

    //メソッドがなければ生成
    if(c.method == null){
      c.method = new HashMap<String, Method>();
    }

    Method m = new Method();

    String id = ctx.IDENTIFIER().getText();
    String type;
    var typeType = ctx.typeTypeOrVoid().typeType();

    //返り値型を追加
    if(ctx.typeTypeOrVoid().VOID() != null){
      m.returnType = ctx.typeTypeOrVoid().getText();
    }else if(typeType.refType() != null){
      type = typeType.refType().getText();
    }else{
      type = typeType.primitiveType().getText();
    }

    if(ctx.pre != null && ctx.pre.delta() != null){
      for (var loc: ctx.pre.delta().IDENTIFIER()) {
        m.abstLocs.add(loc.getText());
      }
      constraint = m.pre;
      visit(ctx.pre);
      constraint = null;
    }

    if(ctx.post != null && ctx.post.delta() != null){
      for (var loc: ctx.condition().get(1).delta().IDENTIFIER()) {
        m.bindLocs.add(loc.getText());
      }
      constraint = m.post;
      visit(ctx.post);
      constraint = null;
    }

    arg = m.argType;
    visit(ctx.formalParameters());
    arg = null;

    //辞書にメソッド追加
    c.method.put(id, m);
    return null;
  }

}
