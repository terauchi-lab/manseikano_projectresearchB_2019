import java.util.ArrayList;
interface IType {
    String getTypeName();
    boolean subType(IType t); //whether this.IType <: IType
    IType substitute(ArrayList<String> userLoc, ArrayList<String> formalLoc); //ITpye[userLoc/formalLoc]
}
