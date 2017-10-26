package info.knowledgeTechnology.docks.Utils;

import info.knowledgeTechnology.docks.UniqueProjectDescriptorKnowledgeTechnologyDocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class FileLoader {
	private static int INSIDE_ORIGINAL_PROJECT = 0;
	private static int LINKED_TO_PROJECT = 1;
	private static int LINKED_TO_JAR = 2;

	public static int getLinkStatus() {
		URL main = UniqueProjectDescriptorKnowledgeTechnologyDocks.class
				.getResource("UniqueProjectDescriptorKnowledgeTechnologyDocks.class");
		if (!"file".equalsIgnoreCase(main.getProtocol())) {
			return LINKED_TO_JAR;
		}
		String projectLocatorClass = main.getPath();
		String projectLocatorPath = projectLocatorClass
				.replace(
						"/bin/info/knowledgeTechnology/docks/UniqueProjectDescriptorKnowledgeTechnologyDocks.class",
						"");
		String current = System.getProperty("user.dir");
		if (current.equals(projectLocatorPath))
			return INSIDE_ORIGINAL_PROJECT;
		return LINKED_TO_PROJECT;
	}

	public static URL resolve(String path) {
		return (new FileLoader()).resolvePath(path);
	}

	private static String getProjectPath() {
		URL main = UniqueProjectDescriptorKnowledgeTechnologyDocks.class
				.getResource("UniqueProjectDescriptorKnowledgeTechnologyDocks.class");
		String projectLocatorClass = main.getPath();
		String projectLocatorPath = projectLocatorClass
				.replace(
						"/bin/info/knowledgeTechnology/docks/UniqueProjectDescriptorKnowledgeTechnologyDocks.class",
						"");
		return projectLocatorPath;
	}

	
	public static void copyFileToLocalDirectory(String path, String outputPath)
	{
		int linkStatus = getLinkStatus();
		if (linkStatus == LINKED_TO_JAR) {
			String tmpPath = System.getProperty("user.dir") + outputPath;
			File f = new File(tmpPath);
			(new File(f.getParent())).mkdirs();
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(tmpPath);
				InputStream is = UniqueProjectDescriptorKnowledgeTechnologyDocks.class
						.getResourceAsStream("/" + path);
				byte[] buf = new byte[2048];
				int r;
				while (-1 != (r = is.read(buf))) {
					fos.write(buf, 0, r);
				}
				fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (linkStatus == LINKED_TO_PROJECT || linkStatus == INSIDE_ORIGINAL_PROJECT) {
			String searchPath = getProjectPath() + "/resources/" + path;
			String tmpPath = System.getProperty("user.dir")+"/" + outputPath;

			File source = new File(searchPath);
			File dest  = new File(tmpPath);
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
			} else {
				throw new IllegalStateException("Could not find file: "
						+ searchPath);
			}
		}
	}
	public static String runFile(String path) {
		int linkStatus = getLinkStatus();
		if (linkStatus == LINKED_TO_JAR) {
			String tmpPath = System.getProperty("user.dir") + "/tmp/" + path;
			File f = new File(tmpPath);
			(new File(f.getParent())).mkdirs();
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(tmpPath);
				InputStream is = UniqueProjectDescriptorKnowledgeTechnologyDocks.class
						.getResourceAsStream("/" + path);
				byte[] buf = new byte[2048];
				int r;
				while (-1 != (r = is.read(buf))) {
					fos.write(buf, 0, r);
				}
				fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (f.exists() && !f.isDirectory()) {
				return tmpPath;
			} else {
				throw new IllegalStateException("Could not find file: "
						+ tmpPath);
			}

		}
		if (linkStatus == LINKED_TO_PROJECT || linkStatus == INSIDE_ORIGINAL_PROJECT) {
			String searchPath = getProjectPath() + "/resources/" + path;
			// System.out.println(searchPath);
			File f = new File(searchPath);
			if (f.exists() && !f.isDirectory()) {
				return searchPath;
			} else {
				throw new IllegalStateException("Could not find file: "
						+ searchPath);
			}
		}
//		if (linkStatus == INSIDE_ORIGINAL_PROJECT) {
//			String searchPath = getProjectPath() + "/" + path;
//			// System.out.println(searchPath);
//			File f = new File(searchPath);
//			if (f.exists() && !f.isDirectory()) {
//				return searchPath;
//			} else {
//				throw new IllegalStateException("Could not find file: "
//						+ searchPath);
//			}
//		}
		return null;

	}

	public static InputStream resolveAsInputStream(String path) {
		FileLoader fl = new FileLoader();
		URL main = UniqueProjectDescriptorKnowledgeTechnologyDocks.class
				.getResource("UniqueProjectDescriptorKnowledgeTechnologyDocks.class");
		String searchPath = "/" + path;
		InputStream res = null;
		if (!"file".equalsIgnoreCase(main.getProtocol())) {
			System.out.println("trying to load from resource at:");
			System.out.println(searchPath);
			res = fl.getClass().getResourceAsStream(searchPath);
			if (res != null)
				System.out.println("found in resource");
			else
				throw new IllegalStateException("Could not find resource: "
						+ searchPath);
			return res;
		}

		// File path = new File(main.getPath());
		String projectLocatorClass = main.getPath();
		String projectLocatorPath = projectLocatorClass
				.replace(
						"bin/info/knowledgeTechnology/docks/UniqueProjectDescriptorKnowledgeTechnologyDocks.class",
						"");

		System.out.println("trying to open file: " + path);
		String current = System.getProperty("user.dir");
		System.out.println("Current working directory in Java : " + current);

		ClassLoader cl = fl.getClass().getClassLoader();

		String resourcePath = path;
		String prefixPath = "./";
		searchPath = prefixPath + resourcePath;
		File f = new File(searchPath);

		if (f.exists() && !f.isDirectory()) {
			// do something

			System.out.println("found as file on the path: " + searchPath);
			System.out.println("looks like you are working inside docks");
			try {
				res = new FileInputStream(searchPath);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("not found at: " + searchPath);
			System.out
					.println("trying to load from linked docks source code at:");
			searchPath = projectLocatorPath + "resources/" + resourcePath;
			System.out.println(searchPath);
			f = new File(searchPath);
			if (f.exists() && !f.isDirectory()) {
				// do something
				System.out.println("found as file on the path: " + searchPath);
				System.out
						.println("looks like you are working in a project that is linked to the docks source code");
				// res = cl.getResource(searchPath);
				try {
					res = new FileInputStream(searchPath);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println(res);
			} else {
				throw new IllegalStateException("Could not find resource: "
						+ resourcePath);
			}
		}
		return res;
	}

	private URL resolvePath(String path) {

		URL main = UniqueProjectDescriptorKnowledgeTechnologyDocks.class
				.getResource("UniqueProjectDescriptorKnowledgeTechnologyDocks.class");
		String searchPath = "/" + path;
		URL res = null;
		if (!"file".equalsIgnoreCase(main.getProtocol())) {
			System.out.println("trying to load from resource at:");
			System.out.println(searchPath);
			res = getClass().getResource(searchPath);
			if (res != null)
				System.out.println("found in resource");
			else
				throw new IllegalStateException("Could not find resource: "
						+ searchPath);
			return res;
		}

		// File path = new File(main.getPath());
		String projectLocatorClass = main.getPath();
		String projectLocatorPath = projectLocatorClass
				.replace(
						"bin/info/knowledgeTechnology/docks/UniqueProjectDescriptorKnowledgeTechnologyDocks.class",
						"");

		System.out.println("trying to open file: " + path);
		String current = System.getProperty("user.dir");
		System.out.println("Current working directory in Java : " + current);

		ClassLoader cl = this.getClass().getClassLoader();

		String resourcePath = path;
		String prefixPath = "./";
		searchPath = prefixPath + resourcePath;
		File f = new File(searchPath);

		if (f.exists() && !f.isDirectory()) {
			// do something

			System.out.println("found as file on the path: " + searchPath);
			System.out.println("looks like you are working inside docks");
			res = cl.getResource(searchPath);
		} else {
			System.out.println("not found at: " + searchPath);
			System.out
					.println("trying to load from linked docks source code at:");
			searchPath = projectLocatorPath + "resources/" + resourcePath;
			System.out.println(searchPath);
			f = new File(searchPath);
			if (f.exists() && !f.isDirectory()) {
				// do something
				System.out.println("found as file on the path: " + searchPath);
				System.out
						.println("looks like you are working in a project that is linked to the docks source code");
				// res = cl.getResource(searchPath);
				try {
					res = f.toURI().toURL();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(res);
			} else {
				throw new IllegalStateException("Could not find resource: "
						+ resourcePath);
			}
		}
		return res;
	}
}
