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

    var preCondition = ctx.condition().get(0);
    var postCondition = ctx.condition().get(1);

    //事前条件があれば生成
    if(preCondition != null){
      var abstLocs = ctx.condition().get(0).delta();
      for (var loc: abstLocs.IDENTIFIER()) {
        c.cons.abstLocs.add(loc.getText());
      }

      c.cons.pre = new HashMap<String,Constraint>();
      constraint = c.cons.pre;
      visit(ctx.condition().get(0));
      constraint = null;
    }

    //事後条件があれば生成
    if(postCondition != null){
      var bindLocs = ctx.condition().get(1).delta();
      for (var loc: bindLocs.IDENTIFIER()) {
        c.cons.bindLocs.add(loc.getText());
      }

      c.cons.post = new HashMap<String,Constraint>();
      constraint = c.cons.post;
      visit(ctx.condition().get(1));
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
    var typeType = ctx.typeType();

    if(typeType.refType() != null){
      var type = typeType.refType().getText();
      arg.put(id, type);
    }else{
      var type = typeType.primitiveType().getText();
      arg.put(id, type);
    }
    return null;
  }

  @Override
  public String visitConstraint(JavaParser.ConstraintContext ctx) {
    String location = ctx.IDENTIFIER().get(0).getText();
    Constraint c = new Constraint();
    c.className = clsSt.peekFirst();

    //各変数の型をマップに追加
    for(int i=0; i<ctx.param().size(); i++){
      String id = ctx.param().get(i).IDENTIFIER().getText();
      String type;
      if(ctx.param().get(i).refType() == null){
        type = ctx.param().get(i).typeType().getText();
      }else{
        type = ctx.param().get(i).refType().getText();
      }
      c.fieldType.put(id, type);
    }

    //コンストレイント更新
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
    var typeType = ctx.typeTypeOrVoid();

    //返り値型を追加
    if(typeType.VOID() != null){
      type = typeType.getText();
    }else if(typeType.typeType().refType() != null){
      type = typeType.typeType().refType().getText();
    }else{
      type = typeType.typeType().primitiveType().getText();
    }
    m.returnType = type;

    var preCondition = ctx.condition(0);
    var postCondition = ctx.condition(1);

    if(preCondition != null){
      var abstLocs = ctx.condition().get(0).delta();
      for (var loc: abstLocs.IDENTIFIER()) {
        m.abstLocs.add(loc.getText());
      }

      constraint = m.pre;
      visit(ctx.condition().get(0));
      constraint = null;
    }

    if(postCondition != null){
      var bindLocs = ctx.condition().get(1).delta();
      for (var loc: bindLocs.IDENTIFIER()) {
        m.bindLocs.add(loc.getText());
      }

      constraint = m.post;
      visit(ctx.condition().get(1));
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
