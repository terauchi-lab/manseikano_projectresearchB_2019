class Park {
  Animal animal;

  /*@ Ref p_this @*/ Park(){
    this.animal = new Dog()/*@ [][p1] @*/;
  }
  /*@ [p_this->{c:A, animal:Ref p1}, p1->{c:Dog}] @*/
}

class Animal {
  /*@ Ref p_this @*/ Animal(){}
  /*@ [p_this->{c:Animal}] @*/
}

class Dog extends Animal {
  /*@ Ref p_this @*/ Dog(){}
  /*@ [p_this->{c:Dog}] @*/

  /*@ [p_this->{c:Dog}] @*/
  boolean bark(){
    return true;
  }
  /*@ [p_this->{c:Dog}] @*/

}

class Cat extends Animal {

  /*@ Ref p_this @*/ Cat(){}
  /*@ [p_this->{c:Cat}] @*/

  /*@ [p_this->{c:Cat}] @*/
  boolean nyan(){
    return false;
  }
  /*@ [p_this->{c:Cat}] @*/
}

class Test1 {
  public static void main(String[] args){
    Park park = new Park()/*@ [][p1,p2] @*/;
    Animal animal = park.animal;
    assert (animal instanceof Cat);
  }
}
