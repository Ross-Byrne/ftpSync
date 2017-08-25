package application;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

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

    private DataManager dataManager = new DataManager();
    private FTPClient client = new FTPClient();
    private InetAddress address;
    private DirectoryChooser directoryChooser = new DirectoryChooser();
    private File outputDir;
    private OutputStream outStream;
    private SimpleDateFormat ft = new SimpleDateFormat ("HH:mm:ss MMM d");
    private long daysLimit;
    private boolean outputDirSelected;
    private boolean isDownloadingFiles;
    private Image dirIcon = new Image(getClass().getResourceAsStream("/icons/directory_icon.png"));


    public void initialize(URL location, ResourceBundle resources) {

        // set up directory chooser
        directoryChooser.setTitle("Select Download Location");

        // set default age limit
        fileAgeLimitTF.setText(String.valueOf(dataManager.getFileAgeLimit()));
        daysLimit = dataManager.getFileAgeLimit();

        // set up file tree
        TreeItem<String> rootItem = new TreeItem<> ("Root: /", new ImageView(dirIcon));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);

        // fill in last used server address and username
        addressTF.setText(dataManager.getServerAddress());
        usernameTF.setText(dataManager.getUsername());

        // save the value after it is set
        fileAgeLimitTF.focusedProperty().addListener((observable, oldPropertyValue, newPropertyValue) -> {

            if(!newPropertyValue) { // if lost focus

                // if made blank, reset the value
                if (fileAgeLimitTF.getText() == "") {
                    fileAgeLimitTF.setText(String.valueOf(dataManager.getFileAgeLimit()));
                    return;
                }

                try {

                    // save the file age limit
                    dataManager.setFileAgeLimit(Integer.parseInt(fileAgeLimitTF.getText()));

                    // save preferences
                    dataManager.savePreferences();

                } catch (Exception e) {

                    // fails if value is not a number

                    // reset value
                    fileAgeLimitTF.setText(String.valueOf(dataManager.getFileAgeLimit()));

                    // show alert
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Value entered must be a number!");
                    alert.showAndWait();

                } // try
            } // if
        });

    } // initialize()

    // onClick method for login button
    @FXML void loginButtonClick_OnAction(){

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

            // show alert
            Alert alert = new Alert(Alert.AlertType.ERROR, "Enter ftp server address.");
            alert.showAndWait();
            return;
        }

        if(usernameTF.getCharacters().length() < 1){

            logTA.appendText("\nError, enter Username.");
            // show alert
            Alert alert = new Alert(Alert.AlertType.ERROR, "Enter username.");
            alert.showAndWait();
            return;
        }

        if(passwordPF.getCharacters().length() < 1){

            logTA.appendText("\nError, enter Password.");
            // show alert
            Alert alert = new Alert(Alert.AlertType.ERROR, "Enter password.");
            alert.showAndWait();
            return;
        }

        // make sure output directory is selected
        if(outputDirSelected == false){

            logTA.appendText("\nError, please select an output directory for downloaded files.");
            // show alert
            Alert alert = new Alert(Alert.AlertType.ERROR, "Select a directory for downloaded files.");
            alert.showAndWait();
            return;
        }

        // save the server address and username entered to the data manager
        dataManager.setServerAddress(addressTF.getText());
        dataManager.setUsername(usernameTF.getText());

        // try login, in a separate thread
        new Thread(new Task<Void>(){

            protected Void call() throws Exception {

                // connect to the server
                connectToServer(addressTF.getText(), usernameTF.getText(), passwordPF.getText());
                return null;
            }
        }).start();

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

            logTA.appendText("\nError, not logged in. Cannot sync files.");
            // show alert
            Alert alert = new Alert(Alert.AlertType.ERROR, "Not logged in, cannot sync files.");
            alert.showAndWait();
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

                    Platform.runLater(() ->{

                        // update displayed files
                        try {

                            displayFileTree(client);

                        } catch (Exception e) {

                            logTA.appendText("\nError updating file tree!");
                            e.printStackTrace();
                        } // try

                        // disconnect from the server
                        disconnectServer();

                    });

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

                // save the set of synced files
                dataManager.saveSyncedFileLedger();

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

                    Platform.runLater(() -> logTA.appendText("\nError: " + client.getReplyString()));

                    client.disconnect();

                    return;
                } // if

                // logged in successfully, save preferences
                dataManager.savePreferences();

                // enter passive mode
                client.enterLocalPassiveMode();

                // logged in ok
                Platform.runLater(() -> logTA.appendText("\n" + client.getReplyString()));

                // display files
                Platform.runLater(() ->{

                    try {

                        displayFileTree(client);

                    } catch (Exception e) {

                        logTA.appendText("\nError updating file tree!");
                        e.printStackTrace();
                    } // try
                });

            } // if

        }catch (Exception e){

            System.out.println("Error, cannot connect to the server.");
            Platform.runLater(() -> logTA.appendText("\nError, cannot connect to the server."));

            // disconnect the user from server
            disconnectServer();

        } // try

    } // connectToServer()


    // disconnect the user form server
    private void disconnectServer(){

        try {

            // disconnect client
            client.disconnect();

            Platform.runLater(() -> logTA.appendText("\nDisconnecting."));
            System.out.println("Disconnecting");

        } catch (Exception e) {

            //e.printStackTrace();
            Platform.runLater(() -> logTA.appendText("\nError Disconnecting."));

        } // try
    } // disconnectServer()


    // use recursive method to build and display file tree
    private void displayFileTree(FTPClient client) throws Exception{

        // set up file tree
        TreeItem<String> rootItem = new TreeItem<> ("Root: /", new ImageView(dirIcon));
        rootItem.setExpanded(true);

        // set the tree root
        fileTreeView.setRoot(rootItem);

        // start building the file tree
        buildFileTree(fileTreeView.getRoot(), client);

    } // displayFileTree()

    // builds the tree view of the files
    private void buildFileTree(TreeItem treeNode, FTPClient client) throws Exception {

        // display the files
        FTPFile[] files = client.listFiles("", FTPFile::isFile);

        for (FTPFile file : files) {

            if(!file.getName().startsWith(".")) {

                System.out.println("File: " + file.getName());

                // add file to file tree
                treeNode.getChildren().add(new TreeItem<>(getFileDisplayText(client, file)));

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
                newDir.setExpanded(false);

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


    // creates the display text that is shown for a file
    private String getFileDisplayText(FTPClient client, FTPFile file) throws Exception {

        String name = file.getName() + " | " +
                ft.format(file.getTimestamp().getTime()) + " | " +
                fileSyncStatus(file, client.printWorkingDirectory() + File.separator + file.getName());

        return name;

    } // getFileDisplayText()


    // returns the string with the files sync status eg. not synced etc.
    private String fileSyncStatus(FTPFile file, String remoteFilePath){

        if(dataManager.isFileSynced(remoteFilePath))
            return "Synced";
        else if(getFilesAgeInDays(file) >= dataManager.getFileAgeLimit())
            return "Ignored";
        else
            return "Not Synced";

    } // checkIfSynced()


    private long getFilesAgeInDays(FTPFile file){

        // finds the difference, in days, between the files timestamp and the current time
        return Duration.between(file.getTimestamp().toInstant(), Calendar.getInstance().toInstant()).toDays();
    } // getFilesAgeInDays()


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
                daysOld = getFilesAgeInDays(file);

                System.out.println("File is " + daysOld + " days old");
                String remoteFilePath = client.printWorkingDirectory() + File.separator + file.getName();

                // if file is not older then limit and not already synced
                if (daysOld < daysLimit && dataManager.isFileSynced(remoteFilePath) == false) {

                    long size;

                    System.out.println("Downloading: " + remoteFilePath);
                    Platform.runLater(() -> logTA.appendText("\nDownloading: " + remoteFilePath));

                    // save the log
                    StringBuilder sb = new StringBuilder();
                    sb.append(logTA.getText());

                    // create the directory that the file will be places in on users pc
                    File localFilePath = new File(outputDir.getAbsoluteFile() + client.printWorkingDirectory());
                    localFilePath.mkdirs();

                    // get the file size
                    size = file.getSize();

                    // create outputStream for file
                    outStream = new FileOutputStream(localFilePath + File.separator + file.getName());

                    // wrap in counting out stream to track progress
                    CountingOutputStream cos = new CountingOutputStream(outStream){
                        protected void beforeWrite(int n){
                            super.beforeWrite(n);

                            displayDownloadProgress(sb.toString(), getCount(), size);

                            //System.err.println("Downloaded "+getCount() + "/" + size);
                        }
                    };

                    // retrieve the files
                    client.retrieveFile(file.getName(), cos);

                    // close the stream
                    cos.close();

                    // flag file as synced
                    dataManager.flagFileAsSynced(remoteFilePath);

                    // reset log
                    logTA.setText(sb.toString());
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


    private void displayDownloadProgress(String log, long current, long total){

        long percent = (total/current) * 100;

        Platform.runLater(() -> logTA.setText(log + "\n" + percent + "%"));

    } // displayDownloadProgress()

} // class
