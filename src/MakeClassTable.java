import java.util.*;

public class MakeClassTable extends JavaParserBaseVisitor<String> {
  //クラステーブル
  public HashMap<String, Class> clsTable = new HashMap<String, Class>();
  //クラス名一時保管
  public ArrayDeque<String> cNameStack = new ArrayDeque<String>();
  //引数の型を一時保管
  public HashMap<String, String> tmpArgTypes = new HashMap<String, String>();
  //コンストレイント一時保管
  public HashMap<String, ObjectType> tmpConstraint = new HashMap<String, ObjectType>();

  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();
    var c = new Class();

    //クラスを継承していたら親クラスを記録
    if(ctx.EXTENDS() != null){
      c.sClassName = ctx.typeType().getText();
    }
    clsTable.put(cName, c);

    cNameStack.addFirst(cName);
    visitChildren(ctx);
    cNameStack.removeFirst();
    return null;
  }

  @Override
  public String visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String cName = cNameStack.peekFirst();
    Class c = clsTable.get(cName);
    c.cons = new Constructor();

    //事前条件があるとき
    if(ctx.pre != null){
      //全称量化子で束縛する位置を記録
      for (var constraint : ctx.pre.constraints().constraint()) {
        c.cons.abstLocs.add(constraint.IDENTIFIER(0).getText());
      }

      //事前条件が変数に記録してvisit
      c.cons.pre = new HashMap<String, ObjectType>();
      tmpConstraint = c.cons.pre;
      visit(ctx.pre);
      tmpConstraint = null;
    }

    //事後条件があるとき
    if(ctx.post != null){
      //存在量化子で束縛する位置を記録
      for (var constraint : ctx.post.constraints().constraint()) {
        var p = constraint.IDENTIFIER(0).getText();

        //全称量化子で束縛されていないものを存在量化
        if(c.cons.abstLocs.contains(p)){
          continue;
        } else {
          c.cons.bindLocs.add(p);
        }
      }

      c.cons.post = new HashMap<String, ObjectType>();
      tmpConstraint = c.cons.post;
      visit(ctx.post);
      tmpConstraint = null;
    }

    //返り値型を記録
    var returnType = ctx.refType().getText();
    c.cons.returnType = returnType;

    tmpArgTypes = c.cons.argTypes;
    visit(ctx.formalParameters());
    tmpArgTypes = null;
    return null;
  }

  @Override
  public String visitFormalParameter(JavaParser.FormalParameterContext ctx) {
    String id = ctx.variableDeclaratorId().getText();

    if(ctx.typeType().refType() != null){
      tmpArgTypes.put(id, ctx.typeType().refType().getText());
    }else{
      if(ctx.typeType().classOrInterfaceType() != null){
        tmpArgTypes.put(id, ctx.typeType().classOrInterfaceType().getText());
      }else{
        tmpArgTypes.put(id, ctx.typeType().primitiveType().getText());
      }
    }
    return null;
  }

  @Override
  public String visitConstraint(JavaParser.ConstraintContext ctx) {
    ObjectType c = new ObjectType();
    c.className = ctx.className.getText();

    //各変数の型をマップに追加
    for(int i=0; i<ctx.param().size(); i++){
      String id = ctx.param().get(i).IDENTIFIER().getText();
      if(ctx.param().get(i).refType() == null){
        c.fieldTypes.put(id, ctx.param().get(i).typeType().getText());
      }else{
        c.fieldTypes.put(id, ctx.param().get(i).refType().getText());
      }
    }

    //コンストレイント更新
    String location = ctx.IDENTIFIER().get(0).getText();
    tmpConstraint.put(location, c);
    return visitChildren(ctx);
  }

  @Override
  public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String cName = cNameStack.peekFirst();
    Class c = clsTable.get(cName);

    //メソッドがなければ生成
    if(c.methods == null){
      c.methods = new HashMap<String, Method>();
    }

    Method m = new Method();

    String id = ctx.IDENTIFIER().getText();
    var typeType = ctx.typeTypeOrVoid().typeType();

    //返り値型を追加
    if(ctx.typeTypeOrVoid().VOID() != null){
      m.returnType = ctx.typeTypeOrVoid().getText();
    }else if(typeType.refType() != null){
      m.returnType = typeType.refType().getText();
    }else{
      m.returnType = typeType.primitiveType().getText();
    }

    //事前条件があるとき
    if(ctx.pre != null){
      //全称量化子で束縛する位置を記録
      for (var constraint : ctx.pre.constraints().constraint()) {
        m.abstLocs.add(constraint.IDENTIFIER(0).getText());
      }
      tmpConstraint = m.pre;
      visit(ctx.pre);
      tmpConstraint = null;
    }

    //事後条件があるとき
    if(ctx.post != null){
      //存在量化子で束縛する位置を記録
      for (var constraint : ctx.post.constraints().constraint()) {
        m.bindLocs.add(constraint.IDENTIFIER(0).getText());
        var p = constraint.IDENTIFIER(0).getText();

        //全称量化子で束縛されていないものを存在量化
        if(m.abstLocs.contains(p)){
          continue;
        } else {
          m.bindLocs.add(p);
        }
      }

      tmpConstraint = m.post;
      visit(ctx.post);
      tmpConstraint = null;
    }

    tmpArgTypes = m.argTypes;
    visit(ctx.formalParameters());
    tmpArgTypes = null;

    //辞書にメソッド追加
    c.methods.put(id, m);
    return null;
  }

}
