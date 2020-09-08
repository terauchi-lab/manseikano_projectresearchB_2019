class A {}

class B extends A {
  A x;

  /*@ [pt; pt->{c:B, x:NULL}] @*/
  /*@ Ref pt@*/ A m(){
    return this.x = new B()/*@[][p]@*/;
  }
  /*@ [p; pt->{c:B,x:Ref p}, p->{c:B, x:NULL}] @*/
}

class C extends A{}

class Test2 {
  void main(){
    B b1 = new B()/*@[][p1]@*/;
    b1.m()/*@[p1][p2]@*/;
    b1.x = new C()/*@[][p3]@*/;
    b1.x = new A()/*@[][p4]@*/;
  }
}
