class A {
  ptr(_) x;
  int tmp = 3;

  []
  A(ptr(py) y){
    this.x = y;
  }
  [p, py; {p -> {c:A, x:ptr(py)}}]

}

class Main {
  void main(){
    ptr(p1) a = new A(new A(null)[p3])[p2];
    a.x = new A(null)[p4];
  }
}
