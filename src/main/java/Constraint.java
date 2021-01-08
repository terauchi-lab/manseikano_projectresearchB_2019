import java.util.ArrayList;
import java.util.HashMap;

public class Constraint {

    //位置変数の置換
    public static String substitute(String location, ArrayList<String> userLocs,
                                    ArrayList<String> formalLocs){
        for (int i = 0; i < formalLocs.size(); i++) {
            if(location.equals(formalLocs.get(i))){
                return userLocs.get(i);
            }
        }
        return location;
    }

    //制約に現れる位置変数の置換 c[formalLocs/userLocs]
    public static HashMap<String, ObjectType> substitute(HashMap<String, ObjectType> c,
                                                         ArrayList<String> userLocs,
                                                         ArrayList<String> formalLocs){

        var substitutedC = new HashMap<String, ObjectType>();

        for (String location : c.keySet()) {
            var loc = substitute(location, userLocs, formalLocs);
            var objType = c.get(location).substitute(userLocs,formalLocs);
            substitutedC.put(loc, objType);
        }
        return substitutedC;
    }

    static boolean isSubClass(ObjectType subObj, ObjectType obj){
        //同じクラス名
        if(subObj.className.equals(obj.className)){
            return true;
        }else{
            //継承関係かどうか
            var superClassName = Data.clsTable.get(subObj.className).sClassName;
            return superClassName.equals(obj.className);
        }
    }

    //c_s <: c[formalLocs/userLocs]
    public static boolean isSubConstraint(HashMap<String, ObjectType> c_s,
                                        HashMap<String, ObjectType> c,
                                        ArrayList<String> userLocs,
                                        ArrayList<String> formalLocs){
        //要求する位置の数が異なる場合
        if(formalLocs.size() != userLocs.size()){
            return false;
        }

        var substitutedC = substitute(c, userLocs, formalLocs);

        for (var location : substitutedC.keySet()) {

            //c[formalLocs/userLocs]にある位置がc_sになかったら
            if(!c_s.containsKey(location)){
                return false;
            }
            var subObjectType = c_s.get(location);
            var objectType = substitutedC.get(location);

            //オブジェクトのクラスが部分型関係になっているか
            if(!isSubClass(subObjectType, objectType)){
                return false;
            }

            //それぞれのフィールドが部分型関係になっているか
            for (var field : objectType.fieldTypes.keySet()) {
                var subType = subObjectType.fieldTypes.get(field);

                //そもそもフィールドがない
                if(subType == null){
                    return false;
                }

                //部分型じゃない
                if(!subType.subType(objectType.fieldTypes.get(field))){
                    return false;
                }
            }
        }

        return true;
    }

    //制約のコピー
    public static HashMap<String, ObjectType> clone(HashMap<String, ObjectType> c){
        var copyC = new HashMap<String, ObjectType>();
        for (var loc : c.keySet()) {
            var copyObjType = c.get(loc).clone();
            copyC.put(loc, copyObjType);
        }
        return copyC;
    }

    //p1->{...}, p2->{...}形式で返す
    public static String toString(HashMap<String, ObjectType> c){
        var str = new ArrayList<String>();
        for (var loc : c.keySet()) {
            var objStr = new ArrayList<String>();

            var objectType = c.get(loc);
            objStr.add("c:"+objectType.className);
            for (var field : objectType.fieldTypes.keySet()) {
                objStr.add(field+":"+objectType.fieldTypes.get(field).getTypeName());
            }

            str.add(loc+"->"+"{"+String.join(",", objStr)+"}");
        }
        return String.join(", ", str);
    }
}
