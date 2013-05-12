package iTests.framework.utils;

import org.codehaus.groovy.control.CompilationFailedException;

import java.io.IOException;

/**
 * @author Sagi Bernstein
 *
 */
public class WGetUtils {

	private static String prefix = "test/groovy/";
	
	public static void wget(String url) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException{  
		ScriptUtils.runGroovy(prefix + "wget.groovy", "wget", url);
	}

	public static void wget(String url, String destFolder) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException{  
		ScriptUtils.runGroovy(prefix + "wget.groovy", "wget", url, destFolder);
	}

	public static void wget(String url, String destFolder, String destFileName) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException{  
		ScriptUtils.runGroovy(prefix + "wget.groovy", "wget", url, destFolder, destFileName);
	}
	
}