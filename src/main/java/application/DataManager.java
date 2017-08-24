package application;

import java.io.*;
import java.util.*;

/**
 * Created by Ross Byrne on 24/08/17.
 *
 * Manages the saving of data and preferences.
 */
public class DataManager {

    private final static String preference_FileName = "preferences.ini";
    private final static String syncedFileLedger_FileName = "syncedFiles.dat";
    private String serverAddress;
    private String username;
    private int fileAgeLimit;
    private File settingsDir = new File("settings");

    private HashSet<String> syncedFileLedger = new HashSet<>();


    // loads the programs preferences
    public boolean loadPreferences(){

        return true;
    } // loadPreferences()


    // saves the programs preferences
    public boolean savePreferences(){


        return true;
    } // savePreferences();

    // loads the synced file ledger from a file
    public void loadSyncedFileLedger(){

        try {

            System.out.println("Loading synced file ledger.");

            FileInputStream fin = new FileInputStream(settingsDir.getName() + File.separator + syncedFileLedger_FileName);
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
    public void saveSyncedFileLedger(){

        try {

            System.out.println("Saving synced file ledger.");

            FileOutputStream fout = new FileOutputStream(settingsDir.getName() + File.separator + syncedFileLedger_FileName);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(syncedFileLedger);

        }catch (Exception e){

            System.out.println("Error saving synced file ledger");
            e.printStackTrace();
        } // try

    } // saveSyncedFileLedger()


    // checks if a file has been synced
    public boolean isFileSynced(String remoteFilePath){

        return syncedFileLedger.contains(remoteFilePath);
    } // isFileSynced()

    // flags a file as synced by adding the files remote path to hashset
    public void flagFileAsSynced(String remoteFilePath){

        syncedFileLedger.add(remoteFilePath);
    } // flagFileAsSynced()


    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getFileAgeLimit() {
        return fileAgeLimit;
    }

    public void setFileAgeLimit(int fileAgeLimit) {
        this.fileAgeLimit = fileAgeLimit;
    }

} // class
