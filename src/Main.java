import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Main {
  public static void main(String[] args) throws Exception {
    JavaLexer lexer = new JavaLexer(CharStreams.fromFileName(args[0]));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JavaParser parser = new JavaParser(tokens);
    ParseTree tree = parser.compilationUnit();

    //GUI表示
    //TreeViewer viewr = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
    //viewr.open();

    //クラステーブルを生成するvisitor
    MakeClassTable makeClassTable = new MakeClassTable();
    makeClassTable.visit(tree);

    //クラステーブルを保管
    Data.clsTable = makeClassTable.clsTable;

    //型付けを行うvisiotr
    TypeCheck typeCheck = new TypeCheck();
    typeCheck.visit(tree);
  }
}
