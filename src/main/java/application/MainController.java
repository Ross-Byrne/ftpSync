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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.chrono.ChronoPeriod;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // UI elements

    @FXML private TreeView<String> fileTreeView;
    @FXML private TextField addressTF;
    @FXML private TextField usernameTF;
    @FXML private PasswordField passwordPF;
    @FXML private Button loginBT;
    @FXML private Label messageLB;
    @FXML private Button outputDirBT;
    @FXML private Label outputDirLB;

    // variables

    FTPClient client = new FTPClient();
    InetAddress address;
    DirectoryChooser directoryChooser = new DirectoryChooser();
    File outputDir = new File("downloadedFiles");
    OutputStream outStream;
    SimpleDateFormat ft = new SimpleDateFormat ("HH:mm:ss MMM d");
    long daysLimit = 7;
    boolean outputDirSelected;

    private Image dirIcon = new Image(getClass().getResourceAsStream("/icons/directory_icon.png"));


    public void initialize(URL location, ResourceBundle resources) {

        // make the output directory
        outputDir.mkdir();

        // set up directory chooser
        directoryChooser.setTitle("Select Download Location");

        // set up file tree
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

        // make sure output directory is selected
        if(outputDirSelected == false){

            this.messageLB.setText("Please select an output directory for downloaded files.");
            return;
        }

        // try login
        connectToServer(this.addressTF.getText(), this.usernameTF.getText(), this.passwordPF.getText());

    } // loginButtonClick()


    @FXML void outputDirBT_OnAction(){

        // open output directory chooser
        outputDir = directoryChooser.showDialog(null);

        // show selected folder

        if(outputDir != null) {

            // flag as directory selected
            outputDirSelected = true;

            // display output location
            outputDirLB.setText(outputDir.getAbsolutePath());
        } // if

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

                    System.out.println("Starting to download files");

                    syncFiles(client, "");

                    System.out.println("Finished downloading files");

                } // if

            }catch (Exception e){

                System.out.println("Error: " + e.getMessage());
                messageLB.setText("Error: " + e.getMessage());

            } finally {

                try {

                    // disconnect client
                    client.disconnect();

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
            treeNode.getChildren().add(new TreeItem<>(file.getName() + " | " + ft.format(file.getTimestamp().getTime())));

        } // for

        // get the directories
        FTPFile[] directories = client.listDirectories(path);

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

        long daysOld = 0;

        // display the files
        FTPFile[] files = client.listFiles(path, FTPFile::isFile);

        for (FTPFile file : files) {

            // get the number of days old this file is
            daysOld = Duration.between(file.getTimestamp().toInstant(), Calendar.getInstance().toInstant()).toDays();
            System.out.println("File is " + daysOld + " days old");

            // if file is not older then limit
            if(daysOld < daysLimit) {

                System.out.println("Downloading: " + file.getName());

                // create outputStream for file
                outStream = new FileOutputStream(outputDir.getName() + File.separator + file.getName());

                // retrieve the files
                client.retrieveFile(path + file.getName(), outStream);

                // close the stream
                outStream.close();

            } // if

        } // for

        // get the directories
        FTPFile[] directories = client.listDirectories(path);

        for (FTPFile dir : directories) {

            if(!dir.getName().startsWith(".")) {

                // build path to new directory in server
                String newPath = path + File.separator + dir.getName();

                System.out.println("Downloading Files in: " + newPath);

                // recursively call method to add files and directories to new directory
                syncFiles(client, newPath);
            } // if

        } // for

    } // syncFiles()

} // class
