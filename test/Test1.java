class A {
  void ma(){}
}

class B {
  void ma(){}
  void mb(){}
}

class Test1 {
  void main(){
    /*ptr(p1)*/ a = new B();
    a.mb();
  }
}
