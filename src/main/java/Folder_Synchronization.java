import java.io.*;
import java.util.*;

import java.io.*;
import java.security.MessageDigest;

/*
    Data structure is as follows
    A Map with key=fileSize and value=ArrayList of filenames
*/
public class Folder_Synchronization {
  private static String SRC_BASE_FOLDER_FINAL = "";
  private static String DST_BASE_FOLDER_FINAL = "";
  private static String SRC_BASE_FOLDER       = "";
  private static String DST_BASE_FOLDER       = "";
  private static List<String> sourceFileList = new ArrayList<String>(10_000);
  private static List<String> destinationFileList = new ArrayList<String>(10_000);
  private static SortedMap<String,String> destinationCheckSumMap = Collections.synchronizedSortedMap(new TreeMap<String,String>());
  private static SortedMap<Long,List<String>> destinationFileMap = Collections.synchronizedSortedMap(new TreeMap<Long,List<String>>());
  private static String logFileName = "sync.log";
  private static String SYNC_COMMAND_BATCH_FILE = "folder_sync.bat";
  private static FileOutputStream batchFileOutputStream = null;
  /******************************************************************************************/
  public static void main(String[] args) throws Exception {
      
	setFolderNames();      
	batchFileOutputStream = new FileOutputStream (new File(SYNC_COMMAND_BATCH_FILE));
    batchFileOutputStream.write("REM - copying changed files....\r\n".getBytes());
	sourceFileList = getFileListFromFolder(SRC_BASE_FOLDER_FINAL);
    destinationFileList = getFileListFromFolder(DST_BASE_FOLDER_FINAL);
    System.out.println("REM - sourceFileList.size() = "+sourceFileList.size());
    System.out.println("REM - destinationFileList.size() = "+destinationFileList.size());

    System.out.println("REM - loading to Map - start");
    for(int i=0;i<sourceFileList.size();i++) {
      if(i%1000 == 0) {
        //System.out.println();
      }
      //System.out.println("----------------------------------------------------------------------------------------------------------------");
      String sourceFileFullPath = sourceFileList.get(i);
      //System.out.println(sourceFileFullPath);
      boolean check = checkAndDeleteIfDestinationFileExists(sourceFileFullPath);
      if(check == true) {
        //if this element was removed, then do not increment counter index
        i--;
      }
      //System.out.print(","+i);
    }
    System.out.println("REM - loading to Map - end");

    System.out.println("REM - destinationFileList.size() = "+destinationFileList.size());
    
    //cmpute the checksum of destination folder files
    for(int i=0;i<destinationFileList.size();i++) {
        computeDestinationCheckSum(destinationFileList.get(i));
    }

    
    for(int i=0;i<sourceFileList.size();i++) {
      String sourceFileFullPath = sourceFileList.get(i);
      //System.out.println("sourceFileFullPath       = " + sourceFileFullPath);
      boolean check = checkIfDestinationCheckSumExists(sourceFileFullPath);
      if(check == true) {
        //if this element was removed, then do not increment counter index
        i--;
      }
    }

    System.out.println("REM   -------- program end");
    
  }
  /******************************************************************************************/
  public static ArrayList<String> readFileList(String fileName) throws Exception {
    System.out.println("loading sob.txt - start");
    ArrayList<String> returnArray = new ArrayList<String>();
    BufferedReader input =  new BufferedReader(new FileReader(new File(fileName)));
    try {
        String line = null; //not declared within while loop
        while (( line = input.readLine()) != null){
          returnArray.add(line);
        }
      }
      finally {
        input.close();
      }
    System.out.println("loading sob.txt - end");
    return returnArray;
  }
  /******************************************************************************************/
  public static boolean compareFiles(String left, String right) throws Exception {
    File leftFile = new File(left);
    File rightFile = new File(right);
    long leftFileSize = leftFile.length();
    long rightFileSize = rightFile.length();
    
    if(leftFileSize != rightFileSize) {
        return false;
    }
    long sizeToCompare = leftFileSize;
    if(leftFileSize > 1000000) {
        sizeToCompare = 1000000;
    }
    BufferedInputStream left_bis = null;
    BufferedInputStream right_bis = null;
    byte[] leftBA = new byte[(int)sizeToCompare];
    byte[] rightBA = new byte[(int)sizeToCompare];
    try {
        left_bis = new BufferedInputStream(new FileInputStream(leftFile));
        right_bis = new BufferedInputStream(new FileInputStream(rightFile));
        left_bis.read(leftBA, 0, (int)sizeToCompare);
        right_bis.read(rightBA, 0, (int)sizeToCompare);
    }catch(FileNotFoundException fnfe) {
        return false;
    }
    return blockCompare(leftBA, rightBA);
  }
  /******************************************************************************************/
  public static boolean blockCompare(byte[] left, byte[] right) throws Exception {
    if(left.length != right.length) {
        return false;
    }
    for(int i=0;i<left.length;i++) {
        if(left[i] != right[i]) {
            return false;
        }
    }
    return true;
  }
  /******************************************************************************************/
  private static boolean checkAndDeleteIfDestinationFileExists(String sourceFileFullPath) {
    boolean exists = false;
    String relativePath = sourceFileFullPath.substring(SRC_BASE_FOLDER_FINAL.length());
    //System.out.println();
    //System.out.println("sourceFileFullPath       = " + sourceFileFullPath);
    //System.out.println("relativePath="+relativePath);
    String destinationFileFullPath  = DST_BASE_FOLDER_FINAL + relativePath;
    //wSystem.out.println("destinationFileFullPath  = " + destinationFileFullPath);
    File destFile = new File(destinationFileFullPath);
    if(destFile.exists()) {
        destinationFileList.remove(destinationFileFullPath);
        sourceFileList.remove(sourceFileFullPath);
        return true;
    }
    return false;
  }

  /******************************************************************************************/
    public static List<String> getFileListFromFolder(String sourcePath) {
        //System.out.println(sourcePath);
        File dir = new File(sourcePath);
        if(!dir.exists()) {
            return new ArrayList<String>();
        }
        List<String> fileTree = new ArrayList<String>(100);
        for (File entry : dir.listFiles()) {
            if (entry.isFile()) {
                //System.out.println(entry);
                fileTree.add(entry.getAbsolutePath());
            } else {
                try
                {
                    fileTree.addAll(getFileListFromFolder(entry.getAbsolutePath()));
                }
                catch (Exception e)
                {
                }
            }
        }
        return fileTree;
    }
  /******************************************************************************************/
    public static void computeDestinationCheckSum(String destFileFullPath) {
        try {
            //String key = computeCheckSum(destFileFullPath);
            //destinationCheckSumMap.put(key, destFileFullPath);

              String fileName = destFileFullPath;
              File file = new File(fileName);
              if(file.isDirectory()) {
                return;
              }
              long fileSize = file.length();
              //if( (fileSize < 10000) || (fileSize > 50000)) {
              if(fileSize < 10000) {
                return;
              }
              if(destinationFileMap.containsKey(new Long(fileSize))) {
                destinationFileMap.get(new Long(fileSize)).add(fileName);
              } else {
                List<String> l = new ArrayList<String>();
                l.add(fileName);
                destinationFileMap.put(new Long(fileSize), l);
              }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    /******************************************************************************************/
    public static String computeCheckSum(String fileFullPath) {
        try {
            byte[] ba = getBinarydataFromFile(fileFullPath);
            if(ba == null) {
                return "";
            }
            //printCharArray(fileFullPath+"=",Base64Coder.encode(ba));
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digestBA = md.digest(ba);
            char[] charArray = Base64Coder.encode(digestBA);
            String key = new String(charArray);
            return key;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "";
    }
  /******************************************************************************************/
  public static byte[] getBinarydataFromFile(String fileFullPath) throws Exception {
    File file = new File(fileFullPath);
    long fileSize = file.length();
    

    long sizeToCompare = fileSize;
    if(fileSize > 1000000) {
        sizeToCompare = 1000000;
    }
    //System.out.println(fileFullPath + ", filesize="+sizeToCompare);
    BufferedInputStream bis = null;
    byte[] ba = new byte[(int)sizeToCompare];
    try {
        bis = new BufferedInputStream(new FileInputStream(file));
        bis.read(ba, 0, (int)sizeToCompare);
    }catch(FileNotFoundException fnfe) {
        return null;
    }
    return ba;
  }
  /******************************************************************************************/
  public static boolean checkIfDestinationCheckSumExists(String sourceFileFullPath) {
        
        String sourceCheckSum = computeCheckSum(sourceFileFullPath);
        //check if this key is present in destination checksum map
        File file = new File(sourceFileFullPath);
        long sourceFileSize = file.length();
        List<String> destinationFilesWithSameFileSize = destinationFileMap.get(Long.valueOf(sourceFileSize));
        if(destinationFilesWithSameFileSize == null) {
            destinationFilesWithSameFileSize = new ArrayList<String>();
        }
        for(int i=0;i<destinationFilesWithSameFileSize.size();i++) {
            //all files in this list have same file size.
            //check now to see if one file matches source checksum
            String destinationFullPathBefore = destinationFilesWithSameFileSize.get(i);
            String destCheckSum = computeCheckSum(destinationFullPathBefore);
            String sourceRelativePath = sourceFileFullPath.substring(SRC_BASE_FOLDER_FINAL.length());
            String destFileFullPathAfter  = DST_BASE_FOLDER_FINAL + sourceRelativePath;
            if(sourceCheckSum.equals(destCheckSum)){
                // move the destination file according to source file structure
                System.out.println();
                //System.out.println("key=============================="+key);
                //System.out.println("REM   -------- sourceFileFullPath       ="+sourceFileFullPath);
                //System.out.println("REM   -------- destFileFullPathBefore   ="+destFileFullPathBefore);
                //System.out.println("REM   -------- destFileFullPathAfter    ="+destFileFullPathAfter);
                File srcFile = new File(sourceFileFullPath);
                File destFile = new File(destFileFullPathAfter);
                try {
                    destFile.getParentFile().getCanonicalPath();
                    String mkdirCommand = "md \"" + destFile.getParentFile().getCanonicalPath() + "\"" + "\r\n";
                    System.out.println(mkdirCommand );
                    batchFileOutputStream.write(mkdirCommand.getBytes());

                    String command = "move \"" + destinationFullPathBefore + "\" \"" + destFileFullPathAfter + "\"" + "\r\n";
                    System.out.println(command );
                    batchFileOutputStream.write(command.getBytes());
                    batchFileOutputStream.write("\r\n".getBytes());
                    //srcFile.renameTo(destFile);
                }catch(Exception e) {
                }
            }
        }
        return false;
  }

  /******************************************************************************************/
  private static void setFolderNames() throws Exception {
      
    boolean correctPathFound = false;
    String propertyFilePath = locatePropertiesFile();
    System.out.println("propertyFilePath="+propertyFilePath);
    List<String> configFileLineArray = PropertiesLoader.readListFromFile(propertyFilePath);
    String userCurrentDir = System.getProperty("user.dir")+"\\";
    System.out.println("REM - userCurrentDir = "+userCurrentDir);
    for(int j=0;j<configFileLineArray.size();j++) {
        if(correctPathFound) {
            break;
        }
        SRC_BASE_FOLDER               = "";
        DST_BASE_FOLDER               = "";
        //OTHER_PARAMS                  = "";

        String oneLine = configFileLineArray.get(j);
		oneLine = oneLine.trim();
		if(oneLine.equals("")) {
			continue;
		}
		if(oneLine.startsWith("#")) {
			continue; //it is a comment line, skip it
		}

        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("oneLine = "+oneLine );
        String[] stringArray = oneLine.split("=");
        if(stringArray.length >= 1) {
            SRC_BASE_FOLDER = stringArray[0];
        }
        if(stringArray.length >= 2) {
            DST_BASE_FOLDER = stringArray[1];
        }
        
        System.out.println("From Config File - SRC_BASE_FOLDER          = "+SRC_BASE_FOLDER );
        System.out.println("From Config File - DST_BASE_FOLDER          = "+DST_BASE_FOLDER );

        int i = userCurrentDir.indexOf(SRC_BASE_FOLDER);
		if(i == -1) {
			SRC_BASE_FOLDER               = "";
			DST_BASE_FOLDER               = "";
			//OTHER_PARAMS                  = "";
			continue;
		}
        if(i > -1) {
            System.out.println("Source folder found. Checking destination folder...");
        
            File temp1 = new File(DST_BASE_FOLDER);
            if (temp1.exists()) {
                System.out.println("Destination folder found. using this config !! ");
            } else {
                System.out.println("Destination folder does not exist. skipping this config !! ");
                continue;
			}

            if(!SRC_BASE_FOLDER.endsWith("\\")) {
                SRC_BASE_FOLDER = SRC_BASE_FOLDER + "\\";
            }
            if(!DST_BASE_FOLDER.endsWith("\\")) {
                DST_BASE_FOLDER = DST_BASE_FOLDER + "\\";
            }
            String subDir = SRC_BASE_FOLDER + userCurrentDir.substring(SRC_BASE_FOLDER.length());
            String dstDir = DST_BASE_FOLDER + userCurrentDir.substring(SRC_BASE_FOLDER.length());
            System.out.println("REM - Final SRC Dir = " + subDir);
            System.out.println("REM - FInal DST Dir = " + dstDir);
            SRC_BASE_FOLDER_FINAL = subDir;
            DST_BASE_FOLDER_FINAL = dstDir;
            correctPathFound = true;
        }
    } //end of for loop
    System.out.println("**************************************************************************");
    System.out.println("REM - Final : SRC_BASE_FOLDER_FINAL = "+SRC_BASE_FOLDER_FINAL  );
    System.out.println("REM - Final : DST_BASE_FOLDER_FINAL = "+DST_BASE_FOLDER_FINAL  );
    System.out.println("**************************************************************************");
  }
  /******************************************************************************************/
  private static void printByteArray(String msg, byte[] ba) {
    System.out.print(msg);
    for(int i=0;i<ba.length;i++) {
        System.out.print(ba[i]);
    }
    System.out.println();
  }

  /******************************************************************************************/
  private static void printCharArray(String msg, char[] ba) {
    System.out.print(msg);
    for(int i=0;i<ba.length;i++) {
        System.out.print(ba[i]);
    }
    System.out.println();
  }

  /******************************************************************************************/
  public static String locatePropertiesFile() {
	  List<String> list = new ArrayList<String>();
      list.add("C:\\FOLDER_SYNC_PROPERTIES.TXT");
      list.add("D:\\FOLDER_SYNC_PROPERTIES.TXT");
      list.add("D:\\Programs_Portable_GIT\\Java_Utils\\FOLDER_SYNC_PROPERTIES.TXT");
      list.add("D:\\Program_Files_Portable\\Java_Utils\\FOLDER_SYNC_PROPERTIES.TXT");
	  
	  for(String filePath : list) {
		if((new File(filePath)).exists()) {
			return filePath;
		}
	  }
            
      return "";
  }
  /******************************************************************************************/
}
