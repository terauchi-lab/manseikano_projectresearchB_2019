import java.util.*;

public class TypeCheck extends JavaParserBaseVisitor<IType> {
  //クラス名保管
  public Deque<String> clsNameStack = new ArrayDeque<>();
  //型環境
  public Deque<LinkedHashMap<String, IType>> typeEnvStack = new ArrayDeque<>();
  //コンストレイント
  public HashMap<String, ObjectType> tmpConstraint = new HashMap<>();

  boolean typeCheckError = false;

  //引数の型チェック
  boolean isValidArgument(LinkedHashMap<String, IType> formalArgTypes, JavaParser.ExpressionListContext arguments,
                       ArrayList<String> userLocs, ArrayList<String> abstLocs){
    //具体化する位置変数の数が要求と異なる場合
    if(abstLocs.size() != userLocs.size()){
      return false;
    }

    if (1 < formalArgTypes.size()){
      int argCnt = 0;
      for (var formalArgs : formalArgTypes.keySet()) {
        if(formalArgs.contains("this")) continue; //skip for "this"

        IType formalArg = formalArgTypes.get(formalArgs);
        IType arg = visit(arguments.expression(argCnt));
        argCnt++;

        //実引数の型と仮引数の方が一致しない場合
        if (!arg.subType(formalArg.substitute(userLocs, abstLocs))){
          return false;
        }
      }
    }

    return true;
  }

  //ユーザの注釈から位置のリストを生成
  ArrayList<String> createLocList(JavaParser.DeltaContext ctx){
    var list = new ArrayList<String>();
    if (ctx != null) {
      for (var loc : ctx.IDENTIFIER()) {
        list.add(loc.getText());
      }
    }
    return list;
  }

  //コンストレイント更新(update(postC,substitutedC'[userLocs/abstLocs]))
  void updateConstraint(HashMap<String, ObjectType> postC, ArrayList<String> userLocs, ArrayList<String> abstLocs,
                        ArrayList<String> userBindLocs, ArrayList<String> bindLocs){
    for (String loc : postC.keySet()) {
      var objType = new ObjectType();

      //オブジェクトのクラスタイプを記録
      objType.className = postC.get(loc).className;

      //ユーザが渡した位置で具体化
      var substitutedC = Constraint.substitute(postC, userLocs, abstLocs);

      //ユーザが渡した位置でunpack
      var unpackedC = Constraint.substitute(substitutedC, userBindLocs, bindLocs);

      //位置が重なる制約はC'でupdate
      for (var point: unpackedC.keySet()) {
        tmpConstraint.put(point, unpackedC.get(point));
      }
    }
  }

  @Override
  public IType visitBlock(JavaParser.BlockContext ctx) {
    //ブロックに入ったら型環境をスタックに追加
    LinkedHashMap<String, IType> newEnv = new LinkedHashMap<>();
    typeEnvStack.addFirst(newEnv);

    visitChildren(ctx);

    //ブロックを抜けたら最新の型環境をスタックから削除
    typeEnvStack.removeFirst();
    return null;
  }

  @Override public IType visitBlockStatement(JavaParser.BlockStatementContext ctx) {
    if(typeCheckError){ return null; } //前のstatementでエラーしていたらskip

    visitChildren(ctx);

    //visitしてエラーだったら
    if(typeCheckError){
      Data.errorLine = ctx.start.getLine();
      return null;
    }

    String cName = clsNameStack.peekFirst();
    //main関数の場合
    if(Data.sourceFile.contains(cName)){

      var debug = new DebugInfo();
      debug.line = ctx.start.getLine();
      debug.statement = ctx.getText();

      //型環境
      var str = new ArrayList<String>();
      for (var typeEnv : typeEnvStack) {
        for (var field : typeEnv.keySet()) {
          if(field.equals("args")) continue;
          str.add(field+":"+typeEnv.get(field).getTypeName());
        }
      }
      debug.typeEnv = String.join(", ", str);

      //制約
      debug.constraint = Constraint.toString(tmpConstraint);
      Data.mainDebugInfo.add(debug);
    }

    return null;
  }

  @Override
  public IType visitStatement(JavaParser.StatementContext ctx) {

    //assert (e instanceof A)の場合
    if(ctx.ASSERT() != null && ctx.expression(0).primary() != null && ctx.expression(0).primary().expression().INSTANCEOF() != null){
      var expr = ctx.expression(0).primary().expression();
      var type = visit(expr.expression(0));
      var loc = ((RefType) type).getLocation();
      var clsType = tmpConstraint.get(loc).className;

      if(!clsType.equals(expr.typeType().getText())){
        Data.error = "assert failure";
        typeCheckError = true;
      }
      return null;
    }

    return visitChildren(ctx);

  }

  @Override public IType visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
    IType type;
    if(ctx.typeType().classOrInterfaceType() != null){
      type = new NullType();
    }else{
      type = PrimitiveType.getType(ctx.typeType().primitiveType().getText());
    }
    HashMap<String, IType> typeEnv = typeEnvStack.peekFirst();

    //変数宣言時に型環境に追加
    for(var decCtx : ctx.variableDeclarators().variableDeclarator()){
      //初期化
      if(decCtx.variableInitializer() != null){
        type = visit(decCtx.variableInitializer());
      }
      String var = decCtx.variableDeclaratorId().getText();
      typeEnv.put(var, type);
    }

    return type;
  }


  @Override
  public IType visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String clsName = ctx.IDENTIFIER().getText();
    clsNameStack.addFirst(clsName);
    visitChildren(ctx);
    clsNameStack.removeFirst();
    return null;
  }

  //TODO p_thisの制約
  @Override
  public IType visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String clsName = clsNameStack.peekFirst();
    var cls = Data.clsTable.get(clsName);
    var cons = cls.cons;

    tmpConstraint = Constraint.clone(cons.pre);

    var objType = new ObjectType();
    objType.className = clsName;

    //p_thisのフィールドの型を追加
    if(cls.fieldType != null){
      for (var field : cls.fieldType.keySet()) {
        var type = cls.fieldType.get(field);
        objType.fieldTypes.put(field, type);
      }
    }
    tmpConstraint.put("p_this", objType);

    LinkedHashMap<String, IType> newEnv = cons.argTypes;
    typeEnvStack.addFirst(newEnv);
    typeEnvStack.peekFirst().put("this", new RefType("p_this"));

    visitChildren(ctx);

    tmpConstraint = null;
    typeEnvStack.removeFirst();
    return null;
  }

  @Override
  public IType visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String clsName = clsNameStack.peekFirst();
    var methodName = ctx.IDENTIFIER().getText();
    var method = Data.clsTable.get(clsName).methods.get(methodName);
    tmpConstraint = Constraint.clone(method.pre);

    LinkedHashMap<String, IType> newEnv = method.argTypes;
    typeEnvStack.addFirst(newEnv);
    if(!Data.clsTable.get(clsName).methods.containsKey("main")){
      typeEnvStack.peekFirst().put("this", new RefType("p_this"));
    }

    visitChildren(ctx);

    tmpConstraint = null;
    typeEnvStack.removeFirst();
    return null;
  }

  @Override
  public IType visitExpression(JavaParser.ExpressionContext ctx) {

    //基本型
    if(ctx.primary() != null){
      //変数だったら
      for (var typeEnv : typeEnvStack) {
        if(typeEnv.containsKey(ctx.primary().getText())){
          return typeEnv.get(ctx.primary().getText());
        }
      }
      //値だったら
      return visit(ctx.primary());
    }

    //フィールド参照のとき
    if(ctx.bop != null && ctx.bop.getText().equals(".") && ctx.IDENTIFIER() != null){
      String instance = ctx.getChild(0).getText();
      String field = ctx.IDENTIFIER().getText();

      HashMap<String, IType> currentEnv = typeEnvStack.peekFirst();
      IType type = currentEnv.get(instance);

      //TODO NULLのときにRefにしてlocを生成するかどうか
      String location = ((RefType)type).getLocation();

      var obj = tmpConstraint.get(location);

      //objがコンストレイントになかったら
      if(obj == null){
        Data.error = location + " wasn't found in constraints";
        typeCheckError = true;
        return null;
      }

      return obj.fieldTypes.get(field);
    }

    //newのとき
    if(ctx.NEW() != null) {
      var creator = ctx.creator();
      var arguments = creator.classCreatorRest().arguments().expressionList();
      String clsName = ctx.creator().createdName().getText();

      //クラステーブルからコンストラクタ取得
      var cons = Data.clsTable.get(clsName).cons;

      //ユーザーによって与えられた位置を記録
      var userLocs = createLocList(creator.forall);

      // 引数が仮引数の型で型付けできるかをチェック
      if(!isValidArgument(cons.argTypes, arguments, userLocs, cons.abstLocs)){
        Data.error = "Invalid arguments";
        typeCheckError = true;
        return null;
      };

      //コンストレイントの部分型チェック
      if(!Constraint.isSubConstraint(tmpConstraint, cons.pre, userLocs, cons.abstLocs)){
        Data.error = "constraint didn't match the constructor constraint";
        typeCheckError = true;
        return null;
      }

      //ユーザーが束縛する位置を記録
      var userBindLocs = createLocList(creator.exists);


      //束縛する位置変数の数が要求と異なる場合
      if(cons.bindLocs.size() != userBindLocs.size()){
        Data.error = "Invalid binded location";
        typeCheckError = true;
        return null;
      }

      //コンストレイント更新(update(C,C'[abst/real]))
      updateConstraint(cons.post, userLocs, cons.abstLocs, userBindLocs, cons.bindLocs);

      return cons.returnType.substitute(userBindLocs, cons.bindLocs);
    }

    //インスタンスメソッド呼び出しのとき
    if(ctx.methodCall() != null && ctx.bop != null && ctx.bop.getText().equals(".")){
      var instance = ctx.expression(0).primary().getText();
      var arguments  = ctx.methodCall().expressionList();

      IType instanceType;
      String objClassName = "";

      //インスタンスがRef型&ポインタが制約にあるか
      for (HashMap<String, IType> typeEnv : typeEnvStack) {
        if(typeEnv.containsKey(instance)){
          instanceType = typeEnv.get(instance);

          //インスタンス変数がRef型じゃなかったら
          if(!(instanceType instanceof RefType)){
            Data.error = instance + " wasn't Ref type";
            typeCheckError = true;
            return null;
          }

          //ポインタが指す先が制約になかったら
          var point = ((RefType) instanceType).getLocation();
          if(!tmpConstraint.containsKey(point)){
            Data.error = point + " wasn't found in a constraint";
            typeCheckError = true;
            return null;
          }
          objClassName = tmpConstraint.get(point).className;
        }
      }

      String methodName = ctx.methodCall().IDENTIFIER().getText();
      //クラステーブルからメソッド取得
      var method= Data.clsTable.get(objClassName).methods.get(methodName);

      //ユーザーによって与えられた位置を記録
      var userLocs = createLocList(ctx.methodCall().forall);

      // 引数が仮引数の型で型付けできるかをチェック
      isValidArgument(method.argTypes, arguments, userLocs, method.abstLocs);

      //ユーザーが束縛する位置を記録
      var userBindLocs = createLocList(ctx.methodCall().exists);

      //束縛する位置変数の数が要求と異なる場合
      if(method.abstLocs.size() != userBindLocs.size()){
        Data.error = "Invalid binded location";
        typeCheckError = true;
        return null;
      }

      //コンストレイントの部分型チェック
      if(!Constraint.isSubConstraint(tmpConstraint, method.pre, userLocs, method.abstLocs)){
        Data.error = "constraint didn't match the method constraint";
        typeCheckError = true;
        return null;
      }

      //コンストレイント更新(update(C,C'[abst/real]))
      updateConstraint(method.post, userLocs, method.abstLocs, userBindLocs, method.bindLocs);

      //返り値型
      return method.returnType.substitute(userLocs, method.abstLocs).substitute(userBindLocs, method.bindLocs);
    }

    //変数への代入のとき
    if(ctx.bop != null && ctx.bop.getText().equals("=")){
      var right = ctx.expression(0);
      var left = ctx.expression(1);

      IType lType = visit(left);

      //フィールドへの代入のとき
      if(right.bop != null && right.bop.getText().equals(".")){

        String instance = right.getChild(0).getText();
        String location = null;
        for (var typeEnv : typeEnvStack) {
          if(typeEnv.containsKey(instance)){
            var instanceType = typeEnv.get(instance);
            location = ((RefType)instanceType).getLocation();
          }
        }

        //オブジェクトが制約になかったら
        if(tmpConstraint.get(location) == null){
          Data.error = location + " wasn't found in a constraint";
          typeCheckError = true;
          return null;
        }

        String field = right.IDENTIFIER().getText();
        //制約のフィールドの型を更新
        tmpConstraint.get(location).fieldTypes.replace(field, lType);
      }

      return lType;
    }

    return visitChildren(ctx);
  }

  @Override public IType visitPrimary(JavaParser.PrimaryContext ctx) {
    String val = ctx.getText();
    if(val.equals("null")){
      return new NullType();
    } else if(val.contains("ptr")) {
      var loc = ctx.IDENTIFIER().getText();
      return new RefType(loc);
    } else {
      return PrimitiveType.getType(ctx.getText());
    }
  }
}
