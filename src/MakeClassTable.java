import java.util.*;

public class MakeClassTable extends JavaParserBaseVisitor<IType> {
  //クラステーブル
  public HashMap<String, Class> clsTable = new HashMap<>();
  //クラス名一時保管
  public ArrayDeque<String> cNameStack = new ArrayDeque<>();
  //引数の型を一時保管
  public HashMap<String, IType> tmpArgTypes = new HashMap<>();
  //コンストレイント一時保管
  public HashMap<String, ObjectType> tmpConstraint = new HashMap<>();

  //事前制約とforallをクラステーブルに追加
  void addPreConstraint(JavaParser.ConditionContext ctx, HashMap<String, ObjectType> constraint, ArrayList<String> abstLocs){
    //事前条件があるとき
    if(ctx != null){
      //全称量化子で束縛する位置を記録
      for (var constraintCtx : ctx.constraints().constraint()) {
        abstLocs.add(constraintCtx.IDENTIFIER(0).getText());
      }
      tmpConstraint = constraint;
      //制約にvisit
      visit(ctx);
      tmpConstraint = null;
    }
  }

  //事後制約とexistをクラステーブルに追加
  void addPostConstraint(JavaParser.ConditionContext ctx, HashMap<String, ObjectType> constraint,
                         ArrayList<String> abstLocs, ArrayList<String> bindLocs){
    //事後条件があるとき
    if(ctx != null){
      //存在量化子で束縛する位置を記録
      for (var constraintCtx : ctx.constraints().constraint()) {
        var loc = constraintCtx.IDENTIFIER(0).getText();

        //全称量化子で束縛されていないものを存在量化
        if(!abstLocs.contains(loc)){
          bindLocs.add(loc);
        }
      }

      tmpConstraint = constraint;
      visit(ctx);
      tmpConstraint = null;
    }
  }

  @Override
  public IType visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();
    var cls = new Class();

    //クラスを継承していたら親クラスを記録
    if(ctx.EXTENDS() == null){
      cls.sClassName = "Object";
    }else{
      cls.sClassName = ctx.typeType().getText();
    }
    clsTable.put(cName, cls);

    cNameStack.addFirst(cName);
    visitChildren(ctx);
    cNameStack.removeFirst();
    return null;
  }

  @Override
  public IType visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String cName = cNameStack.peekFirst();
    Class cls = clsTable.get(cName);
    cls.cons = new Constructor();

    //制約の追加
    addPreConstraint(ctx.pre, cls.cons.pre, cls.cons.abstLocs);
    addPostConstraint(ctx.post, cls.cons.post, cls.cons.abstLocs, cls.cons.bindLocs);

    //返り値型を記録
    if(ctx.refType() != null){
      var loc = ctx.refType().IDENTIFIER().getText();
      cls.cons.returnType = new RefType(loc);
    }

    tmpArgTypes = cls.cons.argTypes;
    visit(ctx.formalParameters());
    tmpArgTypes = null;
    return null;
  }

  @Override
  public IType visitFormalParameter(JavaParser.FormalParameterContext ctx) {
    String id = ctx.variableDeclaratorId().getText();

    if(ctx.typeType().refType() != null){
      var loc = ctx.typeType().refType().IDENTIFIER().getText();
      tmpArgTypes.put(id, new RefType(loc));
    }else{
      if(ctx.typeType().classOrInterfaceType() != null){
        tmpArgTypes.put(id, new NullType());
      }else{
        var type = PrimitiveType.getType(ctx.typeType().primitiveType().getText());
        tmpArgTypes.put(id, type);
      }
    }
    return null;
  }

  @Override
  public IType visitConstraint(JavaParser.ConstraintContext ctx) {
    ObjectType objType = new ObjectType();
    objType.className = ctx.className.getText();

    //各変数の型をマップに追加
    for(int i=0; i<ctx.param().size(); i++){
      String field = ctx.param().get(i).IDENTIFIER().getText();
      if(ctx.param().get(i).refType() != null){
        var loc = ctx.param().get(i).refType().IDENTIFIER().getText();
        objType.fieldTypes.put(field, new RefType(loc));
      }else {
          if(ctx.param().get(i).typeType().primitiveType() != null){
            var type = PrimitiveType.getType(ctx.param().get(i).typeType().primitiveType().getText());
            objType.fieldTypes.put(field, type);
          }else{
            objType.fieldTypes.put(field, new NullType());
          }
      }
    }

    //コンストレイント更新
    String location = ctx.IDENTIFIER().get(0).getText();
    tmpConstraint.put(location, objType);
    return visitChildren(ctx);
  }

  @Override
  public IType visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String cName = cNameStack.peekFirst();
    Class cls = clsTable.get(cName);

    //メソッドがなければ生成
    if(cls.methods == null){
      cls.methods = new HashMap<>();
    }

    Method method = new Method();

    String id = ctx.IDENTIFIER().getText();
    var typeType = ctx.typeTypeOrVoid().typeType();

    //返り値型を追加
    if(ctx.typeTypeOrVoid().VOID() != null){
      method.returnType = new NullType();
    }else if(typeType.refType() != null){
      var loc = typeType.refType().IDENTIFIER().getText();
      method.returnType = new RefType(loc);
    }else{
      method.returnType = PrimitiveType.getType(typeType.primitiveType().getText());
    }

    //制約の追加
    addPreConstraint(ctx.pre, method.pre, method.abstLocs);
    addPostConstraint(ctx.post, method.post, method.abstLocs, method.bindLocs);

    //引数の追加
    tmpArgTypes = method.argTypes;
    visit(ctx.formalParameters());
    tmpArgTypes = null;

    //辞書にメソッド追加
    cls.methods.put(id, method);
    return null;
  }

}
