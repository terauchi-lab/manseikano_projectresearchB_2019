class A {
    /*@ Ref p_this @*/ A(){}
    /*@ [p_this->{c:A}] @*/
}

class B extends A {
    A x;
    /*@ [pa->{c:A}] @*/
    /*@ Ref p_this @*/ B(/*@ Ref pa @*/ A pa){
        this.x = pa;
    }
    /*@ [p_this->{c:B, x:Ref pa}, pa->{c:A}] @*/
}

class Test3 {
    public static void main(String[] args){
        A a = new A()/*@ [][p1] @*/;
        B b = new B(a)/*@ [p1][p2] @*/;
        b.x = b;
    }
}
