import static java.lang.System.*;
public enum Type {
  Int,
  String,
  Boolean,
  Char,
  Byte,
  Long,
  Float,
  Double,
  Ok,
  Null,
  ;

  public static Type getType(String name){
    //先頭の文字列を大文字にする ex) float->Float
    String typeName = name.substring(0, 1).toUpperCase() + name.substring(1);

    Type[] types = Type.values();
    for (int i = 0; i < types.length; i++) {
      if (typeName.equals(types[i].name())){
        return types[i];
      }
    }
    return null;
  }
}
