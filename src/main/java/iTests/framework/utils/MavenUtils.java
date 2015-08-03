package iTests.framework.utils;

import iTests.framework.tools.SGTestHelper;
import org.openspaces.admin.machine.Machine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static iTests.framework.utils.LogUtils.log;
import static iTests.framework.utils.ScriptUtils.getBuildPath;

/**
 * Utility class for maven operations
 * @author elip
 *
 */
public class MavenUtils {

	public static final String username = SSHUtils.SSH_USERNAME;
	public static final String password = SSHUtils.SSH_PASSWORD;
	public static String mavenRepLocation;
	public static final String mavenCreate = "mvn os:create -Dtemplate=";
	public static final String mavenCompile = "mvn compile";
	public static final String mavenPackage = "mvn package";
	public static final String mavenDeploy = "mvn os:deploy";
    public static final String mavenUndeploy = "mvn os:undeploy";
	public static final String mavenRun = "mvn os:run";
	public static final String mavenRunStandalone = "mvn os:run-standalone";

    public final static long DEFAULT_TEST_TIMEOUT = 15 * 60 * 1000;

    /**
	 * installs the maven repository
	 * @throws Exception 
	 */
	public static boolean installMavenRep(Machine machine) throws Exception {
		mavenRepLocation = getBuildPath()+"/../m2Repository";
		String mavenHome = getBuildPath() + "/tools/maven";
		File customInstallRepFile  = createCustomInstallMavenRepFile(mavenHome);
        LogUtils.log("running command " + "cd " + mavenHome + ";" + "./installmavenrepBackup.sh");
		String scriptOutPut = SSHUtils.runCommand(machine.getHostAddress(), DEFAULT_TEST_TIMEOUT * 2,
                "cd " + mavenHome + ";" + "./installmavenrepBackup.sh", MavenUtils.username, MavenUtils.password);
		if (scriptOutPut == null) return false;
		customInstallRepFile.delete();
		return true;
	}
		
	private static File createCustomInstallMavenRepFile(String mavenHome) throws IOException {
		String customPath = mavenHome+"/installmavenrepBackup.sh";
		String oldPath = mavenHome+"/installmavenrep.sh";
		String mvnInstallstr = "mvn install:install-file";
		String mvnInstallPluginStr = "mvn -f maven-openspaces-plugin/pom.xml install -DcreateChecksum=true";
		File customFile = new File(customPath);
		customFile.createNewFile();
		customFile.setExecutable(true);
		IOUtils.copyFile(oldPath, customPath);
		final Map<String, String> propsToReplace = new HashMap<String, String>();
		propsToReplace.put(mvnInstallstr, mvnInstallstr+" -X -Dmaven.repo.local="+mavenRepLocation);
		propsToReplace.put(mvnInstallPluginStr, mvnInstallPluginStr+" -X -Dmaven.repo.local="+mavenRepLocation);
		IOUtils.replaceTextInFile(customPath, propsToReplace);
		return new File (customPath);
	}

    /**
     * checks if maven created the my-app folder
     * this method will be usually invoked to ensure that maven os:create was successful
     * @param hostName - the host on witch the app should be created on
     * @return
     */
    public static boolean isAppExists(String hostName) {
        String pathName = getBuildPath() + "/../my-app";
        File myAppDir = new File(pathName);
        return myAppDir.isDirectory();
    }

	/**
	 * checks if maven created the my-app folder
	 * this method will be usually invoked to ensure that maven os:create was successful
	 * @param hostName - the host on which the app should be created on
	 * @return
	 */
	public static boolean isAppExists(String hostName, String appName) {
		String pathName = getBuildPath() + "/../" + appName;
		String command = "if [ -d \"" +pathName + "\" ]; then echo 'exist'; fi";
		return SSHUtils.runCommand(hostName, 10*1000, command, SSHUtils.SSH_USERNAME, SSHUtils.SSH_PASSWORD).contains("exist");
	}


	/**
	 * delete the app maven created
	 * @param machine
	 */
	public static void deleteApp(Machine machine, String appName) {
		String buildPath = ScriptUtils.getBuildPath();
		SSHUtils.runCommand(machine.getHostAddress(), DEFAULT_TEST_TIMEOUT * 2,
                "cd " + buildPath + "/..;rm -rf " + appName, MavenUtils.username, MavenUtils.password);

	}

	public static boolean importMuleJars(Machine machine) throws Exception {
//		WGetUtils.wget("http://dist.codehaus.org/mule/distributions/mule-3.2.0-embedded.jar",
//	        ScriptUtils.getBuildPath() + "/lib/platform/mule/");
		String mulePath = getBuildPath() + "/lib/platform/mule";

        if(SGTestHelper.isXap()){
            SSHUtils.runCommand(machine.getHostAddress(), 600000, "mkdir " + mulePath + ";cp -r /export/tgrid/mule-jars/* " +
                    mulePath, MavenUtils.username, MavenUtils.password);
        }
        else{
            SSHUtils.runCommand(machine.getHostAddress(), 600000, "mkdir " + mulePath + ";cp -r /export/utils/temp/\"mule jars\"/* " +
                    mulePath, MavenUtils.username, MavenUtils.password);
        }

		return true;
	}

	public static void deleteMuleJars(Machine machine){
		String platformDirPath = getBuildPath() + "/lib/platform";
		SSHUtils.runCommand(machine.getHostAddress(), 600000, "cd " + platformDirPath + ";rm -rf " + platformDirPath + "/mule",
                MavenUtils.username, MavenUtils.password);
	}

    public static void packageMaven(Machine machine, String appPath){

        log("Packaging...");
        log("running command " + "cd " + appPath + ";" + MavenUtils.mavenPackage +" -Dmaven.repo.local="+mavenRepLocation);
        SSHUtils.runCommand(machine.getHostAddress(), DEFAULT_TEST_TIMEOUT,
                "cd " + appPath + ";" + MavenUtils.mavenPackage +" -Dmaven.repo.local="+mavenRepLocation, username , password);
    }

    public static void deployMaven(Machine machine, String appPath, String group){

        log("deploying...");
        log("running command " + "cd " + appPath + ";" + MavenUtils.mavenDeploy +" -Dmaven.repo.local="+mavenRepLocation + " -Dgroups=" + group);
        SSHUtils.runCommand(machine.getHostAddress(), DEFAULT_TEST_TIMEOUT,
                "cd " + appPath + ";" + MavenUtils.mavenDeploy +" -Dmaven.repo.local="+mavenRepLocation + "-Dgroups=" + group, username , password);
    }

}