import java.util.*;
class Constructor {
  public String returnType;

  public ArrayList<String> abstLocs = new ArrayList<>(); //Delta_forall
  public ArrayList<String> bindLocs = new ArrayList<>(); //Delta_exists

  //Argument->Type
  public LinkedHashMap<String,String> argTypes = new LinkedHashMap<>();

  //PreConstraint
  public HashMap<String, ObjectType> pre = new HashMap<>();

  //PostConstraint
  public HashMap<String, ObjectType> post = new HashMap<>();
}
