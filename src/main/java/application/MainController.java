package application;

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

    private Preferences prefs = new Preferences();
    private FTPClient client = new FTPClient();
    private InetAddress address;
    private DirectoryChooser directoryChooser = new DirectoryChooser();
    private File outputDir;
    private OutputStream outStream;
    private SimpleDateFormat ft = new SimpleDateFormat ("HH:mm:ss MMM d");
    private File settingsDir;
    private HashSet<String> syncedFileLedger = new HashSet<>();
    private String syncedFileLedgerName = "syncedFiles.dat";
    private long defaultDaysLimit = 6;
    private long daysLimit;
    private boolean outputDirSelected;
    private boolean isDownloadingFiles;

    private Image dirIcon = new Image(getClass().getResourceAsStream("/icons/directory_icon.png"));


    public void initialize(URL location, ResourceBundle resources) {

        // make the settings directory
        settingsDir = new File("settings");

        if(settingsDir.exists()){

            // try load settings

            // load the set of synced files
            loadSyncedFileLedger();

        } else {

            // make settings directory
            settingsDir.mkdir();

            System.out.println("Settings folder not found, recreating.");
        } // if

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
                saveSyncedFileLedger();

                return true;
            } // call()
        };

        // start the thread
        new Thread(downloadTask).start();

    } // syncFilesBT_OnAction()

    // loads the synced file ledger from a file
    private void loadSyncedFileLedger(){

        try {

            System.out.println("Loading synced file ledger.");

            FileInputStream fin = new FileInputStream(settingsDir.getName() + File.separator + syncedFileLedgerName);
            ObjectInputStream ois = new ObjectInputStream(fin);
            syncedFileLedger = (HashSet<String>) ois.readObject();

        }catch (FileNotFoundException fnf){

            System.out.println("Error, syncedFileLedger file not found.");
        }
        catch(Exception e){

            e.printStackTrace();
        } // try

    } // loadSyncedFileLedger()

    // saves the map that contains all the files that are synced
    private void saveSyncedFileLedger(){

        try {

            System.out.println("Saving synced file ledger.");

            FileOutputStream fout = new FileOutputStream(settingsDir.getName() + File.separator + syncedFileLedgerName);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(syncedFileLedger);

        }catch (Exception e){

            System.out.println("Error saving synced file ledger");
            e.printStackTrace();
        } // try

    } // saveSyncedFileLedger()


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
                treeNode.getChildren().add(new TreeItem<>(file.getName() + " | " +
                        ft.format(file.getTimestamp().getTime()) + " | " +
                        checkIfSynced(client.printWorkingDirectory() + File.separator + file.getName())));

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
                newDir.setExpanded(true);

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


    // returns the string "Synced" if the file is synced
    private String checkIfSynced(String fileName){

        if(syncedFileLedger.contains(fileName))
            return "Synced";
        else
            return "Not Synced";

    } // checkIfSynced()


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
                String fileOnServer = client.printWorkingDirectory() + File.separator + file.getName();

                System.out.println("File on server: " + fileOnServer);
                // if file is not older then limit and not already synced
                if (daysOld < daysLimit && syncedFileLedger.contains(fileOnServer) == false) {

                    System.out.println("Downloading: " + file.getName());
                    Platform.runLater(() -> logTA.appendText("\nDownloading: " + file.getName()));

                    // create the directory that the file will be places in on users pc
                    File filePath = new File(outputDir.getAbsoluteFile() + client.printWorkingDirectory());
                    filePath.mkdirs();

                    System.out.println("Output file: " + filePath.getAbsolutePath());

                    // create outputStream for file
                    outStream = new FileOutputStream(filePath + File.separator + file.getName());

                    // retrieve the files
                    client.retrieveFile(file.getName(), outStream);

                    // close the stream
                    outStream.close();

                    // flag file as synced
                    syncedFileLedger.add(fileOnServer);

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
