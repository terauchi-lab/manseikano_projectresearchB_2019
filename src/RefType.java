import java.util.ArrayList;

class RefType implements IType {
    private String location;

    RefType(String s){
        this.location = s;
    }

    @Override
    public String getTypeName(){
        return "Ref "+this.location;
    }

    @Override
    public boolean subType(IType t){
       return getTypeName().equals(t.getTypeName());
    }

    //Ref p[userLoc/formalLoc]
    @Override
    public IType substitute(ArrayList<String> userLoc, ArrayList<String> formalLoc) {
        for (int i = 0; i < formalLoc.size(); i++) {
            if(location.equals(formalLoc.get(i))){
                return new RefType(userLoc.get(i));
            }
        }
        return new RefType(location);
    }

    public String getLocation(){
        return location;
    }
}
