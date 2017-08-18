package main;

import com.sun.org.apache.xpath.internal.SourceTree;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.net.InetAddress;

public class Main extends Application {

    public void start(Stage primaryStage) throws Exception{

        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        primaryStage.setTitle("ftp Sync");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(600);
        primaryStage.show();

    } // start()


    public static void main(String[] args) {



        ftpTest();

        launch(args);

    } // main()

    public static void ftpTest() {

        String server = "localhost";

        FTPClient client = new FTPClient();

        try {
            InetAddress addr = InetAddress.getByName(server);
            client.connect(addr);
            client.login("bob", "qwerty");

            if (client.isConnected()) {

                // enter passive mode
                client.enterLocalPassiveMode();
                //client.enterRemoteActiveMode(addr,3000);

                System.out.println("Connected");

                System.out.print(client.getReplyString());
                FTPFile[] files = client.listFiles();

                System.out.println("No of Files: " + files.length);

                // Obtain a list of filenames in the current working
                // directory. When no file found an empty array will
                // be returned.
                String[] names = client.listNames();

                //System.out.println("No of files: " + names.length);
                for (String name : names) {
                    System.out.println("Name = " + name);
                }

                FTPFile[] ftpFiles = client.listFiles();
                for (FTPFile ftpFile : ftpFiles) {
                    // Check if FTPFile is a regular file
                    if (ftpFile.getType() == FTPFile.FILE_TYPE) {
                        System.out.printf("FTPFile: %s; %s; %s%n",
                                ftpFile.getName(),
                                FileUtils.byteCountToDisplaySize(ftpFile.getSize()),
                                ftpFile.getTimestamp().getTime().toString());
                    }
                }
            }
            client.logout();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error Disconnecting");
            }
        }
    }

} // class
