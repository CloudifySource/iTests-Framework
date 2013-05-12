package iTests.framework.utils;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.events.SpaceModeChangedEvent;
import org.openspaces.admin.space.events.SpaceModeChangedEventListener;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.Assert;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static iTests.framework.utils.AssertUtils.sleep;

public class ProcessingUnitUtils {

    public static final long OPERATION_TIMEOUT = 5 * 60 * 1000;

    public static void waitForActiveElection(ProcessingUnit processingUnit) {
		waitForActiveElection(processingUnit,Integer.MAX_VALUE,TimeUnit.SECONDS);
	}
	
    /**
     * waits for active election is over
     */
    public static boolean waitForActiveElection(ProcessingUnit processingUnit,int timeout, TimeUnit units) {
        for (ProcessingUnitInstance puInstance : processingUnit.getInstances()) {
            final CountDownLatch latch = new CountDownLatch(1);
            puInstance.waitForSpaceInstance();
            SpaceMode currentMode = puInstance.getSpaceInstance().getMode();
            if (currentMode.equals(SpaceMode.NONE)) {
                SpaceModeChangedEventListener listener = new SpaceModeChangedEventListener() {
                    public void spaceModeChanged(SpaceModeChangedEvent event) {
                        if (!event.getNewMode().equals(SpaceMode.NONE)) {
                            latch.countDown();
                        }
                    }
                };
                puInstance.getSpaceInstance().getSpaceModeChanged().add(listener);
                try {
                	//one last check before we go into await
                	if (SpaceMode.NONE.equals(puInstance.getSpaceInstance().getMode()) 
                			&& !latch.await(timeout,units)) {
                    	return false;
                    }
                } catch (InterruptedException e) {
                    //ignore
                } finally {
                    puInstance.getSpaceInstance().getSpaceModeChanged().remove(listener);
                }
            }
        }
		return true;
    }


    /**
     * waits 1 minute until deployment status changes to the expected
     * @deprecated - use {@link #waitForDeploymentStatus(org.openspaces.admin.pu.ProcessingUnit processingUnit, org.openspaces.admin.pu.DeploymentStatus expected, long timeout, java.util.concurrent.TimeUnit timeUnit)} instead.
     */
    @Deprecated()
    public static DeploymentStatus waitForDeploymentStatus(ProcessingUnit processingUnit, DeploymentStatus expected) {
        int retries = 60;
        while (retries-- > 0 && !expected.equals(processingUnit.getStatus())) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        return processingUnit.getStatus();
    }
    
    /**
     * waits until deployment status changes to the expected
     */
    public static DeploymentStatus waitForDeploymentStatus(ProcessingUnit processingUnit, DeploymentStatus expected, long timeout, TimeUnit timeUnit) {
        long retries = timeUnit.toSeconds(timeout);
        while (retries-- > 0 && !expected.equals(processingUnit.getStatus())) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                Assert.fail("interrupted",e);
            }
        }

        return processingUnit.getStatus();
    }

    /**
     * waits 1 minute until processing unit is managed by the provided GSM
     */
    public static GridServiceManager waitForManaged(ProcessingUnit processingUnit, GridServiceManager gridServiceManager) {
        int retries = 60;
        while (retries-- > 0 && !gridServiceManager.equals(processingUnit.getManagingGridServiceManager())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        return processingUnit.getManagingGridServiceManager();
    }

    /**
     * waits 1 minute until processing unit is managed by the provided GSM
     */
    public static void repetitiveAssertManagingGsm(final ProcessingUnit processingUnit, final GridServiceManager gridServiceManager) {
    	final long OPERATION_TIMEOUT = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
    	long start = System.currentTimeMillis();
    	AssertUtils.repetitiveAssertTrue("Expected Processing Unit managing GSM to be " + ToStringUtils.gsmToStringWithUidOrNull(gridServiceManager),
                new RepetitiveConditionProvider() {

                    @Override
                    public boolean getCondition() {
                        boolean condition = gridServiceManager.equals(processingUnit.getManagingGridServiceManager());
                        if (!condition) {
                            LogUtils.log("Waiting for Processing Unit managing GSM to be " + ToStringUtils.gsmToStringWithUidOrNull(gridServiceManager)
                                    + ". Currently it is " + ToStringUtils.gsmToStringWithUidOrNull(processingUnit.getManagingGridServiceManager()));
                        } else {
                            LogUtils.log("Processing Unit managing GSM is " + ToStringUtils.gsmToStringWithUidOrNull(gridServiceManager));
                        }
                        return condition;
                    }
                }, OPERATION_TIMEOUT);
    	long elapsedTimeMilliseconds = System.currentTimeMillis() - start;
        Assert.assertTrue(elapsedTimeMilliseconds <= TimeUnit.MINUTES.toMillis(1),"GSM " + ToStringUtils.gsmToStringWithUidOrNull(gridServiceManager) + " is managing pu " + processingUnit.getName() + ", however this took more than one minute: " + elapsedTimeMilliseconds + "ms");
    }

    public static Set<Machine> getMachinesFromPu(final ProcessingUnit pu) {

        Set<Machine> machines = new HashSet<Machine>();
        for (ProcessingUnitInstance groovy2Instance : pu.getInstances()) {
            machines.add(groovy2Instance.getMachine());
        }

        return machines;

    }

    /**
     * waits 1 minute until processing unit has a backup GSM
     */
    public static GridServiceManager waitForBackupGsm(ProcessingUnit processingUnit, GridServiceManager gridServiceManager) {
        int retries = 60;
        while (retries-- > 0 && !gridServiceManager.equals(processingUnit.getBackupGridServiceManager(gridServiceManager.getUid()))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        return processingUnit.getManagingGridServiceManager();
    }

    /**
     * waits 1 minute until processing unit has a backup GSM
     */
    public static void repetitiveAssertBackupGsm(final ProcessingUnit processingUnit, final GridServiceManager gridServiceManager) {
    	long timeoutMilliseconds = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES); 
    	AssertUtils.repetitiveAssertTrue("Expected Processing Unit backup GSM to be " + ToStringUtils.gsmToStringWithUidOrNull(gridServiceManager)
                + " while it is " + ToStringUtils.gsmToStringWithUidOrNull(processingUnit.getManagingGridServiceManager()),
                new RepetitiveConditionProvider() {

                    @Override
                    public boolean getCondition() {
                        boolean condition = gridServiceManager.equals(processingUnit.getBackupGridServiceManager(gridServiceManager.getUid()));
                        if (!condition) {
                            List<String> backupGsmIds = new ArrayList<String>();
                            for (GridServiceManager backupGsm : processingUnit.getBackupGridServiceManagers()) {
                                String backupGsmId = backupGsm.getUid();
                                if (backupGsm.getVirtualMachine() != null && backupGsm.getMachine() != null) {
                                    backupGsmId = "gsm pid[" + backupGsm.getVirtualMachine().getDetails().getPid() + "] host [" + backupGsm.getMachine().getHostName() + "/" + backupGsm.getMachine().getHostAddress() + "]";
                                }
                                backupGsmIds.add(backupGsmId);
                            }
                            LogUtils.log("Waiting for Processing Unit backup GSM to be " + ToStringUtils.gsmToStringWithUidOrNull(gridServiceManager) + " current backup gsms=" + backupGsmIds);
                        } else {
                            LogUtils.log("Processing Unit backup GSM is " + ToStringUtils.gsmToStringWithUidOrNull(gridServiceManager));
                        }
                        return condition;
                    }
                }, timeoutMilliseconds);
    }
    
    /**
     * Return the name representing each Processing Unit (as shown in the UI).
     */
    public static String getProcessingUnitInstanceName(ProcessingUnitInstance[] pus) {
        int index = 0;
        String[] strings = new String[pus.length];
        for (ProcessingUnitInstance pu : pus) {
            String name = ProcessingUnitUtils.getProcessingUnitInstanceName(pu);
            strings[index++] = name;
        }
        return Arrays.toString(strings);
    }

    /**
     * Return the name representing this Processing Unit (as shown in the UI).
     */
    public static String getProcessingUnitInstanceName(ProcessingUnitInstance pu) {
        String name = "null";
        ClusterInfo clusterInfo = pu.getClusterInfo();
        if (clusterInfo != null) {
            name = clusterInfo.getName();
            Integer id = clusterInfo.getInstanceId();
            if (clusterInfo.getNumberOfBackups() > 0) {
                Integer bid = clusterInfo.getBackupId();
                if (bid == null) {
                    bid = Integer.valueOf(0);
                }
                name += "." + id + " [" + (bid + 1) + "]";
            } else {
                name += " [" + id + "]";
            }
        }
        return name;
    }
    
	public static URL getWebProcessingUnitURL(ProcessingUnit pu) {
		ProcessingUnitInstance[] instances = pu.getInstances();
		if (instances.length == 0) {
			return null;
		}
		ProcessingUnitInstance pui = instances[0];
		Map<String, ServiceDetails> alldetails = pui
		.getServiceDetailsByServiceId();

		ServiceDetails details = alldetails.get("jee-container");
		String host = details.getAttributes().get("host").toString();
		String port = details.getAttributes().get("port").toString();
		String ctx = details.getAttributes().get("context-path").toString();
		String url = "http://" + host + ":" + port + ctx;
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			// this is a bug since we formed the URL correctly
			throw new IllegalStateException(e);
		}
	}

    public static URL getWebProcessingUnitURL(ProcessingUnit pu, boolean isSecured) {

        String url;

        ProcessingUnitInstance pui = pu.getInstances()[0];
        Map<String, ServiceDetails> alldetails = pui
                .getServiceDetailsByServiceId();

        ServiceDetails details = alldetails.get("jee-container");
        String host = details.getAttributes().get("host").toString();
        String port = details.getAttributes().get("port").toString();
        String ctx = details.getAttributes().get("context-path").toString();
        if(isSecured){
            url = "https://" + host + ":" + port + ctx;
        }
        else{
            url = "http://" + host + ":" + port + ctx;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            // this is a bug since we formed the URL correctly
            throw new IllegalStateException(e);
        }
    }
	
	public static int getNumberOfSpaceInstances(ProcessingUnit pu, SpaceMode mode) {
		ProcessingUnitInstance[] instances = pu.getInstances();
		LogUtils.log("Number Of Space Instances is : " + instances.length);
		int count = 0;
		for (ProcessingUnitInstance inst : instances) {
			SpaceInstance spaceInstance = inst.waitForSpaceInstance(2, TimeUnit.MINUTES);
			AssertUtils.assertNotNull("runtime getSpaceInstance() is null", spaceInstance);
			SpaceMode instMode = spaceInstance.getMode();
			LogUtils.log("Mode for space " + inst.getProcessingUnitInstanceName() + " is :" + instMode);
			if (mode.equals(instMode)) count++;
		}
		return count;
	}
	
	public static void repetitiveAssertExactNumberOfInstances(final ProcessingUnit processingUnit, final int expectedNumberOfInstances, long timeout, TimeUnit timeunit) {
		 
    	AssertUtils.repetitiveAssertTrue("Expected exactly " + expectedNumberOfInstances + " " + processingUnit.getName() + " instances",
                new RepetitiveConditionProvider() {

                    @Override
                    public boolean getCondition() {
                        int actualNumberOfInstances = processingUnit.getInstances().length;
                        boolean condition = actualNumberOfInstances == expectedNumberOfInstances;
                        if (!condition) {
                            LogUtils.log("Waiting for " + expectedNumberOfInstances + " " + processingUnit.getName() + " instances. Currently " + actualNumberOfInstances + " instances");
                        } else {
                            LogUtils.log("Found " + expectedNumberOfInstances + " " + processingUnit.getName() + " instances");
                        }
                        return condition;
                    }
                }, timeunit.toMillis(timeout));
	}

    public static Set<Machine> getMachinesOfApplication(final Admin admin , final String applicationName) {
        Set<Machine> machines = new HashSet<Machine>();
        Application app = admin.getApplications().waitFor(applicationName, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        for (ProcessingUnit pu : app.getProcessingUnits()) {
            for (ProcessingUnitInstance puInstance : pu.getInstances()) {
                machines.add(puInstance.getMachine());
            }

        }
        return machines;
    }

    public static Set<Machine> getMachinesOfService(final Admin admin, final String serviceName) {
        LogUtils.log("Retreiving machines for processing unit " + serviceName);
        Set<Machine> machines = new HashSet<Machine>();
        ProcessingUnit pu = admin.getProcessingUnits().waitFor(serviceName, OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        for (ProcessingUnitInstance puInstance : pu.getInstances()) {
            machines.add(puInstance.getMachine());
        }
        return machines;
    }
	
}
