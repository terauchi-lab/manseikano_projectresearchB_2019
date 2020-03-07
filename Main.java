import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import static java.lang.System.*;
import static java.lang.Math.*;
import java.util.*;
import java.util.Arrays;
import org.antlr.v4.gui.TreeViewer;

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
    Data.ct = makeClassTable.ct;

    //デバッグ
    printClassTable(Data.ct);
    out.println("**Debug**");

    //型付けを行うvisiotr
    TypeCheck typeCheck = new TypeCheck();
    typeCheck.visit(tree);
  }

  //クラステーブルを出力
  public static void printClassTable(HashMap<String, Class> ct){
    out.println();
    out.println("**Class Table**");
    for (String key : ct.keySet()) {
      out.println("class: "+key);
      System.out.println("  "+key + " => " + ct.get(key));

      Class c = ct.get(key);

      out.println("Field");
      if(c.fmap != null){
        for (String id : c.fmap.keySet()) {
          System.out.println("  "+id + " => " + c.fmap.get(id));
        }
      }

      out.println("Constructor");
      Constructor cons = ct.get(key).cons;
      out.println("  Params");
      if(cons != null){
        for (String id : cons.pmap.keySet()) {
          System.out.println("    "+id + " => " + cons.pmap.get(id));
        }

        out.println("  Pre Condition");
        if(cons.pre != null){
          for (String id : cons.pre.keySet()) {
            Constraint constraint = cons.pre.get(id);
            out.println("    "+constraint.className+ " => ");
            for (String val : constraint.cmap.keySet()) {
              System.out.println("      "+val + " => " + constraint.cmap.get(val));
            }
          }
        }
        out.println("  Post Condition");
        if(cons.post != null){
          for (String id : cons.post.keySet()) {
            Constraint constraint = cons.post.get(id);
            out.println("    "+id+ " => ");
            out.println("      "+"c : "+constraint.className);
            for (String val : constraint.cmap.keySet()) {
              System.out.println("      "+val + " => " + constraint.cmap.get(val));
            }
          }
        }
      }

      out.println("Method");
      if(c.methodMap != null){
        for (String id : c.methodMap.keySet()) {
          out.println("method: "+id);

          Method m = c.methodMap.get(id);
          out.println("  Params");
          for (String val : m.pmap.keySet()) {
            System.out.println("    "+val + " => " + m.pmap.get(val));
          }
          out.println("  Return Type");
          out.println("    "+m.returnType);

          out.println("  Pre Condition");
          if(m.pre != null){
            for (String loc : m.pre.keySet()) {
              Constraint constraint = m.pre.get(loc);
              out.println("    "+constraint.className+ " => ");
              for (String val : constraint.cmap.keySet()) {
                System.out.println("      "+val + " => " + constraint.cmap.get(val));
              }
            }
          }
          out.println("  Post Condition");
          if(m.post != null){
            for (String loc : m.post.keySet()) {
              Constraint constraint = m.post.get(loc);
              out.println("    "+id+ " => ");
              out.println("      "+"c : "+constraint.className);
              for (String val : constraint.cmap.keySet()) {
                System.out.println("      "+val + " => " + constraint.cmap.get(val));
              }
            }
          }
        }
      }

      out.println();
    }
  }

}
