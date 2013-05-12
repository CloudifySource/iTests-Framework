/**
 * 
 */
package iTests.framework.utils;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;

import static iTests.framework.utils.AdminUtils.loadGSCs;
import static iTests.framework.utils.AdminUtils.loadGSM;
import static iTests.framework.utils.AssertUtils.sleep;

/**
 * @author rafi
 *
 */
public class TestUtils {

	/**
	 * waits for numOfMachines and 1 GSA, loads 1 gsm and numOfGscsPerMachine
	 */
	public static GridServiceManager waitAndLoad(Admin admin, int numOfMachines,int numOfGscsPerMachine) {
		admin.getMachines().waitFor(numOfMachines);
		GridServiceAgent gsa = admin.getGridServiceAgents().waitForAtLeastOne();
		GridServiceManager gsm = loadGSM(gsa.getMachine());
		if (numOfGscsPerMachine > 0){
			for (GridServiceAgent gsa1 : admin.getGridServiceAgents()) {
				loadGSCs(gsa1,numOfGscsPerMachine);
			}
		}
		return gsm;
	}
	
	public static ProcessingUnit deploySpace(GridServiceManager gsm, String spaceName,int numberOfInstances, int numberOfBackups){
		ProcessingUnit pu = gsm.deploy(new SpaceDeployment(spaceName).numberOfInstances(numberOfInstances).numberOfBackups(numberOfBackups));
		pu.waitForSpace();
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		return pu;
	}
			
	public static void repetitive(Runnable repeatedAssert, int timeout)
	{
	    for(int delay = 0; delay < timeout; delay += 5)
	    {
		try
		{
		    repeatedAssert.run();
		    return;
		}
		catch(AssertionError e)
		{
		    try
		    {
		    	sleep(5);
		    }
		    catch (InterruptedException e1)
		    {
		    }
		}
	    }
	    repeatedAssert.run();
	}

    public static String getClasspathString() {
        StringBuffer classpath = new StringBuffer();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        URL[] urls = ((URLClassLoader)classLoader).getURLs();
        for(int i=0; i < urls.length; i++) {
            classpath.append(urls[i].getFile()).append("\r\n");
        }

        return classpath.toString();
    }

    public static void writeTextFile(File file, String text) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            out.println(text);
        } finally {
            out.close();
        }
    }
}
