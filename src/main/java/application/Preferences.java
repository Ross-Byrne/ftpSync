package application;

/**
 * Created by Ross Byrne on 24/08/17.
 *
 * Loads and saves the programs preferences eg. file age limit.
 */
public class Preferences {

    private final static String fileName = "preferences.ini";
    private String serverAddress;
    private String username;
    private int fileAgeLimit;


    // loads the programs preferences
    public boolean loadPreferences(){

        return true;
    } // loadPreferences()


    // saves the programs preferences
    public boolean savePreferences(){


        return true;
    } // savePreferences();


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
