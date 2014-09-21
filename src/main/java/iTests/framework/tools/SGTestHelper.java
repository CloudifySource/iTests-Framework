package iTests.framework.tools;

import com.j_spaces.kernel.PlatformVersion;
import iTests.framework.utils.ScriptUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SGTestHelper {

    private static final String DEV_BUILD_PATH_PROP = "dev.build.path";

	/**
	 * This is a very cool method which returns jar or directory from where the supplied class has loaded.
	 * 
	 * @param claz the class to get location.
	 * @return jar/path location of supplied class.
	 */
	public static String getClassLocation(Class claz)
	{
		return claz.getProtectionDomain().getCodeSource().getLocation().toString().substring(5);
	}

	public static boolean isDevMode() {
		boolean isDevMode;

		if (System.getenv().containsKey("DEV_ENV")) {
            return Boolean.valueOf(System.getenv("DEV_ENV"));
		} else if (System.getProperties().containsKey("DEV_ENV")) {
			return Boolean.getBoolean("DEV_ENV");
		} else if (System.getProperties().containsKey("iTests.cloud.enabled")) {
            if   (Boolean.getBoolean("iTests.cloud.enabled")){
                return false;
            }
        }

		if (ScriptUtils.isWindows()) {
			isDevMode = !System.getenv("USERNAME").equals("ca");
		}
		else {
			isDevMode = !System.getProperty("user.name").equals("tgrid");
		}
		return isDevMode;
	}

	public static String getSGTestSrcDir() {
		String sgtestSrcDir;
		if (isDevMode()) {
			sgtestSrcDir = getSGTestRootDir() + "/src";
		}
		else {
			sgtestSrcDir = getSGTestRootDir() + "/tmp";
		}
		return sgtestSrcDir;

	}

   public static String getBackwardsRecipesBranch() {
      return System.getProperty("org.cloudifysource.recipes-backwards.branch", "master");
   }
	
	public static String getSuiteName(){
		return System.getProperty("iTests.suiteName", "");
	}
	
	public static String getSuiteId() {
		return System.getProperty("iTests.suiteId", "");
	}

	public static String getSuiteType() {
		return System.getProperty("iTests.suiteType", "");
	}

    public static boolean isXap(){

        if(isDevMode()){
            return PlatformVersion.getOfficialVersion().contains("XAP");
        }
        else{
            return getSuiteType().contains("XAP");
        }
    }

	//each suite has it's own work dir.
	public static String getWorkDirName() {
		String suiteDir = getSuiteName();
		String suiteId = getSuiteId();
		Integer numberOfSuite = Integer.getInteger("iTests.numOfSuites",1);

		if (StringUtils.isEmpty(suiteDir) || StringUtils.isEmpty(suiteId) || numberOfSuite == 1) {
			return "work";
		} else {
			return suiteDir + suiteId + "_work";
		}
	}

	/** @return SGTest root directory */
	public static String getSGTestRootDir(){
		return new File(".").getAbsolutePath();
	}

	public static String getBuildDir(){
        if (SGTestHelper.isDevMode()) {
            String buildPathOnLinuxMachine = System.getProperty(DEV_BUILD_PATH_PROP);
            if (buildPathOnLinuxMachine == null) {
                throw new IllegalStateException("You are running in dev mode. please set the dev.build.path System Property to point to the build path on your linux machine");
            }
            else {
                return buildPathOnLinuxMachine;
            }
        }
		return ScriptUtils.getBuildPath();
	}

	public static String getCustomCloudConfigDir(String cloudName) {
		return getSGTestRootDir() + "/src/main/resources/custom-cloud-configs/" + cloudName;
	}

    public static String getBranchName() {
        return System.getProperty("branch.name", "");
    }

    public static Properties getPropertiesFromFile(String propertiesFileName) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFileName));
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + propertiesFileName + " file - " + e, e);
        }


        return properties;
    }

    public static void main(String[] args) {
        isXap();
    }
}
