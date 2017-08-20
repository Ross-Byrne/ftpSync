package application;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
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
import java.time.chrono.ChronoPeriod;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // UI elements

    @FXML TreeView<String> fileTreeView;
    @FXML TextField addressTF;
    @FXML TextField usernameTF;
    @FXML PasswordField passwordPF;
    @FXML Button loginBT;
    @FXML Label messageLB;
    @FXML Button outputDirBT;
    @FXML Label outputDirLB;

    // variables

    FTPClient client = new FTPClient();
    InetAddress address;
    DirectoryChooser directoryChooser = new DirectoryChooser();
    File outputDir = new File("downloadedFiles");
    OutputStream outStream;


    private Image dirIcon = new Image(getClass().getResourceAsStream("/icons/directory_icon.png"));


    public void initialize(URL location, ResourceBundle resources) {

        // make the output directory
        outputDir.mkdir();

        directoryChooser.setTitle("Select Download Location");


        TreeItem<String> rootItem = new TreeItem<> ("Root: /", new ImageView(dirIcon));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);

        // set the login details to make testing faster
        addressTF.setText("ftp.vaultfortress.net");
        usernameTF.setText("ross@vaultfortress.net");
        passwordPF.setText("TopGun666");

    } // initialize()

    // onClick method for login button
    @FXML void loginButtonClick_OnAction(){

        //System.out.println("Login Click");

        // clear message label
        messageLB.setText("");

        // clear the file tree

        TreeItem<String> rootItem = new TreeItem<String> ("Root: /", new ImageView(dirIcon));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);

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

        // try login
        connectToServer(this.addressTF.getText(), this.usernameTF.getText(), this.passwordPF.getText());

    } // loginButtonClick()


    @FXML void outputDirBT_OnAction(){

        // open output directory chooser
        outputDir = directoryChooser.showDialog(null);

        // show selected folder
        outputDirLB.setText(outputDir.getAbsolutePath());

    } // outputDirBT_OnAction()


    private void connectToServer(String serverAddress, String username, String password){

        // try connect
        if(this.client.isConnected() == false){

            try {

                // create a server address
                this.address = InetAddress.getByName(serverAddress);

                // connect to the address
                client.connect(address, 21);

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

                    // display files
                    buildFileTree(fileTreeView.getRoot(), client, "");

                    // download the files

//                    System.out.println("Downloading files");
//
//                    for (FTPFile file : files) {
//
//                        System.out.println("Downloading: " + file.getName());
//
//                        if(file.isDirectory()){
//                            System.out.println("Is Directory");
//                        }
//
//                        // create outputStream for file
//                        outStream = new FileOutputStream(outputDir.getName() + File.separator + file.getName());
//
//                        // retrieve the files
//                        client.retrieveFile("/" + file.getName(), outStream);
//
//                    } // for
//
//                    System.out.println("Finished downloading files");

                } // if

            }catch (Exception e){

                System.out.println("Error: " + e.getMessage());
                messageLB.setText("Error: " + e.getMessage());

            } finally {

                try {

                    // disconnect client
                    client.disconnect();

                    // close outStream
                    //outStream.close();

                    System.out.println("Disconnecting");

                } catch (IOException e) {

                    //e.printStackTrace();
                    System.out.println("Error Disconnecting");

                } catch (Exception e){

                    e.printStackTrace();
                }
            } // try

        } // if

    } // connectToServer()

    // builds the tree view of the files
    private void buildFileTree(TreeItem treeNode, FTPClient client, String path) throws Exception {

        // display the files
        FTPFile[] files = client.listFiles(path, FTPFile::isFile);

        for (FTPFile file : files) {

            // add file to file tree
            treeNode.getChildren().add(new TreeItem<>(file.getName() + " | " + file.getTimestamp().toInstant()));

        } // for

        // get the directories
        FTPFile[] directories = client.listDirectories(File.separator + path);

        for (FTPFile dir : directories) {

            if(!dir.getName().startsWith(".")) {
                // create treeItem to represent new Directory
                TreeItem newDir = new TreeItem<>(dir.getName(), new ImageView(dirIcon));

                // add directory to file tree
                treeNode.getChildren().add(newDir);

                // build path to new directory in server
                String newPath = path + File.separator + dir.getName();

                System.out.println("Discovering Files in: " + newPath);

                // recursively call method to add files and directories to new directory
                buildFileTree(newDir, client, newPath);
            }

        } // for

    } // buildFileTree()


    // sync files, by download files that need to be downloaded
    private void syncFiles(FTPClient client, String path) throws Exception {

        // display the files
        FTPFile[] files = client.listFiles(path, FTPFile::isFile);

        for (FTPFile file : files) {

            // add file to file tree
            //treeNode.getChildren().add(new TreeItem<>(file.getName() + " | " + file.getTimestamp().toInstant()));

        } // for

        // get the directories
        FTPFile[] directories = client.listDirectories(File.separator + path);

        for (FTPFile dir : directories) {

            if(!dir.getName().startsWith(".")) {
                // create treeItem to represent new Directory
                TreeItem newDir = new TreeItem<>(dir.getName(), new ImageView(dirIcon));

                // add directory to file tree
                //treeNode.getChildren().add(newDir);

                // build path to new directory in server
                String newPath = path + File.separator + dir.getName();

                System.out.println("Discovering Files in: " + newPath);

                // recursively call method to add files and directories to new directory
                syncFiles(client, newPath);
            }

        } // for

    } // syncFiles()


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
