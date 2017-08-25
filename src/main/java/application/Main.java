package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("ftp Sync");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        // shutdown any running threads when application exit button is pressed
        MainController controller = loader.getController();
        primaryStage.setOnCloseRequest(e -> controller.shutdown());

    } // start()


    public static void main(String[] args) {

        launch(args);

    } // main()

} // class
