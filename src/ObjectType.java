import java.util.HashMap;
import java.util.ArrayList;

class ObjectType {
  public String className;
  //field->Type
  public HashMap<String, IType> fieldTypes = new HashMap<>();

  public ObjectType substitute(ArrayList<String> userLocs, ArrayList<String> formalLocs){
    var newObjType = new ObjectType();
    newObjType.className = this.className;

    for (var field : this.fieldTypes.keySet()) {
      var type = this.fieldTypes.get(field).substitute(userLocs,formalLocs);
      newObjType.fieldTypes.put(field,type);
    }
    return newObjType;
  }

  public ObjectType clone(){
    var newObjType = new ObjectType();
    newObjType.className = this.className;
    for (var val: this.fieldTypes.keySet()) {
      newObjType.fieldTypes.put(val, this.fieldTypes.get(val));
    }
    return newObjType;
  }
}

