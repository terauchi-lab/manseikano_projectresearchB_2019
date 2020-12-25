import java.util.ArrayList;

public enum PrimitiveType implements IType {
    INT("int"),
    BOOL("boolean"),
    ;

    final String str;

    PrimitiveType(final String str){
         this.str = str;
     }

    @Override
    public String getTypeName() {
        return this.str;
    }

    @Override
    public boolean subType(IType t) {
        return str.equals(t.getTypeName());
    }

    @Override
    public IType substitute(ArrayList<String> userLoc, ArrayList<String> formalLoc) {
        return this;
    }

    //String -> Enum
    public static PrimitiveType getType(final String s) {
        PrimitiveType[] types = PrimitiveType.values();
        for (var type : types) {
            if (type.str.equals(s)) {
                return type;
            }
        }
        return null;
    }
}
