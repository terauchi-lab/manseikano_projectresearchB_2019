import java.util.*;
import static java.lang.System.*;

public class TypeCheck extends JavaParserBaseVisitor<String> {
  //クラス名保管
  public Deque<String> clsSt = new ArrayDeque<>();
  //型環境
  public Deque<HashMap<String,String>> env = new ArrayDeque<>();
  //コンストレイント
  public HashMap<String,Constraint> constraint = new HashMap<>();
  //Delta_forall
  public ArrayList<String> abstLocs = new ArrayList<>();
  //Delta_exists
  public ArrayList<String> bindLocs = new ArrayList<>();

  @Override
  public String visitBlock(JavaParser.BlockContext ctx) {
    //ブロックに入ったら型環境をスタックに追加
    var newEnv = new HashMap<String,String>();
    env.addFirst(newEnv);

    visitChildren(ctx);

    //ブロックを抜けたら最新の型環境をスタックから削除
    env.removeFirst();
    return null;
  }

  @Override public String visitBlockStatement(JavaParser.BlockStatementContext ctx) {
    String cName = clsSt.peekFirst();

    if(cName.contains("Test")){
      visitChildren(ctx);
      return null;
    }else{
      return visitChildren(ctx);
    }
  }

  @Override public String visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
    String type;
    if(ctx.typeType().classOrInterfaceType() != null){
      type = "NULL";
    }else{
      type = ctx.typeType().getText();
    }
    var currentEnv = env.peekFirst();

    //変数宣言時に型環境に追加
    var decList = ctx.variableDeclarators().variableDeclarator();
    for(var dec : decList){
      //初期化
      if(dec.variableInitializer() != null){
        type = visit(dec.variableInitializer());
      }
      String id = dec.variableDeclaratorId().getText();
      currentEnv.put(id, type);
    }

    return type;
  }


  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();
    clsSt.addFirst(cName);
    visitChildren(ctx);
    clsSt.removeFirst();
    return null;
  }

  @Override
  public String visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String cName = clsSt.peekFirst();
    var cons = Data.ct.get(cName).cons;
    abstLocs = copyStringList(cons.abstLocs);
    bindLocs = copyStringList(cons.bindLocs);
    bindLocs.add("pt");
    constraint = copyConstraintMap(cons.pre);

    var newEnv = cons.argType;
    env.addFirst(newEnv);
    env.peekFirst().put("this", "Refpt");

    visitChildren(ctx);

    abstLocs = null;
    bindLocs = null;
    constraint = null;
    env.removeFirst();
    return null;
  }

  @Override
  public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String cName = clsSt.peekFirst();
    var mName = ctx.IDENTIFIER().getText();
    var m = Data.ct.get(cName).method.get(mName);
    abstLocs = copyStringList(m.abstLocs);
    bindLocs = copyStringList(m.bindLocs);
    constraint = copyConstraintMap(m.pre);

    var newEnv = m.argType;
    env.addFirst(newEnv);
    if(!Data.ct.get(cName).method.containsKey("main")){
      env.peekFirst().put("this", "Refpt");
    }

    visitChildren(ctx);

    abstLocs = null;
    bindLocs = null;
    constraint = null;
    env.removeFirst();
    return null;
  }

  @Override
  public String visitExpression(JavaParser.ExpressionContext ctx) {

    //基本型の値のとき
    if(ctx.primary() != null){
      for (var item : env ) {
        if(item.containsKey(ctx.primary().getText())){
          return item.get(ctx.primary().getText());
        }
      }
      return visit(ctx.primary());
    }

    //フィールド参照のとき
    if(ctx.bop != null && ctx.bop.getText().equals(".") && ctx.IDENTIFIER() != null){
      String instance = ctx.getChild(0).getText();
      String field = ctx.IDENTIFIER().getText();

      var currentEnv = env.peekFirst();
      String type = currentEnv.get(instance);
      String location = type.substring(3);

      var obj = constraint.get(location);

      //objがコンストレイントになかったら
      if(obj == null){
        err.println(location+" is not found");
        return null;
      }

      return obj.fieldType.get(field);
    }

    //newのとき
    if(ctx.NEW() != null) {
      var creator = ctx.creator();
      var arguments = creator.classCreatorRest().arguments();
      String cName = ctx.creator().createdName().getText();

      //クラステーブルからコンストラクタ取得
      var cons = Data.ct.get(cName).cons;

      //ユーザーによって与えられた位置を記録
      var uAbstLocs = new ArrayList<String>();
      if (creator.forall != null) {
        for (var loc : creator.forall.IDENTIFIER()) {
          uAbstLocs.add(loc.getText());
        }
      }

      //ユーザーが束縛する位置を記録
      var uBindLocs = new ArrayList<String>();
      if (creator.exists != null) {
        for (var loc : creator.exists.IDENTIFIER()) {
          uBindLocs.add(loc.getText());
        }
        //delta_existsに位置変数を追加
        bindLocs.addAll(uBindLocs);
      }

      int cnt = 0;
      int locCnt = 0;

      //TODO 型環境は順序もほしいのでhashmapだとだめかも
      //引数が仮引数の型で型付けできるかをチェック
      if (1 < cons.argType.size()){
        for (var key : cons.argType.keySet()) {
          String pType = cons.argType.get(key);
          if(key.contains("this")) continue;

          String type = "";
          if (arguments.expressionList() != null) {
            type = visit(arguments.expressionList().expression(cnt));
          }

          if (pType.contains("Ref")) {
            //ユーザーが与えた引数の型とコンストラクタが一致しない場合
            if (!isSubtyped(type, pType.replace(cons.abstLocs.get(locCnt), uAbstLocs.get(locCnt)))) {
              System.out.println("Invalid argument and location");
              return null;
            }
            locCnt++;
          }
          cnt++;
        }
      }

      //コンストレイントの部分型チェック
      if(!isSubConstraint(constraint, cons.pre, cons.abstLocs, uAbstLocs)){
        System.out.println("Invalid constraint");
        return null;
      }

      //コンストラクタの事後条件
      var post = cons.post;

      //コンストレイント更新(update(C,C'[abst/real]))
      for (String loc : post.keySet()) {
        var c = new Constraint();

        //クラスメイトとフィールドの型を記録
        c.className = post.get(loc).className;

        //抽象化された位置変数を使っているかチェック(コンストレイントのフィールドの型)
        for (String key : post.get(loc).fieldType.keySet()) {
          String type = post.get(loc).fieldType.get(key);
          if(type.contains("Ref")) {
            //forall
            for (int i = 0; i < cons.abstLocs.size(); i++) {
              if (type.substring(3).equals(cons.abstLocs.get(i))) {
                //ユーザーが定義した位置変数で具体化
                type = "Ref"+type.substring(3).replace(cons.abstLocs.get(i), uAbstLocs.get(i));
              }
            }
            //exists
            for (int i = 0; i < cons.bindLocs.size(); i++) {
              if (type.substring(3).equals(cons.bindLocs.get(i))) {
                //ユーザーが定義した位置変数で具体化
                type = "Ref"+type.substring(3).replace(cons.bindLocs.get(i), uBindLocs.get(i));
              }
            }
          }
          c.fieldType.put(key, type);
        }

        //抽象化された位置変数を使っているかチェック(コンストレイントの定義域)
        //forall
        for(int i=0; i<cons.abstLocs.size(); i++){
          if(loc.equals(cons.abstLocs.get(i))){
            //ユーザーが定義した位置変数で具体化
            loc = uAbstLocs.get(i);
          }
        }
        //exists
        for(int i=0; i<cons.bindLocs.size(); i++){
          if(loc.equals(cons.bindLocs.get(i))){
            loc = uBindLocs.get(i);
          }
        }

        constraint.put(loc, c);
      }

      return "Ref"+uBindLocs.get(0);
    }

    //インスタンスメソッド呼び出しのとき
    if(ctx.methodCall() != null && ctx.bop != null && ctx.bop.getText().equals(".")){
      var caller = ctx.expression(0).primary().getText();
      String cName = "";
      String type = "";

      for (var cEnv : env) {
        if(cEnv.containsKey(caller)){
          type = cEnv.get(caller);
          //インスタンス変数が参照型じゃなかったら
          if(!type.contains("Ref")){
            System.out.println(caller+" should have Ref type");
            return null;
          }
        }

        //ポインタが指す先が制約になかったら
        var point = type.substring(3);
        if(!constraint.containsKey(point)){
          System.out.println(point+" isn't in a constraint");
          return null;
        }
        cName = constraint.get(point).className;
      }

      var arguments = ctx.methodCall().expressionList();
      String mName = ctx.methodCall().IDENTIFIER().getText();

      //クラステーブルからメソッド取得
      var method= Data.ct.get(cName).method.get(mName);

      //ユーザーによって与えられた位置を記録
      var uAbstLocs = new ArrayList<String>();
      if (ctx.methodCall().forall != null) {
        for (var loc : ctx.methodCall().forall.IDENTIFIER()) {
          uAbstLocs.add(loc.getText());
        }
      }

      //ユーザーが束縛する位置を記録
      var uBindLocs = new ArrayList<String>();
      if (ctx.methodCall().exists != null) {
        for (var loc : ctx.methodCall().exists.IDENTIFIER()) {
          uBindLocs.add(loc.getText());
        }
        //delta_existsに位置変数を追加
        bindLocs.addAll(uBindLocs);
      }

      int cnt = 0;
      int locCnt = 0;

      //引数が仮引数の型で型付けできるかをチェック
      if (1 < method.argType.size()){
        for (var key : method.argType.keySet()) {
          String pType = method.argType.get(key);
          if(key.contains("this")) continue;

          type = "";
          if (arguments != null) {
            type = visit(arguments.expression(cnt));
          }

          if (pType.contains("Ref")) {
            //ユーザーが与えた引数の型とコンストラクタが一致しない場合
            if (!isSubtyped(type, pType.replace(method.abstLocs.get(locCnt), uAbstLocs.get(locCnt)))) {
              System.out.println("Invalid argument and location");
              return null;
            }
            locCnt++;
          }
          cnt++;
        }
      }

      //コンストレイントの部分型チェック
      if(!isSubConstraint(constraint, method.pre, method.abstLocs, uAbstLocs)){
        System.out.println("Invalid constraint");
        return null;
      }

      //コンストラクタの事後条件
      var post = method.post;

      //コンストレイント更新(update(C,C'[abst/real]))
      for (String loc : post.keySet()) {
        var c = new Constraint();

        //クラスメイトとフィールドの型を記録
        c.className = post.get(loc).className;

        //抽象化された位置変数を使っているかチェック(コンストレイントのフィールドの型)
        for (String key : post.get(loc).fieldType.keySet()) {
          type = post.get(loc).fieldType.get(key);
          if(type.contains("Ref")) {
            //forall
            for (int i = 0; i < method.abstLocs.size(); i++) {
              if (type.substring(3).equals(method.abstLocs.get(i))) {
                //ユーザーが定義した位置変数で具体化
                type = "Ref"+type.substring(3).replace(method.abstLocs.get(i), uAbstLocs.get(i));
              }
            }
            //exists
            for (int i = 0; i < method.bindLocs.size(); i++) {
              if (type.substring(3).equals(method.bindLocs.get(i))) {
                //ユーザーが定義した位置変数で具体化
                type = "Ref"+type.substring(3).replace(method.bindLocs.get(i), uBindLocs.get(i));
              }
            }
          }
          c.fieldType.put(key, type);
        }

        //抽象化された位置変数を使っているかチェック(コンストレイントの定義域)
        //forall
        for(int i=0; i<method.abstLocs.size(); i++){
          if(loc.equals(method.abstLocs.get(i))){
            //ユーザーが定義した位置変数で具体化
            loc = uAbstLocs.get(i);
          }
        }
        //exists
        for(int i=0; i<method.bindLocs.size(); i++){
          if(loc.equals(method.bindLocs.get(i))){
            loc = uBindLocs.get(i);
          }
        }

        constraint.put(loc, c);
      }

      //返り値型
      String returnType = method.returnType;
      if(returnType.contains("Ref")){
        var loc = returnType.substring(3);
        for(int i=0; i<method.abstLocs.size(); i++){
           if(loc.equals(method.abstLocs.get(i))){
             returnType = "Ref"+uAbstLocs.get(i);
           }
        }
        for(int i=0; i<method.bindLocs.size(); i++){
          if(loc.equals(method.bindLocs.get(i))){
            returnType = "Ref"+uBindLocs.get(i);
          }
        }
      }
      return returnType;
    }

    //変数への代入のとき
    if(ctx.bop != null && ctx.bop.getText().equals("=")){
      var right = ctx.expression(0);
      var left = ctx.expression(1);

      String lType = visit(left);

      //フィールドへの代入のとき
      if(right.bop != null && right.bop.getText().equals(".")){

        String instance = right.getChild(0).getText();
        String location = null;
        for (var cEnv:env) {
          if(cEnv.containsKey(instance)){
            location = cEnv.get(instance).substring(3);
          }
        }

        //オブジェクトがコンストレイントになかったら
        if(constraint.get(location) == null){
          err.println(location+" is not found");
          return null;
        }

        //コンストレイントのフィールドを更新
        String field = right.IDENTIFIER().getText();
        constraint.get(location).fieldType.replace(field, lType);
      }

      return lType;
    }

    return visitChildren(ctx);
  }

  @Override public String visitPrimary(JavaParser.PrimaryContext ctx) {
    String val = ctx.getText();
    if(val.equals("null")){
      return "NULL";
    } else if(val.contains("ptr")) {
      return val.replace("ptr", "Ref");
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

  ArrayList<String> copyStringList(ArrayList<String> list){
    var newList = new ArrayList<String>();
    for (var item:list) {
      newList.add(item);
    }
    return newList;
  }

  Constraint copyConstraint(Constraint c){
    var newC = new Constraint();
    newC.className = c.className;
    for (var val:c.fieldType.keySet()) {
      newC.fieldType.put(val, c.fieldType.get(val));
    }
    return newC;
  }

  HashMap<String, Constraint> copyConstraintMap(HashMap<String, Constraint> cMap){
    var newMap = new HashMap<String, Constraint>();
    for (var loc:cMap.keySet()) {
      var c = copyConstraint(cMap.get(loc));
      newMap.put(loc, c);
    }
    return newMap;
  }

  //部分型かどうか(ts<:t)
  boolean isSubtyped(String ts, String t){
    if(ts.equals(t)){ //型が一致
      return true;
    }else if(ts.equals("NULL") && t.contains("Ref")){ //NULL<:ptr
      return true;
    }

    return false;
  }

  //Cs<:C[abstLoc/realLoc]
  //位置変数がRefとかだと置換したときにバグる
  boolean isSubConstraint(HashMap<String,Constraint> cs, HashMap<String,Constraint> c,
                          ArrayList<String> absLoc, ArrayList<String> realLoc){
    for (var loc : c.keySet()) {

      //cのロケーションを一時保存
      String cLoc = loc;
      //抽象化された位置変数を使っているかチェック
      for(int i=0; i<absLoc.size(); i++){
        if(loc.equals(absLoc.get(i))){
          //ユーザーが定義した位置変数で具体化
          loc = realLoc.get(i);
        }
      }

      //cにあるオブジェクトがcsになかったら
      if(!cs.containsKey(loc)){
        return false;
      }

      //クラスが部分型関係になっているか
      if(!cs.get(loc).className.equals(c.get(cLoc).className)){
        var cls = Data.ct.get(cs.get(loc).className);
        if(!cls.sClass.equals(c.get(loc).className)){
          return false;
        }
      }

      //フィールドが部分型になっているか
      for (var val : c.get(cLoc).fieldType.keySet()) {
        var ts = cs.get(loc) .fieldType.get(val);
        var t = c.get(cLoc).fieldType.get(val);

        //抽象化された位置変数を使っているかチェック
        for(int i=0; i<absLoc.size(); i++){
          if(t.contains(absLoc.get(i))){
            //ユーザーが定義した位置変数で具体化
            t = t.replace(absLoc.get(i), realLoc.get(i));
          }
        }

        if(!isSubtyped(ts, t)){
          return false;
        }
      }
    }

    return true;
  }

}
