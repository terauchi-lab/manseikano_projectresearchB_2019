import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.*;

import java.io.IOException;
import java.util.Arrays;

public class MainApp extends Application {
    static String lexErr;

    public static void main(String[] args) {
        try{
            JavaLexer lexer = new JavaLexer(CharStreams.fromFileName(args[0]));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);
            parser.setErrorHandler(new MyErrorStrategy());
            ParseTree tree = parser.compilationUnit();

            //構文解析結果のGUI表示
            //TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
            //viewer.open();

            Data.sourceFile = args[0];

            //クラステーブルを生成するvisitor
            MakeClassTable makeClassTable = new MakeClassTable();
            makeClassTable.visit(tree);

            //クラステーブルを保管
            Data.clsTable = makeClassTable.clsTable;

            //型付けを行うvisiotr
            TypeCheck typeCheck = new TypeCheck();
            typeCheck.visit(tree);

        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        } catch (RecognitionException e) {
            System.out.println("");
            System.out.println("Syntax Error");
            System.out.println(e);
            System.out.println(MainApp.lexErr);
            System.exit(1);
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        //UI起動
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainView.fxml"));
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add("styles/editor_style.css");
        scene.getStylesheets().add("styles/tab_pane_style.css");
        scene.getStylesheets().add("styles/result_area_style.css");

        primaryStage.setTitle("Main");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.setOnHidden(e -> MainController.Shutdown());
        primaryStage.show();
    }
}

class MyErrorStrategy extends DefaultErrorStrategy {

    @Override
    public void recover(Parser recognizer, RecognitionException e) {
        throw e;
    }

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
        MainApp.lexErr = "mismatched input " + getTokenErrorDisplay(e.getOffendingToken());
        super.reportNoViableAlternative(recognizer, e);
    }
}