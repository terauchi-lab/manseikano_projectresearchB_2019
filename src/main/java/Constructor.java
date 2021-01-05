import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

class Constructor {
  public IType returnType;

  public ArrayList<String> abstLocs = new ArrayList<>(); //Delta_forall
  public ArrayList<String> bindLocs = new ArrayList<>(); //Delta_exists

  //Argument->Type
  public LinkedHashMap<String, IType> argTypes = new LinkedHashMap<>();

  //PreConstraint
  public HashMap<String, ObjectType> pre = new HashMap<>();

  //PostConstraint
  public HashMap<String, ObjectType> post = new HashMap<>();
}
