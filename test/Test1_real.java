public class Test1_real {
  public static void main(String[] args){
    B a = new B();
    a.ma();
    int tmp = a.x;
    int mp = a.y;
    //a.mb();
  }
}

class A {
  int x=3;
  void ma(){
    System.out.println("call ma");
  }
}

class B extends A {
  int y=1;
  void mb(){
    System.out.println("call mb");
  }
}

