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

  Class(){
    fmap = new HashMap<String,String>();
    cons = new Constructor();
    methodMap = new HashMap<String, Method>();
  }
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
  public Stack<HashMap<String,Constraint>> condStack = new Stack<HashMap<String,Constraint>>();

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
    String cName = st.peek();
    String type = ctx.getChild(0).getText();
    String id = ctx.getChild(1).getText();
    Class c = ct.get(cName);

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
    Constructor cons = ct.get(cName).cons;

    var preCondition = ctx.condition().get(0).constraints();
    var postCondition = ctx.condition().get(1).constraints();

    if(preCondition != null){
      cons.pre = new HashMap<String,Constraint>();
      condStack.push(cons.pre);
      visit(ctx.condition().get(0));
      condStack.pop();
    }

    if(postCondition != null){
      cons.post = new HashMap<String,Constraint>();
      condStack.push(cons.post);
      visit(ctx.condition().get(1));
      condStack.pop();
    }

    paramStack.push(cons.pmap);
    visit(ctx.formalParameters());
    var pmap = paramStack.pop();
    cons.pmap = pmap;
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
    HashMap<String,Constraint> condition = condStack.peek();

    //位置
    String location = ctx.IDENTIFIER().get(0).getText();

    //コンストレイント
    Constraint c = new Constraint();
    c.className = st.peek();

    //各変数の型
    for(int i=0; i<ctx.param().size(); i++){
      String id = ctx.param().get(i).IDENTIFIER().getText();
      String type = ctx.param().get(i).typeType().getText();
      c.cmap.put(id, type);
    }

    condition.put(location, c);
    return visitChildren(ctx);
  }

  @Override
  public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
    String type = ctx.typeTypeOrVoid().getText();
    String id = ctx.IDENTIFIER().getText();
    String cName = st.peek();

    Class c = ct.get(cName);
    Method m = new Method();
    m.returnType = type;
    c.methodMap.put(id, m);

    var preCondition = ctx.condition(0);
    var postCondition = ctx.condition(1);

    if(preCondition != null){
      m.pre = new HashMap<String,Constraint>();
      condStack.push(m.pre);
      visit(ctx.condition().get(0));
      condStack.pop();
    }

    if(postCondition != null){
      m.post = new HashMap<String,Constraint>();
      condStack.push(m.post);
      visit(ctx.condition().get(1));
      condStack.pop();
    }

    paramStack.push(m.pmap);
    visit(ctx.formalParameters());
    var param = paramStack.pop();
    m.pmap = param;

    return visitChildren(ctx);
  }

  //@Override
  //public Type visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
  //  Type type = visit(ctx.typeType());
  //  String[] names = ctx.variableDeclarators().getText().split(",");

  //  for(int i=0; i < names.length; i++){
  //    String[] varArray = names[i].split("=");
  //    String name = varArray[0];

  //    //変数名がかぶっていないかチェック
  //    if(typeContext.containsKey(name)){
  //      Token startPos = ctx.getStart();
  //      int line = startPos.getLine();
  //      int chara = startPos.getCharPositionInLine()+1;
  //      out.println(fileName+" at line "+line+", charater "+chara+", error: Variable "+name+" is already defined");
  //      errorCnt++;
  //    }else{
  //      typeContext.put(name, type);
  //    }
  //  }

  //  visitChildren(ctx);

  //  return type;
  //}

  //@Override
  //public Type visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
  //  //変数の初期化
  //  if(ctx.getChildCount() >= 3){
  //    String variable = ctx.getChild(0).getText();
  //    if(typeContext.containsKey(variable)){
  //      Type leftT = typeContext.get(variable);
  //      Type rightT = visit(ctx.getChild(2));

  //      //型が一致しているかチェック
  //      if(leftT != rightT){
  //        Token startPos = ctx.getStart();
  //        int line = startPos.getLine();
  //        int chara = startPos.getCharPositionInLine()+1;
  //        out.println(fileName+" at line "+line+", charater "+chara+", error: "+rightT+" cannot be converted to "+leftT);
  //        errorCnt++;
  //      }
  //    }
  //  }
  //  return null;
  //}

  //@Override
  //public Type visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
  //  String name = ctx.getChild(0).getText();
  //  if (name.equals("String")){
  //    return Type.String;
  //  }else if(name.equals("Boolean")){
  //    return Type.Boolean;
  //  }else{
  //    return visitChildren(ctx);
  //  }
  //}

  //@Override
  //public Type visitExpression(JavaParser.ExpressionContext ctx) {
  //  Type leftT = null;

  //  if(ctx.getChildCount() >= 3){
  //    //代入
  //    if(ctx.bop.getText().equals("=")){
  //      leftT = visit(ctx.expression(0));
  //      Type rightT = visit(ctx.expression(1));

  //      //型が一致しているかチェック
  //      if(leftT != rightT){
  //        Token startPos = ctx.getStart();
  //        int line = startPos.getLine();
  //        int chara = startPos.getCharPositionInLine()+1;
  //        out.println(fileName+" at line "+line+", charater "+chara+", error: "+rightT+" cannot be converted to "+leftT);
  //        errorCnt++;
  //      }
  //    }
  //    return leftT;
  //  }

  //  return visitChildren(ctx);
  //}

  //@Override
  //public Type visitPrimaryIdentifier(JavaParser.PrimaryIdentifierContext ctx) {
  //  String id = ctx.getText();
  //  if (typeContext.containsKey(id)){
  //    return typeContext.get(id);
  //  }else{
  //    return null;
  //  }
  //}

}
