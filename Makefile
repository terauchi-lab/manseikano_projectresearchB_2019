ANTLR = java -jar /usr/local/lib/antlr-4.7.2-complete.jar

main: Main.java
	${ANTLR} JavaLexer.g4 JavaParser.g4 -visitor -no-listener
	javac Main.java

clean:
	rm *.class

test1:
	java Main test/Test1.java
