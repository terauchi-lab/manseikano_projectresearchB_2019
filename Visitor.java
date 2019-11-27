import java.util.*;
import static java.lang.System.*;

public class Visitor extends JavaParserBaseVisitor<Type> {

  //型環境
  public static HashMap<String, Type> typeContext = new HashMap<String, Type>();

  public static int errorCnt = 0;

  @Override
  public Type visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
    Type type = visit(ctx.typeType());
    String[] names = ctx.variableDeclarators().getText().split(",");

    for(int i=0; i < names.length; i++){
      String[] varArray = names[i].split("=");
      String name = varArray[0];

      if(typeContext.containsKey(name)){
        out.println("Variable "+name+" is already defined");
        errorCnt++;
      }else{
        typeContext.put(name, type);
      }
    }

    visitChildren(ctx);

    return type;
  }

  @Override
  public Type visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
    //変数の初期化
    out.println("hello");
    if(ctx.getChildCount() >= 3){
      String variable = ctx.getChild(0).getText();
      out.println(variable);
      if(typeContext.containsKey(variable)){
        Type leftT = typeContext.get(variable);
        Type rightT = visit(ctx.getChild(2));

        //型が一致しているかチェック
        if(leftT != rightT){
          out.println(rightT+" cannot be converted to "+leftT);
          errorCnt++;
        }
      }
    }
    return null;
  }


  @Override
  public Type visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
    String typeName = ctx.getText();
    Type type = Type.getType(typeName);
    return type;
  }

  @Override
  public Type visitExpression(JavaParser.ExpressionContext ctx) {
    Type leftT = null;

    if(ctx.getChildCount() >= 3){
      //代入の型検査
      if(ctx.bop.getText().equals("=")){
        leftT = visit(ctx.expression(0));
        Type rightT = visit(ctx.expression(1));

        out.println(leftT.name()+" = "+rightT.name());

        if(leftT != rightT){
          out.println(rightT+" cannot be converted to "+leftT);
          errorCnt++;
        }
      }
      return leftT;
    }

    return visitChildren(ctx);
  }

  @Override
  public Type visitPrimaryIdentifier(JavaParser.PrimaryIdentifierContext ctx) {
    String id = ctx.getText();
    if (typeContext.containsKey(id)){
      return typeContext.get(id);
    }else{
      return null;
    }
  }

  @Override
  public Type visitIntLiteral(JavaParser.IntLiteralContext ctx) {
    return Type.Int;
  }

  @Override
  public Type visitFlLiteral(JavaParser.FlLiteralContext ctx) {
    String value = ctx.getText();
    String id = value.substring(value.length()-1);

    if(id.equals("f") || id.equals("F")){
      return Type.Float;
    } else {
      return Type.Double;
    }
  }

  @Override
  public Type visitCharLiteral(JavaParser.CharLiteralContext ctx) {
    return Type.Char;
  }

  @Override
  public Type visitStrLiteral(JavaParser.StrLiteralContext ctx) {
    return Type.String;
  }

  @Override
  public Type visitBoolLiteral(JavaParser.BoolLiteralContext ctx) {
    return Type.Boolean;
  }

  @Override
  public Type visitNullLiteral(JavaParser.NullLiteralContext ctx) {
    return Type.Null;
  }

}
