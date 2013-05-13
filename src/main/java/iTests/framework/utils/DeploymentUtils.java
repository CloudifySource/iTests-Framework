package iTests.framework.utils;

import org.junit.Assert;

import java.io.*;
import java.util.Properties;

public class DeploymentUtils {

	private static final String SGTEST_VERSION_PROP = "sgtest.version";
	private static final String STATIC_SGTEST_VERSION = "3.0.0-SNAPSHOT";

	/**
	 * @deprecated this method was used to create a classes directory for an application
	 * this is now not needed since we have compiled jar files in the local repository
	 * 
	 * @param appName
	 * @return null
	 */
	@Deprecated
    public static File prepareApp(String appName) {
        File appsFolder = new File("src/main/resources/apps/" + appName);
        File metaCommonLib = new File("src/main/resources/apps/meta-common/lib");
        File metaCommonTarget = new File(metaCommonLib.getParentFile() + "/target");
        File commonLib = new File(appsFolder.getAbsolutePath() + "/common/lib");
        File commonTarget = new File(commonLib.getParentFile() + "/target");

        File target = null;
        File lib;

        for (File f : appsFolder.listFiles()) {
            if (f.isDirectory() && !f.getName().contains("common") && !f.getName().contains(".svn")) {
                target = new File(f.getAbsolutePath() + "/target/" + f.getName());
                target.mkdir();
                lib = new File(target.getAbsolutePath() + "/lib");
                lib.mkdir();
                File puLib = new File(f.getAbsolutePath() + "/lib");
                File metainfSpringDir = new File(f.getAbsolutePath() + "/src/META-INF/spring");

                try {
                    if (metaCommonLib.exists())
                        copyLibs(metaCommonLib, lib);
                    if (commonLib.exists())
                        copyLibs(commonLib, lib);
                    if (puLib.exists())
                        copyLibs(puLib, lib);
                    copyClasses(new File(metaCommonTarget.getAbsolutePath() + "/classes"), target);
                    copyClasses(new File(commonTarget.getAbsolutePath() + "/classes"), target);
                    copyClasses(new File(f.getAbsolutePath() + "/target/classes"), target);
                    copyDirectory(metainfSpringDir, target);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return target;
    }
	
	/**
	 * Ever since we changed the pu instance work directory to include some guid suffix
	 * this method does not work accurately if a pu with the same name was deployed more than once
	 * and not cleaned properly 
	 */
	@Deprecated
	public static File getProcessingUnitInstanceWorkDirectory(String puName, int id) {
		String puInstanceFolderPrefix = (puName + "_" + id + "_").replace('.', '_');

		String workDirProp = System.getProperty("com.gs.work", 
				ScriptUtils.getBuildPath() + "/work");

		File processingUnitsDir = new File(workDirProp, "processing-units");
		if (!processingUnitsDir.isDirectory()) {
			Assert.fail("Could not locate processing-units directory");
		}

		for (File dir : processingUnitsDir.listFiles()) {
			if (dir.getName().startsWith(puInstanceFolderPrefix)) {
				return dir;
			}
		}

		Assert.fail("Could not locate processing unit instance directory for " + puInstanceFolderPrefix);
		return null;
	}

	public static void setPUSLCache(String appStr, String puStr) throws IOException {

        String path;

        if(System.getProperty("suiteType").contains("XAP")){
            path = "./apps/";
        }
        else{
            path = "src/main/resources/apps/";
        }

		File srcFile = new File(path + appStr + "/" + puStr + "/src/META-INF/spring/pu-slcache.xml");
		File destFile = new File(path + appStr + "/" + puStr + "/target/" + puStr + "/META-INF/spring/pu.xml");
		copyFile(srcFile, destFile);
	}

	public static File getProcessingUnit(String app, String pu) {

        if(System.getProperty("suiteType").contains("XAP")){
            String s = System.getProperty("file.separator");
            String pathToJar = app + s + pu + s + getSGTestVersion() + s;
            if(app.equals(pu)) {
                pathToJar = pu + s + getSGTestVersion() + s;
            }
            return new File(getAppsPath(s) + pathToJar + pu + "-" + getSGTestVersion() + ".jar" );
        }
        else{
            return new File("src/main/resources/apps/" + app + "/" + pu + "/target/" + pu);
        }
	}

    public static File getArchive(String pu) {
        //in case we want to update the calls to include the version number
		/*String name = pu.split("-")[0];
		Pattern pattern = Pattern.compile("\\d\\.\\d\\.\\d");
		Matcher matcher = pattern.matcher(pu);
		if(matcher.find())
			version = matcher.group();*/

        String s = System.getProperty("file.separator");
        String version = getSGTestVersion();
        String[] dotSplit = pu.split("\\.");
        String name = dotSplit[0];
        String type = dotSplit[dotSplit.length - 1];
        return new File(getAppsPath(s) + "archives" + s + name + s + version + s + name + "-" + version + "." + type);
    }
	
	public static String getSGTestVersion() {
		String versionFromSystem = System.getProperty(SGTEST_VERSION_PROP);
		if(versionFromSystem == null){
			String versionFromFile = loadPropertiesFromClasspath("sgtest.properties").getProperty(SGTEST_VERSION_PROP);
			if(versionFromFile == null){
				return STATIC_SGTEST_VERSION;
			}
			else{ 
				return versionFromFile;
			}
		}
		else{
			return versionFromSystem;
		}
	}

	public static Properties loadPropertiesFromClasspath(String classpath) {
		InputStream is = ClassLoader.getSystemResourceAsStream(classpath);
		Properties props = new Properties();
		try {
			props.load(is);
		} catch (IOException e ) {
			e.printStackTrace();
		}
		catch (RuntimeException e ) {
			e.printStackTrace();
		}
		return props;
	}

	public static String getAppsPath(String s) {

        if(System.getProperty("suiteType").contains("XAP")){
            return getLocalRepository() + "repository" + s + "com" + s + "gigaspaces" + s + "quality" + s + "sgtest" + s + "apps" + s;
        }
        else{
            return getLocalRepository() + "repository" + s + "org" + s + "cloudifysource" + s + "quality" + s + "iTests" + s;
        }
	}

    public static String getQualityItestsPath(String s) {
        return getLocalRepository() + "repository" + s + "org" + s + "cloudifysource" + s + "quality" + s + "iTests" + s;
    }

    private static void copyLibs(File source, File target) throws IOException {
        copyDirectory(source, target);
    }

    private static void copyClasses(File source, File target) throws IOException {
        copyDirectory(source, target);
    }

    private static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) {
                dstDir.mkdir();
            }

            String[] children = srcDir.list();
            for (int i = 0; i < children.length; i++) {
                if (children[i].equals(".svn"))
                    continue;
                copyDirectory(new File(srcDir, children[i]),new File(dstDir, children[i]));
            }
        } else {
            copyFile(srcDir, dstDir);
        }
    }

	private static void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public static String getLocalRepository() {
        String s = System.getProperty("file.separator");
        return System.getProperty("user.home") + s + ".m2" + s;
    }

	public static String getProcessingUnitName(String pu) {
		return pu + "-" + getSGTestVersion();
	}

	public static String getProcessingUnitName(String pu, int instanceNumber) {
		return getProcessingUnitName(pu) + " [" + instanceNumber + "]";
	}

}
