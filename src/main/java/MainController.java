import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class MainController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane sourceArea;

    @FXML
    private TextArea resultTextArea;

    @FXML
    private TextArea ctTextArea;


    @FXML
    void initialize() {
        assert resultTextArea != null : "fx:id=\"resultTextArea\" was not injected: check your FXML file 'MainView.fxml'.";

        //ソースコードのUI
        CodeArea codeArea = new CodeArea();

        var code = new StringBuilder();
        try {
            Files.readAllLines(Paths.get(Data.sourceFile), Charset.defaultCharset()).forEach(s -> code.append(s).append("\n"));
            codeArea.replaceText(code.toString());
        } catch (IOException e){
            var errormsg = "Can't Open File in Tab pane";
        }

        AnchorPane.setBottomAnchor(codeArea, 0.0);
        AnchorPane.setTopAnchor(codeArea, 0.0);
        AnchorPane.setLeftAnchor(codeArea, 0.0);
        AnchorPane.setRightAnchor(codeArea, 0.0);
        sourceArea.getChildren().add(codeArea);

        //行番号表示
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.setEditable(false);

        //main関数の型付け
        var sb = new StringBuilder();
        for (var debugInfo : Data.mainDebugInfo) {
            sb.append("Line ").append(debugInfo.line).append(":").append("\n");
            sb.append("TypingEnvironment: ").append(debugInfo.typeEnv).append("\n");
            sb.append("Constraint: ").append(debugInfo.constraint).append("\n\n");
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
}
