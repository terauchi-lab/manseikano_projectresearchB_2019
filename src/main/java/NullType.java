import java.util.ArrayList;

class NullType implements IType{
    @Override
    public String getTypeName() {
        return "NULL";
    }

    @Override
    public boolean subType(IType t) {
        //NULL<:Ref
        if(t.getTypeName().contains("Ref")){
            return true;
        }else{
            //whether NULL <: NULL
            return getTypeName().equals(t.getTypeName());
        }
    }

    @Override
    public IType substitute(ArrayList<String> userLoc, ArrayList<String> formalLoc) {
        return this;
    }
}
