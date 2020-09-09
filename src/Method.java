import java.util.*;
class Method {
  public Type returnType;

  public LinkedList<String> abstLocs = new LinkedList<String>(); //Delta_forall
  public LinkedList<String> bindLocs = new LinkedList<String>(); //Delta_exists

  //Argument->Type
  public HashMap<String,Type> argType = new HashMap<String,Type>();

  //PreCondition
  public HashMap<String,Constraint> pre;

  //PostCcondition
  public HashMap<String,Constraint> post;
}

