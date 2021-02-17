import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class MainController {

    @FXML
    private AnchorPane sourceArea;

    @FXML
    private TextArea resultTextArea;

    @FXML
    private TextArea ctTextArea;

    //ソースコードのUI
    private static CodeArea codeArea = new CodeArea();
    private static ExecutorService executor;

    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    @FXML
    void initialize() {
        assert resultTextArea != null : "fx:id=\"resultTextArea\" was not injected: check your FXML file 'MainView.fxml'.";

        //コード読み込み
        var code = new StringBuilder();
        try {
            Files.readAllLines(Paths.get(Data.sourceFile), Charset.defaultCharset()).forEach(s -> code.append(s).append("\n"));
        } catch (IOException e){
            var errormsg = "Can't Open File in Tab pane";
        }

        //動的に設置
        AnchorPane.setBottomAnchor(codeArea, 0.0);
        AnchorPane.setTopAnchor(codeArea, 0.0);
        AnchorPane.setLeftAnchor(codeArea, 0.0);
        AnchorPane.setRightAnchor(codeArea, 0.0);
        sourceArea.getChildren().add(codeArea);
        codeArea.setEditable(false);

        //行番号表示
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        //シンタックスハイライト
        executor = Executors.newSingleThreadExecutor();
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(500))
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap(t -> {
                    if(t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(this::applyHighlighting);
        codeArea.replaceText(code.toString());

        //main関数の型付け
        var sb = new StringBuilder();
        for (var debugInfo : Data.mainDebugInfo) {
            sb.append("Line ").append(debugInfo.line).append(":").append("\n");
            sb.append("Γ = ").append(debugInfo.typeEnv).append("\n");
            sb.append("C = ").append(debugInfo.constraint).append("\n\n");
        }

        if(Data.error != null){
            sb.append("Line ").append(Data.errorLine).append(":").append("\n");
            sb.append("Error: ");
            sb.append(Data.error);
        }
        resultTextArea.setText(sb.toString());

        //クラステーブルの出力
        var clsTableSb = new StringBuilder();
        for (var clsName : Data.clsTable.keySet()) {
            sb = new StringBuilder();
            var cls = Data.clsTable.get(clsName);

            if(cls.methods != null && cls.methods.containsKey("main")) continue;

            sb.append("Class: ").append(clsName).append("\n");

            sb.append("Constructor: ");
            sb.append(getType(cls.cons.abstLocs, cls.cons.bindLocs, cls.cons.argTypes, cls.cons.returnType, cls.cons.pre, cls.cons.post));

            if(cls.methods != null){
                sb.append("Method:\n");
                for (var m : cls.methods.keySet()) {
                    var method = cls.methods.get(m);
                    sb.append(m).append(": ")
                      .append(getType(method.abstLocs, method.bindLocs, method.argTypes, method.returnType, method.pre, method.post));
                }
            }

            sb.append("\n");
            clsTableSb.append(sb.toString());
        }


        ctTextArea.setText(clsTableSb.toString());
    }

    //コンストラクタorメソッドの型
    static String getType(ArrayList<String> abstLocs, ArrayList<String> bindLocs,  LinkedHashMap<String, IType> argType,
                          IType returnType, HashMap<String, ObjectType> pre, HashMap<String, ObjectType> post){
        var sb = new StringBuilder();
        sb.append("∀").append(String.join(",", abstLocs))
          .append(".").append(Constraint.toString(pre)).append(";");

        var str = new ArrayList<String>();
        for (var field : argType.keySet()) {
            if(field.equals("this")) continue;

            str.add(field+":"+argType.get(field).getTypeName());
        }
        sb.append(String.join(", ", str));

        sb.append(" => ∃").append(String.join(",", bindLocs)).append(".")
          .append(returnType.getTypeName()).append(";")
          .append(Constraint.toString(post)).append("\n");

        return sb.toString();
    }

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                matcher.group("KEYWORD") != null ? "keyword" :
                matcher.group("PAREN") != null ? "paren" :
                matcher.group("BRACE") != null ? "brace" :
                matcher.group("BRACKET") != null ? "bracket" :
                matcher.group("SEMICOLON") != null ? "semicolon" :
                matcher.group("STRING") != null ? "string" :
                matcher.group("COMMENT") != null ? "comment" :
                null; /* never happens */
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    static void Shutdown() {
        System.out.println("Stop");
        executor.shutdown();
    }
}
