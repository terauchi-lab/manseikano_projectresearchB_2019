class A {
  /*@ Ref p @*/ A(){}
  /*@ [p; p->{c:A}] @*/
}

class B extends A {
  A x;

  /*@ Ref p @*/ B(){}
  /*@ [p; p->{c:B, x:NULL}] @*/

  /*@ [pt; pt->{c:B, x:NULL}] @*/
  /*@ Ref pt@*/ A m(){
    return this.x = new B()/*@[][p]@*/;
  }
  /*@ [p; pt->{c:B,x:Ref p}, p->{c:B, x:NULL}] @*/
}

class C extends A{
  /*@ Ref p @*/ C(){}
  /*@ [p; p->{c:C}] @*/
}

class Test2 {
  public static void main(String[] args){
    B b1 = new B()/*@[][p1]@*/;
    b1.m()/*@[p1][p2]@*/;
    b1.x = new C()/*@[][p3]@*/;
    b1.x = new A()/*@[][p4]@*/;
  }
}
