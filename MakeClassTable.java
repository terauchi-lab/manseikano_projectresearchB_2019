import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

public class MakeClassTable extends JavaParserBaseVisitor<String> {
  //クラステーブル
  public HashMap<String, Class> ct = new HashMap<String, Class>();
  //クラス名一時保管
  public Deque<String> st = new ArrayDeque<String>();
  //引数の型を一時保管
  public Deque<HashMap<String, String>> paramStack = new ArrayDeque<HashMap<String, String>>();
  //コンストレイント一時保管
  public Deque<HashMap<String,Constraint>> conditionStack = new ArrayDeque<HashMap<String,Constraint>>();

  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();

    if(ct.containsKey(cName)){
      System.err.println("the class "+cName+" has already been defined.");
      return null;
    };

    ct.put(cName, new Class());
    st.addFirst(cName);
    visitChildren(ctx);
    st.removeFirst();
    return null;
  }

  @Override
  public String visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
    String type = ctx.typeType().getText();
    var decList = ctx.variableDeclarators().variableDeclarator();
    String cName = st.peekFirst();
    Class c = ct.get(cName);

    //フィールドがなければ生成
    if(c.fmap == null){
      c.fmap = new HashMap<String,String>();
    }

    for(var dec : decList){
      String id = dec.variableDeclaratorId().getText();

      //既にフィールドの変数が定義されていたらエラー
      if(c.fmap.containsKey(id)){
        System.err.println("the variable "+id+" has already been defined.");
        return null;
      };
      c.fmap.put(id, type);
    }

    return visitChildren(ctx);
  }

  @Override
  public String visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String cName = st.peekFirst();
    Class c = ct.get(cName);

    //コンストラクタがなければ生成
    if(c.cons == null){
      c.cons = new Constructor();
    }

    //コンストラクタの名前が違っていたらエラー
    String id = ctx.IDENTIFIER().getText();
    if(!cName.equals(id)){
      System.err.println("invalid constructor name.");
    }

    var preCondition = ctx.condition().get(0).constraints();
    var postCondition = ctx.condition().get(1).constraints();

    //事前条件があれば生成
    if(preCondition != null){
      c.cons.pre = new HashMap<String,Constraint>();
      conditionStack.addFirst(c.cons.pre);
      visit(ctx.condition().get(0));
      conditionStack.removeFirst();
    }

    //事後条件があれば生成
    if(postCondition != null){
      c.cons.post = new HashMap<String,Constraint>();
      conditionStack.addFirst(c.cons.post);
      visit(ctx.condition().get(1));
      conditionStack.removeFirst();
    }

    paramStack.addFirst(c.cons.pmap);
    visit(ctx.formalParameters());
    paramStack.removeFirst();
    return null;
  }

  @Override
  public String visitFormalParameter(JavaParser.FormalParameterContext ctx) {
    var pmap = paramStack.peekFirst();
    String type = ctx.getChild(0).getText();
    String id = ctx.getChild(1).getText();
    pmap.put(id, type);
    return null;
  }

  @Override
  public String visitConstraint(JavaParser.ConstraintContext ctx) {
    HashMap<String,Constraint> condition = conditionStack.peekFirst();

    //位置
    String location = ctx.IDENTIFIER().get(0).getText();

    Constraint c = new Constraint();
    c.className = st.peekFirst();

    //各変数の型をマップに追加
    for(int i=0; i<ctx.param().size(); i++){
      String id = ctx.param().get(i).IDENTIFIER().getText();
      String type = ctx.param().get(i).typeType().getText();
      c.cmap.put(id, type);
    }

    //コンストレイント更新
    condition.put(location, c);
    return visitChildren(ctx);
  }

  @Override
  public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String type = ctx.typeTypeOrVoid().getText();
    String id = ctx.IDENTIFIER().getText();

    String cName = st.peekFirst();
    Class c = ct.get(cName);

    //メソッドがなければ生成
    if(c.methodMap == null){
      c.methodMap = new HashMap<String, Method>();
    }

    Method m = new Method();
    m.returnType = type;

    c.methodMap.put(id, m);

    var preCondition = ctx.condition(0);
    var postCondition = ctx.condition(1);

    if(preCondition != null){
      m.pre = new HashMap<String,Constraint>();
      conditionStack.addFirst(m.pre);
      visit(ctx.condition().get(0));
      conditionStack.removeFirst();
    }

    if(postCondition != null){
      m.post = new HashMap<String,Constraint>();
      conditionStack.addFirst(m.post);
      visit(ctx.condition().get(1));
      conditionStack.removeFirst();
    }

    paramStack.addFirst(m.pmap);
    visit(ctx.formalParameters());
    paramStack.removeFirst();

    return null;
  }

}
