package info.knowledgeTechnology.docks.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import org.apache.commons.io.FileUtils;

public class ConsistencyManager {

	private static void copyFile(File source, File dest) {
		if (source.exists() && !source.isDirectory()) {
			InputStream is = null;
			OutputStream os = null;
			try {
				is = new FileInputStream(source);
				os = new FileOutputStream(dest);
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					is.close();
					os.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

	public static boolean isConsistent(String configname, String filename)
	{
		String backupPath = "config/"+configname+"/.backup/";
		String filePath = "config/"+configname+"/";
		boolean consistent = true;
		
		if(!(new File(backupPath)).exists())
			(new File(backupPath)).mkdirs();
		if(!(new File(backupPath+"model")).exists())
			(new File(backupPath+"model")).mkdirs();
		
		File backupFile = new File(backupPath+filename);
		File orgFile = new File(filePath+filename);
		if(!backupFile.exists())
		{
			consistent = false;
			
		}else{
			try {
				consistent = FileUtils.contentEquals(orgFile, backupFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		if(!consistent)
		{
			System.out.println("file "+filename+" was changed");
			copyFile(orgFile, backupFile);
		}else
			System.out.println("file "+filename+" is consistent");
		return consistent;
	}
}
