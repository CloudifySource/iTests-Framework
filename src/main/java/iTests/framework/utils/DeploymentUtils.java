package iTests.framework.utils;

import iTests.framework.tools.SGTestHelper;
import org.junit.Assert;

import java.io.*;
import java.util.Properties;

public class DeploymentUtils {

    private static final String SGTEST_VERSION_PROP = "sgtest.version";
    private static final String STATIC_SGTEST_VERSION = "3.0.0-SNAPSHOT";

    /**
     * @param appName
     * @return null
     * @deprecated this method was used to create a classes directory for an application
     *             this is now not needed since we have compiled jar files in the local repository
     */
    @Deprecated
    public static File prepareApp(String appName) {
        return null;
    }

    public static void setPUSLCache(String appStr, String puStr) throws IOException {

        String path;

        if (SGTestHelper.isXap()) {
            path = "./apps/";
        } else {
            path = "src/main/resources/apps/";
        }

        File srcFile = new File(path + appStr + "/" + puStr + "/src/META-INF/spring/pu-slcache.xml");
        File destFile = new File(path + appStr + "/" + puStr + "/target/" + puStr + "/META-INF/spring/pu.xml");
        copyFile(srcFile, destFile);
    }

    public static File getWebProcessingUnit(String app, String pu) {
        String s = System.getProperty("file.separator");
        String pathToJar = app + s + pu + s + getSGTestVersion() + s;
        if (app.equals(pu)) {
            pathToJar = pu + s + getSGTestVersion() + s;
        }
        return new File(getAppsPath(s) + pathToJar + pu + "-" + getSGTestVersion() + ".war");
    }

    public static File getProcessingUnit(String app, String pu) {
        String s = System.getProperty("file.separator");
        String pathToJar = app + s + pu + s + getSGTestVersion() + s;
        if (app.equals(pu)) {
            pathToJar = pu + s + getSGTestVersion() + s;
        }
        return new File(getAppsPath(s) + pathToJar + pu + "-" + getSGTestVersion() + ".jar");
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
        if (versionFromSystem == null) {
            String versionFromFile = loadPropertiesFromClasspath("sgtest.properties").getProperty(SGTEST_VERSION_PROP);
            if (versionFromFile == null) {
                return STATIC_SGTEST_VERSION;
            } else {
                return versionFromFile;
            }
        } else {
            return versionFromSystem;
        }
    }

    public static Properties loadPropertiesFromClasspath(String classpath) {
        InputStream is = ClassLoader.getSystemResourceAsStream(classpath);
        Properties props = new Properties();
        if (is != null) {
            try {
                props.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return props;
    }

    public static String getAppsPath(String s) {
        String localRepoProp = System.getProperty("maven.repo.local");
        if(localRepoProp != null)
            return getLocalRepository() + "com" + s + "gigaspaces" + s + "quality" + s + "sgtest" + s + "apps" + s;
        else
            return getLocalRepository() + "repository" + s + "com" + s + "gigaspaces" + s + "quality" + s + "sgtest" + s + "apps" + s;

    }

    public static String getQualityItestsPath(String s) {
        String localRepoProp = System.getProperty("maven.repo.local");
        if(localRepoProp != null)
            return getLocalRepository() + "org" + s + "cloudifysource" + s + "quality" + s + "iTests" + s;
        else
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
                copyDirectory(new File(srcDir, children[i]), new File(dstDir, children[i]));
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
        String localRepoProp = System.getProperty("maven.repo.local");
        System.out.println("maven.repo.local="+localRepoProp);
        if(localRepoProp != null)
            return localRepoProp  + s;
        return System.getProperty("user.home") + s + ".m2" + s;
    }

    public static String getProcessingUnitName(String pu) {
        return pu + "-" + getSGTestVersion();
    }

    public static String getProcessingUnitName(String pu, int instanceNumber) {
        return getProcessingUnitName(pu) + " [" + instanceNumber + "]";
    }

}
