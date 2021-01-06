import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.Arrays;

public class MainApp extends Application {

    public static void main(String[] args) {
        try {
          typeCheck(args[0]);
        } catch (Exception e){
            e.printStackTrace();
        }
        Data.sourceFile = args[0];
        launch(args);
    }

    public static void typeCheck(String file) throws Exception {
        JavaLexer lexer = new JavaLexer(CharStreams.fromFileName(file));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        ParseTree tree = parser.compilationUnit();

        //GUI表示
        //TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
        //viewer.open();

        //クラステーブルを生成するvisitor
        MakeClassTable makeClassTable = new MakeClassTable();
        makeClassTable.visit(tree);

        //クラステーブルを保管
        Data.clsTable = makeClassTable.clsTable;

        //型付けを行うvisiotr
        TypeCheck typeCheck = new TypeCheck();
        typeCheck.visit(tree);
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
        primaryStage.show();

    }
}
