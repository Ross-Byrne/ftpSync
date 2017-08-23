package application;

import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // UI elements

    @FXML private TreeView<String> fileTreeView;
    @FXML private TextField addressTF;
    @FXML private TextField usernameTF;
    @FXML private PasswordField passwordPF;
    @FXML private Button loginBT;
    @FXML private TextArea logTA;
    @FXML private Button outputDirBT;
    @FXML private Label outputDirLB;
    @FXML private TextField fileAgeLimitTF;


    // variables

    private FTPClient client = new FTPClient();
    private InetAddress address;
    private DirectoryChooser directoryChooser = new DirectoryChooser();
    private File outputDir;
    private OutputStream outStream;
    private SimpleDateFormat ft = new SimpleDateFormat ("HH:mm:ss MMM d");
    private long defaultDaysLimit = 6;
    private long daysLimit;
    private boolean outputDirSelected;
    private boolean isDownloadingFiles;

    private Image dirIcon = new Image(getClass().getResourceAsStream("/icons/directory_icon.png"));


    public void initialize(URL location, ResourceBundle resources) {

        // make the output directory
        //outputDir.mkdir();

        // set up directory chooser
        directoryChooser.setTitle("Select Download Location");

        // set default age limit
        fileAgeLimitTF.setText(String.valueOf(defaultDaysLimit));
        daysLimit = defaultDaysLimit;

        // set up file tree
        TreeItem<String> rootItem = new TreeItem<> ("Root: /", new ImageView(dirIcon));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);

        // set the login details to make testing faster
        addressTF.setText("ftp.vaultfortress.net");
        usernameTF.setText("ross@vaultfortress.net");

    } // initialize()

    // onClick method for login button
    @FXML void loginButtonClick_OnAction(){

        System.out.println("Login Click");

        // clear message label
        logTA.setText("Logging in...");

        // clear the file tree

        TreeItem<String> rootItem = new TreeItem<> ("Root: /", new ImageView(dirIcon));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);

        // check that server address is entered
        // check that username and password are entered

        if(addressTF.getCharacters().length() < 3){

            logTA.appendText("\nError, enter ftp server address.");
            return;
        }

        if(usernameTF.getCharacters().length() < 1){

            logTA.appendText("\nError, enter Username.");
            return;
        }

        if(passwordPF.getCharacters().length() < 1){

            logTA.appendText("\nError, enter Password.");
            return;
        }

        // make sure output directory is selected
        if(outputDirSelected == false){

            logTA.appendText("\nError, please select an output directory for downloaded files.");
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


    // runs when the Sync Files button is pressed
    @FXML void syncFilesBT_OnAction(){

        Task downloadTask;

        // check if logged in
        if(client.isConnected() == false) {

            logTA.appendText("Error, not logged in. Cannot sync files.");
            return;
        } // if

        // flag as downloading
        isDownloadingFiles = true;

        // update the days limit
        daysLimit = Long.parseLong(fileAgeLimitTF.getText());

        // download the files, in a separate thread
        downloadTask = new Task<Boolean>(){

            protected Boolean call() throws Exception {

                Platform.runLater(() -> logTA.appendText("\nStarting to download files . . ."));

                try {

                    // sync the files
                    syncFiles(client);

                    // disconnect from the server
                    disconnectServer();

                } catch (Exception ex){

                    Platform.runLater(() -> logTA.appendText("\nError Downloading files!"));

                    // disconnect from the server
                    disconnectServer();

                    isDownloadingFiles = false;

                    ex.printStackTrace();

                    return false;
                } // try

                Platform.runLater(() -> logTA.appendText("\nFinished downloading files."));

                isDownloadingFiles = false;

                return true;
            } // call()
        };

        // start the thread
        new Thread(downloadTask).start();


    } // syncFilesBT_OnAction()


    // connects to the ftp server and discovers the files
    private void connectToServer(String serverAddress, String username, String password){

        // try connect
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

                    logTA.appendText("\nError: " + client.getReplyString());

                    client.disconnect();

                    return;
                } // if

                // enter passive mode
                client.enterLocalPassiveMode();

                // logged in ok
                logTA.appendText("\n" + client.getReplyString());

                // display files
                buildFileTree(fileTreeView.getRoot(), client);

            } // if

        }catch (Exception e){

            System.out.println("Error: " + e.getMessage());
            logTA.appendText("\nError: " + e.getMessage());

            // disconnect the user from server
            disconnectServer();

        } // try

    } // connectToServer()


    // disconnect the user form server
    private void disconnectServer(){

        try {

            // disconnect client
            client.disconnect();

            logTA.appendText("\nDisconnecting.");
            System.out.println("Disconnecting");

        } catch (Exception e) {

            //e.printStackTrace();
            logTA.appendText("\nError Disconnecting.");

        } // try
    } // disconnectServer()


    // builds the tree view of the files
    private void buildFileTree(TreeItem treeNode, FTPClient client) throws Exception {

        // display the files
        FTPFile[] files = client.listFiles("", FTPFile::isFile);

        for (FTPFile file : files) {

            if(!file.getName().startsWith(".")) {

                System.out.println("File: " + file.getName());
                // add file to file tree
                treeNode.getChildren().add(new TreeItem<>(file.getName() + " | " + ft.format(file.getTimestamp().getTime())));

            } // if

        } // for

        // get the directories
        FTPFile[] directories = client.listDirectories();

        for (FTPFile dir : directories) {

            if(!dir.getName().startsWith(".")) {

                // change working directory to detected directory
                client.changeWorkingDirectory(dir.getName());

                // create treeItem to represent new Directory
                TreeItem newDir = new TreeItem<>(dir.getName(), new ImageView(dirIcon));

                // add directory to file tree
                treeNode.getChildren().add(newDir);

                logTA.appendText("\nDiscovering Files in: " + client.printWorkingDirectory());
                System.out.println("Discovering Files in: " + client.printWorkingDirectory());

                // recursively call method to add files and directories to new directory
                buildFileTree(newDir, client);

                // go back to parent directory, once finished in this directory
                client.changeToParentDirectory();

            } // if

        } // for

    } // buildFileTree()


    // sync files, by download files that need to be downloaded
    private void syncFiles(FTPClient client) throws Exception {

        long daysOld;
        String pwd;

        // display the files
        FTPFile[] files = client.listFiles("", FTPFile::isFile);

        if(files.length > 0){

            pwd = client.printWorkingDirectory();
            System.out.println("Downloading Files in: " + client.printWorkingDirectory());
            Platform.runLater(() -> logTA.appendText("\nDownloading Files in: " + pwd));

        } // if

        for (FTPFile file : files) {

            if(!file.getName().startsWith(".")) {

                // get the number of days old this file is
                daysOld = Duration.between(file.getTimestamp().toInstant(), Calendar.getInstance().toInstant()).toDays();

                System.out.println("File is " + daysOld + " days old");

                // if file is not older then limit
                if (daysOld < daysLimit) {

                    System.out.println("Downloading: " + file.getName());
                    Platform.runLater(() -> logTA.appendText("\nDownloading: " + file.getName()));

                    // create outputStream for file
                    outStream = new FileOutputStream(outputDir.getName() + File.separator + file.getName());

                    // retrieve the files
                    client.retrieveFile(file.getName(), outStream);

                    // close the stream
                    outStream.close();

                } // if
            } // if
        } // for

        // get the directories
        FTPFile[] directories = client.listDirectories();

        for (FTPFile dir : directories) {

            if (!dir.getName().startsWith(".")) {

                // change working directory to detected directory
                client.changeWorkingDirectory(dir.getName());

                // recursively call method to add files and directories to new directory
                syncFiles(client);

                // go back to parent directory, once finished in this directory
                client.changeToParentDirectory();

            } // if
        } // for
    } // syncFiles()

} // class
