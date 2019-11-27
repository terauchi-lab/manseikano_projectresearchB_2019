import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
//import static java.lang.System.*;
//import static java.lang.Math.*;
import java.util.Arrays;
import org.antlr.v4.gui.TreeViewer;

public class Main {
  public static void main(String[] args) throws Exception {
    JavaLexer lexer = new JavaLexer(CharStreams.fromFileName("Test.java"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JavaParser parser = new JavaParser(tokens);
    ParseTree tree = parser.compilationUnit();

    //GUI表示
    //TreeViewer viewr = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
    //viewr.open();

    //リスナー
    //TypeCheckListener listener = new TypeCheckListener();
    //ParseTreeWalker walker = new ParseTreeWalker();
    //walker.walk(listener, tree);
    //Double result = listener.getResult();

    //ビジター
    Visitor visitor = new Visitor();
    visitor.visit(tree);

    //型環境デバッグ
    //for (String key : Visitor.typeContext.keySet()) {
    //  System.out.println(key + " => " + Visitor.typeContext.get(key));
    //}


    //System.out.println(result);
  }
}
