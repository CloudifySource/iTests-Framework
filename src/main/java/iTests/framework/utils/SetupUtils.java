package iTests.framework.utils;

import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static iTests.framework.utils.LogUtils.log;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Utility methods for test setup.
 * 
 * @author Moran Avigdor
 */
public class SetupUtils {

	public static final String GSA_MACHINES_COUNT_PROP = "gsa.machines.count";

    private static final long DEFAULT_CLEAN_SETUP_TIMEOUT_MINUTES = 3;

	private static final int SSH_TIMEOUT = 60000;

	private static Map<String, Set<String>> gsaStartupPIDs = null;

	public static final String USERNAME = "tgrid";
	public static final String PASSWORD = "tgrid";
	public static final String LINUX_HOST_PREFIX = "192.168.9";
	private static final String LIST_GSA_PROCESSES_CMD = "ps -ww u -U tgrid | grep java | grep GSA | grep -v PID | awk '{ print $2 }'";
	private static final String LIST_LUS_PROCESSES_CMD = "ps -ww u -U tgrid | grep java | grep LH | grep -v PID | awk '{ print $2 }'";
	private static final String LIST_JAVA_PROCESSES_CMD = "ps -U tgrid | grep java | grep -v PID | awk '{ print $1 }'";

	public static void assertCleanSetup(final Admin admin) {
		assertCleanSetup(admin, DEFAULT_CLEAN_SETUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
	}
	
	/**
	 * asserts that there is a clean setup before the test is run
	 */
	public static void assertCleanSetup(final Admin admin, final long timeout, final TimeUnit timeunit) {

		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

		GridServiceAgent waitForAtLeastOne = admin.getGridServiceAgents().waitForAtLeastOne(timeout,timeunit);
		if (waitForAtLeastOne == null && admin.getGridServiceAgents().getAgents().length == 0) {
			AssertJUnit.fail("Grid Service Agent was not found in the specified timeout of " + timeout + " " + timeunit);
		}

        AssertJUnit.assertNotNull(admin.getGridServiceAgents()
                .waitForAtLeastOne(timeout, timeunit));

		while(true) {

			try {
				// sleep for two seconds to allow discovery to find whatever is left around
				Thread.sleep(2000);
			} catch (final InterruptedException e1) {
				// ignore
			}

			SetupUtils.cleanSetup(admin);
			
			boolean isDone = true;
			
			final int numberOfElasticServiceManagers = admin.getElasticServiceManagers().getSize();
			if (numberOfElasticServiceManagers > 0) {
				LogUtils.log("Waiting for " + numberOfElasticServiceManagers + " ElasticServiceManagers to shutdown");
				isDone = false;
			}
			
			final int numberOfGridServiceManagers = admin.getGridServiceManagers().getSize();
			if (numberOfGridServiceManagers > 0) {
				LogUtils.log("Waiting for " + numberOfGridServiceManagers + " GridServiceManagers to shutdown");
				isDone = false;
			}
			
			final int numberOfGridServiceContainers = admin.getGridServiceContainers().getSize();
			if (numberOfGridServiceContainers > 0) {
				LogUtils.log("Waiting for " + numberOfGridServiceContainers + " GridServiceContainers to shutdown");
				isDone = false;
			}
	    	
			final int numberOfProcessingUnits = admin.getProcessingUnits().getSize();
			if (numberOfProcessingUnits > 0) {
				LogUtils.log("Waiting for " + numberOfProcessingUnits + " ProcessingUnits to shutdown");
				isDone = false;
			}
				    	  
			if (isDone) {
				break;
			}
			
			if (System.currentTimeMillis() > end) {
				try {
		        	TeardownUtils.snapshot(admin);
		        } catch(final Throwable t) {
		        	LogUtils.log("Failed on snapshot", t);
		        }
			
				AssertJUnit.fail("Failed to cleanup existing processes beforeTest. Timeout = " + timeout + " " + timeunit.toString());
			}
	    }
		assertEquals("Unexpected ESM", 0, admin.getElasticServiceManagers().getSize());
		assertEquals("Unexpected GSM", 0, admin.getGridServiceManagers().getSize());
		assertEquals("Unexpected GSC", 0, admin.getGridServiceContainers().getSize());
		assertEquals("Unexpected PUs", 0, admin.getProcessingUnits().getSize());
	}

	private static void cleanSetup(final Admin admin) {
				/*
		 * assert that we have a clean environment - try to kill any left overs
		 */
		for (final ElasticServiceManager esm : admin.getElasticServiceManagers()) {
			try {
                log("killing ESM [ID:" + esm.getAgentId() + "] [PID: " + esm.getVirtualMachine().getDetails().getPid() + " ]");
				esm.getMachine().getGridServiceAgents().waitForAtLeastOne(30, TimeUnit.SECONDS);
				esm.kill();
			} catch (final Exception e) {
				LogUtils.log("Failed to kill ESM", e);
			}
		}

		for (final GridServiceManager gsm : admin.getGridServiceManagers()) {
			try {
                log("killing GSM [ID:" + gsm.getAgentId() + "] [PID: " + gsm.getVirtualMachine().getDetails().getPid() + " ]");
				gsm.getMachine().getGridServiceAgents().waitForAtLeastOne(30, TimeUnit.SECONDS);
				gsm.kill();
			} catch (final Exception e) {
				LogUtils.log("Failed to kill GSM", e);
			}
		}

		for (final GridServiceContainer gsc : admin.getGridServiceContainers()) {
			try {
                log("killing GSC [ID:" + gsc.getAgentId() + "] [PID: " + gsc.getVirtualMachine().getDetails().getPid() + " ]");
				gsc.getMachine().getGridServiceAgents().waitForAtLeastOne(30, TimeUnit.SECONDS);
				gsc.kill();
			} catch (final Exception e) {
				LogUtils.log("Failed to kill GSC on machine " + gsc.getMachine().getHostAddress(), e);
			}
		}

        if(System.getProperty("suiteType").contains("XAP")){
            if (gsaStartupPIDs != null) {
                setupCleanEnvironment(admin);
            }
        }
	}

	private static void setupCleanEnvironment(final Admin admin) {

		final Map<String, Set<String>> currentGSAProcessesId = getProcessesIDsByCommand(admin, LIST_GSA_PROCESSES_CMD);

		log("current GSA Processes Id amount = " + currentGSAProcessesId.size());
		log("GSA start up PID amount = " + gsaStartupPIDs.size());
		if (currentGSAProcessesId.size() < gsaStartupPIDs.size()) {
			recoverBrokenEnvironmnet(admin, currentGSAProcessesId);
		}

		final Map<String, Set<String>> endProcessesIDs = getCurrentProcessesIDs(admin);

		final Map<String, Set<String>> gsaAndLusProcessesId = new HashMap<String, Set<String>>(gsaStartupPIDs);
		final Map<String, Set<String>> lusProcessesId = getProcessesIDsByCommand(admin, LIST_LUS_PROCESSES_CMD);
		for (final Entry<String, Set<String>> entry : lusProcessesId.entrySet()) {
			if (gsaAndLusProcessesId.containsKey(entry.getKey())) {
				gsaAndLusProcessesId.get(entry.getKey()).addAll(entry.getValue());
			} else {
				gsaAndLusProcessesId.put(entry.getKey(), entry.getValue());
			}
		}

		final Map<String, Set<String>> deltaProcessesIDs = getProcessesIDsDelta(gsaAndLusProcessesId, endProcessesIDs);
		killProcessesByIDs(deltaProcessesIDs);
	}

	private static void recoverBrokenEnvironmnet(final Admin admin, final Map<String, Set<String>> currentGSAPIDs) {

		for (final String hostAddress : gsaStartupPIDs.keySet()) {

			if (currentGSAPIDs.containsKey(hostAddress)) {
				continue;
			}

			log("Expected finding a GSA on " + hostAddress + ". starting agent.");

			// clear old PIDs
			gsaStartupPIDs.get(hostAddress).clear();

			String command = "killall -9 java;"; // kill leftovers
			command += "LOOKUPGROUPS=" + admin.getGroups()[0] + ";";
			command += "export LOOKUPGROUPS;";
			command += ScriptUtils.getBuildBinPath() + "/gs-agent.sh gsa.global.lus 2 gsa.global.gsm 0 gsa.gsc 0";

			log("running '" + command + "' on " + hostAddress);
			SSHUtils.runCommand(hostAddress, SSH_TIMEOUT, command, USERNAME, PASSWORD);

		}

		log("Waiting for 4 GSAs");
		assertTrue(admin.getGridServiceAgents().waitFor(4, 120, TimeUnit.SECONDS));

		for (final String hostAddress : gsaStartupPIDs.keySet()) {
			if (gsaStartupPIDs.get(hostAddress).isEmpty()) {

				log("Rebuilding PID list for GSAs on" + hostAddress);

				// get new PIDs

				log("running '" + LIST_GSA_PROCESSES_CMD + "' on " + hostAddress);

				final String output = SSHUtils.runCommand(hostAddress, SSH_TIMEOUT, LIST_GSA_PROCESSES_CMD, USERNAME, PASSWORD);
				final String[] lines = output.split("\n");

				log("Finished executing command. updating list");

				for (final String pid : lines) {
					log("adding: " + pid);
					gsaStartupPIDs.get(hostAddress).add(pid);
				}
			}
		}
	}

	public static void fetchStartProcessesIDs(final Admin admin) {
		gsaStartupPIDs = getProcessesIDsByCommand(admin, LIST_GSA_PROCESSES_CMD);
	}

	private static Map<String, Set<String>> getCurrentProcessesIDs(final Admin admin) {
		return getProcessesIDsByCommand(admin, LIST_JAVA_PROCESSES_CMD);
	}

	private static Map<String, Set<String>> getProcessesIDsByCommand(final Admin admin, final String command) {

		final Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		final String numberOfMachines = System.getProperty(GSA_MACHINES_COUNT_PROP);
		int numberOfMachinesToWaitFor;

		if (numberOfMachines != null) {
			numberOfMachinesToWaitFor = Integer.parseInt(numberOfMachines);
		}
		else {
			numberOfMachinesToWaitFor = 4;
		}

		if (gsaStartupPIDs == null) {
			Assert.assertTrue(admin.getGridServiceAgents().waitFor(numberOfMachinesToWaitFor, 120, TimeUnit.SECONDS));
		} else {
			admin.getGridServiceAgents().waitFor(numberOfMachinesToWaitFor, 10, TimeUnit.SECONDS);
		}

		for (final GridServiceAgent gsa : admin.getGridServiceAgents()) {
			final String host = gsa.getMachine().getHostAddress();
			if (host.startsWith(LINUX_HOST_PREFIX)) {
				result.put(host, new HashSet<String>());

				final String output = SSHUtils.runCommand(host, SSH_TIMEOUT, command, USERNAME, PASSWORD);
				final String[] lines = output.split("\n");

				for (final String pid : lines) {
					result.get(host).add(pid);
				}
			}
		}
		return result;
	}

	private static Map<String, Set<String>> getProcessesIDsDelta(final Map<String, Set<String>> startProcessesIDs, final Map<String, Set<String>> endProcessesIDs) {

		final Map<String, Set<String>> result = new HashMap<String, Set<String>>();

		for (final String host : endProcessesIDs.keySet()) {
			result.put(host, new HashSet<String>());
			for (final String pid : endProcessesIDs.get(host)) {
				if (!startProcessesIDs.get(host).contains(pid)) {
					result.get(host).add(pid);
				}
			}
		}

		return result;
	}

    public static Set<String> getClientProcessesIDsDelta(final Set<String> startProcessesIDs, final Set<String> endProcessesIDs) {
        final Set<String> result = new HashSet<String>();
        for (final String pid : endProcessesIDs) {
            if (!startProcessesIDs.contains(pid)) {
                result.add(pid);
            }
        }
        return result;
    }

	private static void killProcessesByIDs(final Map<String, Set<String>> processesIDs) {
		for (final String host : processesIDs.keySet()) {
			if (processesIDs.get(host).isEmpty()) {
				continue;
			}

			log("Remaining java processes found on " + host + ". Killing remaining processes");

			String command = "kill -9 ";
			for (final String pid : processesIDs.get(host)) {
				command += pid + " ";
			}
			log("running '" + command + "' on " + host);
			SSHUtils.runCommand(host, SSH_TIMEOUT, command, USERNAME, PASSWORD);
			try {
				Thread.sleep(5000);
			} catch (final Exception e) {
				// ignore
			}

		}
	}

}
