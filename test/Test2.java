class A {
  ptr(_) x;

  []
  A(ptr(py) y){
    x = y;
  }
  [p, py; {p -> {c:A, x:ptr(py)}}]

}

class B {
  ptr(_) x;
  int z;

  []
  A(ptr(py) y){
    x = y;
  }
  [p, py; {p -> {c:A, x:ptr(py), z:int}}]

}

class Main {
  void main(){
    ptr(pt1) a = new A(new A(null)[x1])[pt2];
    a.x = new B(null)[x2];
  }
}
