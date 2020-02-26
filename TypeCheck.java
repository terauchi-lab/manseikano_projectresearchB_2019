import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

public class TypeCheck extends JavaParserBaseVisitor<String> {
  //クラス名一時保管
  public Stack<String> st = new Stack<String>();

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
    if(ctx.bop.getText().equals(".")){
      String val = ctx.IDENTIFIER().getText();
    }

    //newのとき
    if(ctx.NEW() != null){
      String val = ctx.IDENTIFIER().getText();
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

}
