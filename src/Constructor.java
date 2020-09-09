import java.util.*;
class Constructor {
  public String returnType;

  public LinkedList<String> abstLocs = new LinkedList<String>(); //Delta_forall
  public LinkedList<String> bindLocs = new LinkedList<String>(); //Delta_exists

  //Argument->Type
  public HashMap<String,String> argType = new HashMap<String,String>();

  //PreCondition
  public HashMap<String,Constraint> pre = new HashMap<String,Constraint>();

  //PostCcondition
  public HashMap<String,Constraint> post = new HashMap<String,Constraint>();
}
