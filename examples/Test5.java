class Data {
  A a;
  /*@ Ref p_this @*/ Data(){}
  /*@ [p_this->{c:Data, a:NULL}] @*/
}

class A {
  /*@ Ref p_this @*/ A(){}
  /*@ [p_this->{c:A}] @*/
}

class B extends A {
  /*@ Ref p_this @*/ B(){}
  /*@ [p_this->{c:B}] @*/
}

class C extends A{
  /*@ Ref p_this @*/ C(){}
  /*@ [p_this->{c:C}] @*/
}

class Test5 {
  public static void main(String[] args){
    Data data = new Data()/*@[][p1]@*/;
    data.a = new A()/*@[][p2]@*/;
    data.a = new B()/*@[][p3]@*/;
    data.a = new C()/*@[][p4]@*/;
    assert(data.a instanceof B);
    B b = (B)data.a;
  }
}
