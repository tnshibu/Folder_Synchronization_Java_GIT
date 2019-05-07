import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Properties;
import java.util.ArrayList;

public class PropertiesLoader {
    // =========================================================================================
    public static Properties load(String fileName) {
        Properties props = new Properties();
        try {
            String propertyFileContents = readContentFromFile(fileName);
            props.load(new StringReader(propertyFileContents.replace("\\", "\\\\")));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return props;
    }

    // =========================================================================================
    private static String readContentFromFile(String fileName) throws Exception {
        String returnString = "";
        BufferedReader input = new BufferedReader(new FileReader(new File(fileName)));
        try {
            String line = null; // not declared within while loop
            while ((line = input.readLine()) != null) {
                returnString = returnString + line + "\r\n";
            }
        } finally {
            input.close();
        }
        return returnString;
    }

    // =========================================================================================
    public static ArrayList<String> readListFromFile(String fileName) throws Exception {
        ArrayList<String> returnArray = new ArrayList<String>();
        BufferedReader input = new BufferedReader(new FileReader(new File(fileName)));
        try {
            String line = null; // not declared within while loop
            while ((line = input.readLine()) != null) {
                returnArray.add(line);
            }
        } finally {
            input.close();
        }
        return returnArray;
    }
    // =========================================================================================
    public static void main(String args[]) {
        Properties props =PropertiesLoader.load("input.properties");
    }
    // =========================================================================================
}
