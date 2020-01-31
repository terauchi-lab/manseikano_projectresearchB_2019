# 環境構築
- 定義ファイルを元に構文解析器とVisitorを生成
  - `antlr4 JavaLexer.g4 JavaParser.g4 -visitor -no-listener`
- コンパイル
  - `javac Main.java`
- 実行(コマンドライン引数で指定)
  - `java Main test/Test2.java`

# 入力として受け取れるサンプルコード
```java
class A {
  ptr(_) x;

  []
  A(ptr(py) y){
    this.x = y;
  }
  [p, py; {p -> {c:A, x:ptr(py)}}]
}

class Test2 {
  void main(){
    ptr(p1) a = new A(new A(null)[p3])[p2];
    a.x = new A(null)[p4];
  }
}
```

# ファイルについての説明
- JavaLexer.g4
  - Lexerの定義ファイル
- JavaParser.g4
  - Parserの定義ファイル
- Visitor.java
  - Antlr4が生成した構文木をvisitパターンで型付けする
- Main.java
  - コマンドライン引数で与えられたプログラムに対して構文解析と型付けをする
- Test
  - テストファイル
