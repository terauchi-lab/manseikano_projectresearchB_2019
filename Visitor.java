import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

class Class {
  //Field->Type
  public HashMap<String,String> fmap;
  //Constructor
  public Constructor cons;
  //Method
  public HashMap<String,Method> methodMap;
}

class Constructor {
  //Param->Type
  public HashMap<String,String> pmap = new HashMap<String,String>();

  //PreCondition
  //Location -> Precondition
  public HashMap<String,Constraint> pre;

  //PosCcondition
  //Location -> Precondition
  public HashMap<String,Constraint> post;
}

class Method {
  //Param->Type
  public HashMap<String,String> pmap = new HashMap<String,String>();

  //PreCondition
  //Location -> Precondition
  public HashMap<String,Constraint> pre;

  //PosCcondition
  //Location -> Precondition
  public HashMap<String,Constraint> post;

  //ReturnType
  public String returnType;
}

class Constraint {
  //Class name
  public String className;
  //constraint field-type
  public HashMap<String,String> cmap = new HashMap<String,String>();
}

public class Visitor extends JavaParserBaseVisitor<String> {
  //クラステーブル
  public HashMap<String, Class> ct = new HashMap<String, Class>();
  //クラス名一時保管
  public Stack<String> st = new Stack<String>();
  //引数の型を一時保管
  public Stack<HashMap<String, String>> paramStack = new Stack<HashMap<String, String>>();
  //コンストレイント一時保管
  public Stack<HashMap<String,Constraint>> conditionStack = new Stack<HashMap<String,Constraint>>();

  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();

    if(ct.containsKey(cName)){
      System.err.println("the class "+cName+" has already been defined.");
      return null;
    };

    ct.put(cName, new Class());
    st.push(cName);
    visitChildren(ctx);
    st.pop();
    return null;
  }

  @Override
  public String visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
    String type = ctx.getChild(0).getText();
    String id = ctx.getChild(1).getText();
    String cName = st.peek();
    Class c = ct.get(cName);

    //フィールドがなければ生成
    if(c.fmap == null){
      c.fmap = new HashMap<String,String>();
    }

    if(c.fmap.containsKey(id)){
      System.err.println("the variable "+id+" has already been defined.");
      return null;
    };

    c.fmap.put(id, type);
    return visitChildren(ctx);
  }

  @Override
  public String visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String cName = st.peek();
    Class c = ct.get(cName);

    //コンストラクタがなければ生成
    if(c.cons == null){
      c.cons = new Constructor();
    }

    var preCondition = ctx.condition().get(0).constraints();
    var postCondition = ctx.condition().get(1).constraints();

    //事前条件があれば生成
    if(preCondition != null){
      c.cons.pre = new HashMap<String,Constraint>();
      conditionStack.push(c.cons.pre);
      visit(ctx.condition().get(0));
      conditionStack.pop();
    }

    //事後条件があれば生成
    if(postCondition != null){
      c.cons.post = new HashMap<String,Constraint>();
      conditionStack.push(c.cons.post);
      visit(ctx.condition().get(1));
      conditionStack.pop();
    }

    paramStack.push(c.cons.pmap);
    visit(ctx.formalParameters());
    paramStack.pop();
    return null;
  }

  @Override
  public String visitFormalParameter(JavaParser.FormalParameterContext ctx) {
    var pmap = paramStack.peek();
    String type = ctx.getChild(0).getText();
    String id = ctx.getChild(1).getText();
    pmap.put(id, type);
    return null;
  }

  @Override
  public String visitConstraint(JavaParser.ConstraintContext ctx) {
    HashMap<String,Constraint> condition = conditionStack.peek();

    //位置
    String location = ctx.IDENTIFIER().get(0).getText();

    Constraint c = new Constraint();
    c.className = st.peek();

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

    String cName = st.peek();
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
      conditionStack.push(m.pre);
      visit(ctx.condition().get(0));
      conditionStack.pop();
    }

    if(postCondition != null){
      m.post = new HashMap<String,Constraint>();
      conditionStack.push(m.post);
      visit(ctx.condition().get(1));
      conditionStack.pop();
    }

    paramStack.push(m.pmap);
    visit(ctx.formalParameters());
    paramStack.pop();

    return null;
  }

}
