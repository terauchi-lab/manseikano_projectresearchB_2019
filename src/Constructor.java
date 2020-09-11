import java.util.*;
class Constructor {
  public String returnType;

  public ArrayList<String> abstLocs = new ArrayList<String>(); //Delta_forall
  public ArrayList<String> bindLocs = new ArrayList<String>(); //Delta_exists

  //Argument->Type
  public HashMap<String,String> argType = new HashMap<String,String>();

  //PreCondition
  public HashMap<String,Constraint> pre = new HashMap<String,Constraint>();

  //PostCcondition
  public HashMap<String,Constraint> post = new HashMap<String,Constraint>();
}
