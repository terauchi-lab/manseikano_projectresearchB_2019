import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

public class TypeCheck extends JavaParserBaseVisitor<String> {
  //クラス名保管
  public Stack<String> st = new Stack<String>();
  //型環境
  public Deque<HashMap<String,String>> env = new ArrayDeque<HashMap<String,String>>();
  //コンストレイント
  public Stack<HashMap<String,Constraint>> cStack = new Stack<HashMap<String,Constraint>>();
  //newした回数を記録
  public int ptCnt = 0;

  @Override
  public String visitBlock(JavaParser.BlockContext ctx) {
    //ブロックに入ったら型環境を追加
    var newEnv = new HashMap<String,String>();
    env.addFirst(newEnv);

    visitChildren(ctx);
    printTypeEnv(ctx);

    //ブロックを抜けたら型環境を削除
    env.removeFirst();
    return null;
  }

  @Override public String visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
    var decList = ctx.variableDeclarators().variableDeclarator();
    int max = decList.size();
    String type = ctx.typeType().getText();
    var currentEnv = env.peekFirst();

    //変数宣言or初期化時に型環境に追加
    for(int i=0; i<max; i++){
      String id = decList.get(i).variableDeclaratorId().getText();
      currentEnv.put(id, type);
    }

    return visitChildren(ctx);
  }


  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();

    st.push(cName);
    visitChildren(ctx);
    st.pop();
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
    //TODO
    if(ctx.bop != null && ctx.bop.getText().equals(".")){
      String val = ctx.IDENTIFIER().getText();
    }

    //newのとき
    if(ctx.NEW() != null){
      var creator = ctx.creator();
      var locations = creator.paramLocation().IDENTIFIER();
      var arguments = creator.classCreatorRest().arguments().expressionList().expression();
      int max = arguments.size();
      //newした回数を記録
      ptCnt++;

      String cName = st.peek();
      Class c = Data.ct.get(cName);

      //コンストラクタの引数->型
      var pmap = Data.ct.get(c).cons.pmap;

      //コンストレイント生成
      Constraint newConst = new Constraint();
      newConst.className = cName;

      int cnt=0;
      for (String key : pmap.keySet()) {
        String pType = pmap.get(key);
        String type = visit(arguments.get(cnt));

        //コンストラクタの型と仮引数の型が一致していなかったらエラー
        if(!pType.equals(type) && (pType.contains("ptr") && !type.equals("NULL")) ){
          err.println("constructors cannot be applied to given types.");
        }

        //コンストレントに型追加
        if(pType.contains("ptr")&&type.equals("NULL")){
          newConst.cmap.put(key, "NULL");
        }else if(pType.contains("ptr")&&type.contains("ptr")){
          //引数の型を外から与えられた位置へのポインタとする。
          String loc = locations.get(cnt).getText();
          newConst.cmap.put(key, "ptr("+loc+")");
        }else{
          newConst.cmap.put(key, pType);
        }
        cnt++;
      }

      //コンストラクタと引数の数が一致していなかったら
      if(cnt != max-1){
        err.println("constructors cannot be applied to given types.");
      }

      if(cStack.empty()){
        //事後条件を生成
        HashMap<String,Constraint> condition = new HashMap<String,Constraint>();
        condition.put("pt"+ptCnt, newConst);
        cStack.push(condition);
      }else{
        //事後条件を更新
        var condition = cStack.pop();
        condition.put("pt"+ptCnt, newConst);
        cStack.push(condition);
      }

      return "ptr(pt"+ptCnt+")";
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
  void printTypeEnv(ParserRuleContext ctx){
    int line = ctx.getStart().getLine();
    out.println("Line "+line+":");

    var it = env.iterator();
    while(it.hasNext()){
      var currentEnv = it.next();
      for (String key : currentEnv.keySet()) {
        out.print(key+":"+currentEnv.get(key)+", ");
      }
    }
    out.println();
  }
}
