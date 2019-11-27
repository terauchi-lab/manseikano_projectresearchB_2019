import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

public class Visitor extends JavaParserBaseVisitor<Type> {

  //型環境
  public HashMap<String, Type> typeContext = new HashMap<String, Type>();
  public int errorCnt = 0;

  public String fileName;

  @Override
  public Type visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
    Type type = visit(ctx.typeType());
    String[] names = ctx.variableDeclarators().getText().split(",");

    for(int i=0; i < names.length; i++){
      String[] varArray = names[i].split("=");
      String name = varArray[0];

      //変数名がかぶっていないかチェック
      if(typeContext.containsKey(name)){
        Token startPos = ctx.getStart();
        int line = startPos.getLine();
        int chara = startPos.getCharPositionInLine()+1;
        out.println(fileName+" at line "+line+", charater "+chara+", error: Variable "+name+" is already defined");
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
    if(ctx.getChildCount() >= 3){
      String variable = ctx.getChild(0).getText();
      if(typeContext.containsKey(variable)){
        Type leftT = typeContext.get(variable);
        Type rightT = visit(ctx.getChild(2));

        //型が一致しているかチェック
        if(leftT != rightT){
          Token startPos = ctx.getStart();
          int line = startPos.getLine();
          int chara = startPos.getCharPositionInLine()+1;
          out.println(fileName+" at line "+line+", charater "+chara+", error: "+rightT+" cannot be converted to "+leftT);
          errorCnt++;
        }
      }
    }
    return null;
  }

  @Override
  public Type visitClassOrInterfaceType(JavaParser.ClassOrInterfaceTypeContext ctx) {
    String name = ctx.getChild(0).getText();
    if (name.equals("String")){
      return Type.String;
    }else if(name.equals("Boolean")){
      return Type.Boolean;
    }else{
      return visitChildren(ctx);
    }
  }

  @Override
  public Type visitBoolean(JavaParser.BooleanContext ctx) {
    return Type.Boolean;
  }

  @Override
  public Type visitChar(JavaParser.CharContext ctx) {
    return Type.Char;
  }

  @Override
  public Type visitInt(JavaParser.IntContext ctx) {
    return Type.Int;
  }

  @Override
  public Type visitDouble(JavaParser.DoubleContext ctx) {
    return Type.Double;
  }

  @Override
  public Type visitExpression(JavaParser.ExpressionContext ctx) {
    Type leftT = null;

    if(ctx.getChildCount() >= 3){
      //代入
      if(ctx.bop.getText().equals("=")){
        leftT = visit(ctx.expression(0));
        Type rightT = visit(ctx.expression(1));

        //型が一致しているかチェック
        if(leftT != rightT){
          Token startPos = ctx.getStart();
          int line = startPos.getLine();
          int chara = startPos.getCharPositionInLine()+1;
          out.println(fileName+" at line "+line+", charater "+chara+", error: "+rightT+" cannot be converted to "+leftT);
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
    return Type.Double;
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
