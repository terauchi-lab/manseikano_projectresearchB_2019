import java.util.*;
import java.io.*;

//基本型の変数初期化, 宣言, 代入の型チェック
public class Test1_err {
  public static void main(String[] args){
    int a;
    a = 100;
    double a = 100.0;

    int b = 3, c = 1.4;

    double d = true;
    double e,f;
    e = f = "st";

    char ch = "c";

    String st = 's';

    Boolean bool = 100;
    boolean bool = 10;
  }
}
