class A {
  /*@ Ref p_this @*/ A(){}
  /*@ [p_this->{c:A}] @*/
}

class B extends A {
  A x;

  /*@ Ref p_this @*/ B(){}
  /*@ [p_this->{c:B, x:NULL}] @*/

  /*@ [p_this->{c:B, x:NULL}] @*/
  /*@ Ref p_this @*/ A m(){
    return this.x = new B()/*@[][p]@*/;
  }
  /*@ [p_this->{c:B,x:Ref p}, p->{c:B, x:NULL}] @*/
}

class C extends A{
  /*@ Ref p_this @*/ C(){}
  /*@ [p_this->{c:C}] @*/
}

class Test4 {
  public static void main(String[] args){
    B b1 = new B()/*@[][p1]@*/;
    b1.m()/*@[p1][p2]@*/;
    b1.x = new C()/*@[][p3]@*/;
    b1.x = new A()/*@[][p4]@*/;
  }
}
