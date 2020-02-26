# 環境
- Java
  - 10.0.1
- antlr
  - 4.7.2

# 動作確認
- 構文解析器と型検査器を生成
  - `antlr4 JavaLexer.g4 JavaParser.g4 -visitor -no-listener`
- コンパイル
  - `javac Main.java`
- 実行
  - コマンドライン引数でプログラムを指定
  - `java Main test/Test1.java`

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

class Main {
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
- MakeClassTable.java
  - クラステーブルを生成
- TypeCheck.java
  - 型検査器
- Main.java
  - メインプログラム
- test/*.java
  - テストファイル
