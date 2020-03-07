import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

public class TypeCheck extends JavaParserBaseVisitor<String> {
  //クラス名保管
  public Deque<String> st = new ArrayDeque<String>();
  //型環境
  public Deque<HashMap<String,String>> env = new ArrayDeque<HashMap<String,String>>();
  //コンストレイント
  public Deque<HashMap<String,Constraint>> constr = new ArrayDeque<HashMap<String,Constraint>>();
  //newした回数を記録
  public int newCnt = 0;

  @Override
  public String visitBlock(JavaParser.BlockContext ctx) {
    //ブロックに入ったら型環境とコンストレイントを追加
    var newEnv = new HashMap<String,String>();
    var newConstr = new HashMap<String,Constraint>();
    env.addFirst(newEnv);
    constr.addFirst(newConstr);

    visitChildren(ctx);

    //ブロックを抜けたら型環境とコンストレイントを削除
    env.removeFirst();
    constr.removeFirst();
    return null;
  }

  @Override public String visitBlockStatement(JavaParser.BlockStatementContext ctx) {
    String cName = st.peekFirst();

    if(!cName.equals("Main")){
      return visitChildren(ctx);
    }else{
      visitChildren(ctx);

      //mainの1文ごとに型環境とコンストレイントを出力
      int line = ctx.getStart().getLine();
      out.println("Line "+line+":");
      printTypeEnv();
      printConstraint();
      return null;
    }
  }

  @Override public String visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
    var decList = ctx.variableDeclarators().variableDeclarator();
    int max = decList.size();
    String type = ctx.typeType().getText();
    var currentEnv = env.peekFirst();

    //変数宣言or初期化時に型環境に追加
    for(var dec : decList){
      String id = dec.variableDeclaratorId().getText();
      currentEnv.put(id, type);
    }

    return visitChildren(ctx);
  }


  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();

    st.addFirst(cName);
    visitChildren(ctx);
    st.removeFirst();
    return null;
  }

  @Override
  public String visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
    int max = ctx.variableDeclarators().variableDeclarator().size()-1;
    var initializer = ctx.variableDeclarators().variableDeclarator(max).variableInitializer();

    //変数の初期化の型が一致しているか
    if(initializer != null){
      String type = visit(initializer);
      String idType = ctx.typeType().getText();
      if(!idType.equals(type)){
        err.println(idType+" can not be converted to "+ type);
      }
    }
    return null;
  }

  @Override
  public String visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    return visitChildren(ctx);
  }

  @Override
  public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String returnType = ctx.typeTypeOrVoid().getText();
    String exprType = visitChildren(ctx);

    //関数の返り値の型が正しいか
    if(!returnType.equals(exprType)){
        err.println("invalid return type");
    }
    return null;
  }

  @Override
  public String visitExpression(JavaParser.ExpressionContext ctx) {

    //基本型の値のとき
    if(ctx.primary() != null){
      visit(ctx.primary());
    }

    //フィールド参照のとき
    if(ctx.bop != null && ctx.bop.getText().equals(".")){
      String instance = ctx.getChild(0).getText();
      String field = ctx.IDENTIFIER().getText();

      var currentEnv = env.peekFirst();
      String type = currentEnv.get(instance);

      if(type == null){
        err.println(instance+" is not declared");
      }

      int left = type.indexOf("(")+1;
      int right = type.indexOf(")");
      String location = type.substring(left, right);

      var currentConstr = constr.peekFirst().get(location);

      if(currentConstr == null){
        err.println(instance+" is not instanciated");
      }

      String fieldType = currentConstr.cmap.get(field);
      return fieldType;
    }

    //newのとき
    if(ctx.NEW() != null){
      var creator = ctx.creator();
      var locations = creator.paramLocation().IDENTIFIER();
      var arguments = creator.classCreatorRest().arguments().expressionList().expression();
      int currentNewCnt = ++newCnt; //newした回数を記録

      //コンストレイント追加
      constr.peekFirst().put("pt"+currentNewCnt, null);

      //コンストラクタが要求する型環境
      String cName = creator.createdName().getText();
      var pmap = Data.ct.get(cName).cons.pmap;

      //与えられた引数の型
      List<String> argTypes = new ArrayList<String>();

      int cnt=0;
      for (String key : pmap.keySet()) {
        String pType = pmap.get(key);
        String type = visit(arguments.get(cnt));
        cnt++;

        //コンストラクタの型と仮引数の型が一致していなかったらエラー
        if(!pType.equals(type) && (pType.contains("ptr") && !(type.equals("NULL") || type.contains("ptr"))) ){
          out.println("pType:"+pType);
          out.println("Type:"+type);
          err.println("constructors cannot be applied to given types.");
        }
        argTypes.add(type);
      }
      //コンストラクタと引数の数が一致していなかったらエラー
      if(pmap.keySet().size() != arguments.size()){
        err.println("constructors cannot be applied to given types.");
      }

      //コンストレイント生成
      Constraint newConstr = new Constraint();
      newConstr.className = cName;

      //コンストラクタの事後条件
      var postCmap = Data.ct.get(cName).cons.post;

      //コンストレイント更新
      //事後条件の位置は一つだけ
      //引数と外から与える位置の順番は固定である必要がある
      cnt=0;
      for (String loc : postCmap.keySet()) {
        for (String key : postCmap.get(loc).cmap.keySet()) {
          String type = postCmap.get(loc).cmap.get(key);
          if(type.contains("ptr")){
            String aType = argTypes.get(cnt);
            if(aType.equals("NULL")){
              newConstr.cmap.put(key, "NULL");
            }else{
              newConstr.cmap.put(key, "ptr("+locations.get(cnt).getText()+")");
            }
            cnt++;
          }else{
            newConstr.cmap.put(key, type);
          }
        }
      }

      //コンストレイント更新
      constr.peekFirst().replace("pt"+currentNewCnt, newConstr);
      return "ptr(pt"+currentNewCnt+")";
    }

    //メソッド呼び出しのとき
    if(ctx.methodCall() != null){
      String m = ctx.methodCall().getText();
    }

    //フィールドへの代入のとき

    return visitChildren(ctx);
  }

  @Override public String visitPrimary(JavaParser.PrimaryContext ctx) {
    String val = ctx.getText();
    if(val.equals("null")){
      return "NULL";
    } else if(val.contains("ptr")) {
      return val;
    } else {
      return visitChildren(ctx);
    }
  }

  @Override
  public String visitIntLiteral(JavaParser.IntLiteralContext ctx) {
    return "int";
  }

  @Override public String visitBoolLiteral(JavaParser.BoolLiteralContext ctx) {
    return "bool";
  }

  //型環境を出力
  void printTypeEnv(){
    out.println("  Type Environment:");
    var it = env.iterator();
    while(it.hasNext()){
      var currentEnv = it.next();
      for (String key : currentEnv.keySet()) {
        out.println("    "+key+":"+currentEnv.get(key));
      }
    }
  }

  //コンストレイントを出力
  void printConstraint(){
    out.println("  Constraint:");
    var it = constr.iterator();
    while(it.hasNext()){
      var currentConstr = it.next();
      for (String loc : currentConstr.keySet()) {
        var c = currentConstr.get(loc);
        out.println("    "+loc+" => ");
        out.println("      c:"+c.className);
        for (String val : c.cmap.keySet()) {
          System.out.println("      "+val + " => " + c.cmap.get(val));
        }
      }
    }
  }

}
