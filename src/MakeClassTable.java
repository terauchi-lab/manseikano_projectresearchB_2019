import java.util.*;
import static java.lang.System.*;
import org.antlr.v4.runtime.*;

public class MakeClassTable extends JavaParserBaseVisitor<String> {
  //クラステーブル
  public HashMap<String, Class> ct = new HashMap<String, Class>();
  //クラス名一時保管
  public Deque<String> st = new ArrayDeque<String>();
  //引数の型を一時保管
  public Deque<HashMap<String, String>> argStack = new ArrayDeque<HashMap<String, String>>();
  //コンストレイント一時保管
  public Deque<HashMap<String,Constraint>> conditionStack = new ArrayDeque<HashMap<String,Constraint>>();

  @Override
  public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
    String cName = ctx.IDENTIFIER().getText();
    ct.put(cName, new Class());
    st.addFirst(cName);
    visitChildren(ctx);
    st.removeFirst();
    return null;
  }

  @Override
  public String visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
    String cName = st.peekFirst();
    Class c = ct.get(cName);

    c.cons = new Constructor();

    String pre = ctx.SCOMMENT().get(0).getText();
    String retType = ctx.SCOMMENT().get(1).getText();
    String post = ctx.SCOMMENT().get(2).getText();

    var locAndCond = pre.trim().replaceAll(" ","").replaceAll("　","").substring(4,pre.length()-4).split(";");
    String locs = null;
    String constraints = null;

    if(locAndCond.length == 1){
      locs = locAndCond[0];
    }else if(locAndCond.length == 2){
      constraints = locAndCond[1];
    }

    //Delta_forallを追加
    if(locs != null){
      for (var loc : locs.split(",")){
        System.out.println(loc);
        c.cons.abstLocs.add(loc);
      }
    }

    //事前条件を追加
    if(constraints != null){
      c.cons.pre = new HashMap<String,Constraint>();
      Constraint newConst = new Constraint();

      for (var constraint : constraints.split("\\+")){
        var s = constraint.split("->");
        if(s.length == 2){
          var loc = s[0].substring(1);
          System.out.println(loc);

          var fields = s[1].substring(1,s[1].length()-2).split(",");
          for(int i=0; i<fields.length; i++){
            var val = fields[0];
            var type = fields[1];
            if(i==0){
              newConst.className = val;
            }else{
              newConst.fieldType.put(val,type);
            }
            newConst.fieldType.put(val,type);
          }
          c.cons.pre.put(loc, newConst);
        }

      }
    }

    //返り値型を追加
    var rType = retType.trim().replaceAll(" ","").replaceAll("　","").substring(3,pre.length()-3);
    c.cons.returnType = rType;

    locAndCond = post.trim().replaceAll(" ","").replaceAll("　","").substring(4,post.length()-4).split(";");
    locs = null;
    constraints = null;

    if(locAndCond.length == 1){
      locs = locAndCond[0];
    }else if(locAndCond.length == 2){
      constraints = locAndCond[1];
    }

    //Delta_existsを追加
    if(locs != null){
      for (var loc : locs.split(",")){
        System.out.println(loc);
        c.cons.bindLocs.add(loc);
      }
    }

    //事後条件を追加
    if(constraints != null){
      c.cons.post = new HashMap<String,Constraint>();
      Constraint newConst = new Constraint();

      for (var constraint : constraints.split("\\+")){
        var s = constraint.split("->");
        if(s.length == 2){
          var loc = s[0].substring(1);
          System.out.println(loc);

          var fields = s[1].substring(1,s[1].length()-2).split(",");
          for(int i=0; i<fields.length; i++){
            var val = fields[0];
            var type = fields[1];
            if(i==0){
              newConst.className = val;
            }else{
              newConst.fieldType.put(val,type);
            }
            newConst.fieldType.put(val,type);
          }
          c.cons.post.put(loc, newConst);
        }
      }
    }

    argStack.addFirst(c.cons.argType);
    visit(ctx.formalParameters());
    argStack.removeFirst();
    return null;
  }

  @Override
  public String visitFormalParameter(JavaParser.FormalParameterContext ctx) {
    var map = argStack.peekFirst();

    //メソッドをちゃんと定義すればいらない
    if(map != null){
      String type = ctx.getChild(0).getText();
      String id = ctx.getChild(1).getText();
      map.put(id, type);
    }
    return null;
  }

  //@Override
  //public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
  //  String type = ctx.typeTypeOrVoid().getText();
  //  String id = ctx.IDENTIFIER().getText();

  //  String cName = st.peekFirst();
  //  Class c = ct.get(cName);

  //  //メソッドがなければ生成
  //  if(c.methodMap == null){
  //    c.methodMap = new HashMap<String, Method>();
  //  }

  //  Method m = new Method();
  //  m.returnType = type;

  //  c.methodMap.put(id, m);

  //  var preCondition = ctx.condition(0);
  //  var postCondition = ctx.condition(1);

  //  if(preCondition != null){
  //    m.pre = new HashMap<String,Constraint>();
  //    conditionStack.addFirst(m.pre);
  //    visit(ctx.condition().get(0));
  //    conditionStack.removeFirst();
  //  }

  //  if(postCondition != null){
  //    m.post = new HashMap<String,Constraint>();
  //    conditionStack.addFirst(m.post);
  //    visit(ctx.condition().get(1));
  //    conditionStack.removeFirst();
  //  }

  //  paramStack.addFirst(m.pmap);
  //  visit(ctx.formalParameters());
  //  paramStack.removeFirst();

  //  return null;
  //}

}
