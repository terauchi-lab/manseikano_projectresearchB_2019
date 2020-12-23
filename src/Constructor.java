import java.util.*;
class Constructor {
  public String returnType;

  public ArrayList<String> abstLocs = new ArrayList<>(); //Delta_forall
  public ArrayList<String> bindLocs = new ArrayList<>(); //Delta_exists

  //Argument->Type
  public LinkedHashMap<String,String> argType = new LinkedHashMap<>();

  //PreCondition
  public HashMap<String,Constraint> pre = new HashMap<>();

  //PostCcondition
  public HashMap<String,Constraint> post = new HashMap<>();
}
