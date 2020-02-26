import java.util.*;
class Constructor {
  //Param->Type
  public HashMap<String,String> pmap = new HashMap<String,String>();

  //PreCondition
  //Location -> Precondition
  public HashMap<String,Constraint> pre;

  //PostCcondition
  //Location -> Postcondition
  public HashMap<String,Constraint> post;
}
