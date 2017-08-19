package main;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML TreeView<String> fileTreeView;

    public void initialize(URL location, ResourceBundle resources) {

        TreeItem<String> rootItem = new TreeItem<String> ("Root: /");
       // rootItem.addEventHandler(TreeItem.branchExpandedEvent(), (e) -> addfileToDir(e.getSource()));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);


       ftpTest();
    } // initialize()


    private void getFiles(TreeItem dir){


        System.out.println(dir.getValue());


    } // getFiles()

    private void addfileToDir(TreeItem dir){

        // simulate adding files
        for (int i = 1; i < 4; i++) {
            TreeItem<String> item = new TreeItem<String> ("File" + i);

            // add an event handler for branch expanded
            item.addEventHandler(TreeItem.branchExpandedEvent(), (e) -> addfileToDir(e.getSource()));
            item.setExpanded(false);

            dir.getChildren().add(item);
        }

    } // addFileToDir()

    public void ftpTest() {

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
                    fileTreeView.getRoot().getChildren().add(new TreeItem<>(name));
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
            System.out.println("Error: " + e.getMessage());
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("Error Disconnecting");
            }
        }
    } // ftpTest()

} // class
