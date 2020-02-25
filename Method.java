import java.util.*;
class Method {
  //Param->Type
  public HashMap<String,String> pmap = new HashMap<String,String>();

  //PreCondition
  //Location -> Precondition
  public HashMap<String,Constraint> pre;

  //PosCcondition
  //Location -> Precondition
  public HashMap<String,Constraint> post;

  //ReturnType
  public String returnType;
}

