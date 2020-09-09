class A {}

class B extends A {
  A x;
  /*@ [pa;] @*/
  /*@ Ref p @*/ B(/*@ Ref pa @*/ A pa){
    x = pa;
  }
  /*@ [p; p->{c:B, x:Ref pa}] @*/
}

class Test1 {
  public static void main(String[] args){
    A a = new A()/*@ [][p1] @*/;
    B b = new B(a)/*@ [p1][p2] @*/;
    b.x = b;
  }
}
