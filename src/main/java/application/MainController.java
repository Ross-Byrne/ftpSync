package application;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // UI elements

    @FXML TreeView<String> fileTreeView;
    @FXML TextField addressTF;
    @FXML TextField usernameTF;
    @FXML PasswordField passwordPF;
    @FXML Button loginBT;
    @FXML Label messageLB;

    // variables

    FTPClient client = new FTPClient();
    InetAddress address;
    File outputDir = new File("downloadedFiles");
    OutputStream outStream;


    public void initialize(URL location, ResourceBundle resources) {

        // make the output directory
        outputDir.mkdir();

        TreeItem<String> rootItem = new TreeItem<String> ("Root: /");
       // rootItem.addEventHandler(TreeItem.branchExpandedEvent(), (e) -> addfileToDir(e.getSource()));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);

        // set the login details to make testing faster
        addressTF.setText("localhost");
        usernameTF.setText("bob");
        passwordPF.setText("qwerty");


       //ftpTest();
    } // initialize()

    // onClick method for login button
    @FXML void loginButtonClick_OnAction(){

        System.out.println("Login Click");

        // clear message label
        messageLB.setText("");

        // check that server address is entered
        // check that username and password are entered

        if(addressTF.getCharacters().length() < 3){

            this.messageLB.setText("Error, enter ftp server address.");
            return;
        }

        if(usernameTF.getCharacters().length() < 1){

            this.messageLB.setText("Error, enter Username.");
            return;
        }

        if(passwordPF.getCharacters().length() < 1){

            this.messageLB.setText("Error, enter Password.");
            return;
        }

        System.out.println("Logging in with Username: " + usernameTF.getText() + " and Password: " + passwordPF.getText());

        // try login
        connectToServer(this.addressTF.getText(), this.usernameTF.getText(), this.passwordPF.getText());

    } // loginButtonClick()

    private void getFiles(TreeItem dir){


        System.out.println(dir.getValue());


    } // getFiles()

    private void addfileToDir(TreeItem dir){

        // simulate adding files
        for (int i = 1; i < 4; i++) {
            TreeItem<String> item = new TreeItem<>("File" + i);

            // add an event handler for branch expanded
            item.addEventHandler(TreeItem.branchExpandedEvent(), (e) -> addfileToDir(e.getSource()));
            item.setExpanded(false);

            dir.getChildren().add(item);
        }

    } // addFileToDir()

    private void connectToServer(String serverAddress, String username, String password){

        // try connect
        if(this.client.isConnected() == false){

            try {

                // create a server address
                this.address = InetAddress.getByName(serverAddress);

                // connect to the address
                client.connect(address);

                // try and login
                client.login(username, password);

                if (client.isConnected()) {

                    System.out.print(client.getReplyString());

                    if (!FTPReply.isPositiveCompletion(client.getReplyCode())){

                        messageLB.setText("Error: " + client.getReplyString());

                        client.disconnect();

                        return;
                    } // if

                    // enter passive mode
                    client.enterLocalPassiveMode();

                    // logged in ok
                    messageLB.setText(client.getReplyString());

                    // get files

                    // Obtain a list of filenames in the current working
                    // directory. When no file found an empty array will
                    // be returned.
                    FTPFile[] files = client.listFiles();

                    //System.out.println("No of files: " + names.length);
                    for (FTPFile file : files) {
                        System.out.println("Name = " + file.getName());
                        fileTreeView.getRoot().getChildren().add(new TreeItem<>(file.getName()));

                        if(file.isDirectory()){
                            System.out.println("Is Directory");
                        }
                        outStream = new FileOutputStream(outputDir.getName() + File.separator + file.getName());
                        client.retrieveFile("/" + file.getName(), outStream);

                    }
                } // if

            }catch (Exception e){

                System.out.println("Error: " + e.getMessage());
                messageLB.setText("Error: " + e.getMessage());

            } finally {

                try {

                    // disconnect client
                    client.disconnect();

                    // close outStream
                    outStream.close();

                    System.out.println("Disconnecting");

                } catch (IOException e) {

                    //e.printStackTrace();
                    System.out.println("Error Disconnecting");
                } // try
            } // try

        } // if

    } // connectToServer()

    public void ftpTest() {

        String server = "localhost";



        try {

            address = InetAddress.getByName(server);

            client.connect(address);
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
