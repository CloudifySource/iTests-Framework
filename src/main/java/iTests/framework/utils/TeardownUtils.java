package iTests.framework.utils;

import com.gigaspaces.grid.gsa.AgentProcessDetails;
import com.gigaspaces.grid.gsa.AgentProcessesDetails;
import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.esm.ElasticServiceManagers;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static iTests.framework.utils.LogUtils.log;

/**
 * Utility methods for test teardown.
 * 
 * @author Moran Avigdor
 */
public class TeardownUtils {

    public static void teardownAll(Admin admin) {
        teardownAll(admin, false);
    }
    
    public static void teardownAll(Admin admin, boolean skipLusCleanup) {

        if (admin == null) {
            log("> no snapshot. admin is null");
            return;
        }

        snapshot(admin);

        //undeploy all excess processing units (should invoke orderly shutdown)
        for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
        	try {
        		boolean completed = processingUnit.undeployAndWait(30, TimeUnit.MILLISECONDS);
        		log("undeploying processing unit [" + processingUnit.getName() +"] completed? " + completed);
        	} catch (Exception e) {
        		log("undeploy of processing unit [" + processingUnit.getName() +"] failed - " + e, e);
        	}
        }
        
        Map<GridServiceAgent, List<Integer>> processKilledByAgent = new HashMap<GridServiceAgent, List<Integer>>();
        
        // kill all ESMs first
        for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
            ElasticServiceManagers elasticServiceManagers = gsa.getMachine()
                    .getElasticServiceManagers();
            for (ElasticServiceManager esm : elasticServiceManagers) {
                try {
                    log("killing ESM [ID:" + esm.getAgentId() + "] [PID: " + esm.getVirtualMachine().getDetails().getPid() + "]");
                    esm.kill();
                    
                    addKilledProcess(processKilledByAgent, gsa, esm.getAgentId());
                } catch (Exception e) {
                    log("ESM kill failed - " + e, e);
                }
            }
        }
        
        // kill all GSMs 
        for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
            GridServiceManagers gridServiceManagers = gsa.getMachine()
                    .getGridServiceManagers();
            for (GridServiceManager gsm : gridServiceManagers) {
                try {
                    log("killing GSM [ID:" + gsm.getAgentId() + "] [PID: " + gsm.getVirtualMachine().getDetails().getPid() + "]");
                    gsm.kill();
                    
                    addKilledProcess(processKilledByAgent, gsa, gsm.getAgentId());
                } catch (Exception e) {
                    log("GSM kill failed - " + e, e);
                }
            }
        }

        // kill all GSCs
        for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
            GridServiceContainers gridServiceContainers = gsa.getMachine()
                    .getGridServiceContainers();
            for (GridServiceContainer gsc : gridServiceContainers) {
                try {
                    log("killing GSC [ID:" + gsc.getAgentId() + "] [PID: " + gsc.getVirtualMachine().getDetails().getPid() + "]");
                    gsc.kill();
                    
                    addKilledProcess(processKilledByAgent, gsa, gsc.getAgentId());
                } catch (Exception e) {
                    log("GSC kill failed - " + e, e);
                }
            }
        }

        //kill processes that may have loaded during test (hsql, etc.).
        for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
            try {
                AgentProcessesDetails processesDetails = gsa.getProcessesDetails();
                for (AgentProcessDetails processDetails : processesDetails.getProcessDetails()) {
                    if ("lus".equals(processDetails.getServiceType())) {
                        Integer requiredGlobalInstances = processesDetails.getRequiredGlobalInstances().get("lus");
                        if (skipLusCleanup) {
                            continue; // skip it - test is managing private luses that interfere with this cleanup
                        }
                        if (requiredGlobalInstances == null || requiredGlobalInstances == 0) {
                            continue; //skip it - running **locally** without global instances
                        }
                        boolean shouldKillLookupService = false;
                        if (admin.getLookupServices().getSize() > requiredGlobalInstances) {
                            int countDiscovered = 0;
                            for (LookupService lus : admin.getLookupServices()) {
                                if (lus.isDiscovered() && lus.getAgentId() > 0) {
                                    countDiscovered++;
                                }
                            }
                            if (countDiscovered > requiredGlobalInstances) {
                                log ("Too many lookup services, expected only ["+requiredGlobalInstances+"] but discovered: ["+countDiscovered+"]");
                                shouldKillLookupService = true;
                            }
                        }
                        if (!shouldKillLookupService) {
                            log("Not killing LUS [ID: "
                                    + processDetails.getAgentId() + "] [PID: "
                                    + processDetails.getProcessId()
                                    + "] managed by GSA [PID: "
                                    + gsa.getVirtualMachine().getDetails().getPid()
                                    + "]");
                            continue; //continue to next process
                        }
                    }


                    if ( isProcessAlreadyKilled(processKilledByAgent, gsa, processDetails) ) {
                        continue; //already killed
                    }

                    //otherwise - kill it!
                    log("killing [Type: " + processDetails.getServiceType().toUpperCase()
                            + "] [ID: " + processDetails.getAgentId()
                            + "] [PID: " + processDetails.getProcessId()
                            + "] managed by GSA [PID: "
                            + gsa.getVirtualMachine().getDetails().getPid()
                            + "]");
                    try {
                        gsa.killByAgentId(processDetails.getAgentId());
                    }catch(Exception e) {
                        log("Failed to kill [ID: "
                                + processDetails.getAgentId() + "] [PID: "
                                + processDetails.getProcessId() + "] - "
                                + e);
                    }

                }
            }catch(Exception e) {
                log("Failed to extract 'processesDetails' from GSA [PID: "+gsa.getVirtualMachine().getDetails().getPid() +"] - "+e,e);
            }
        }
    }

	private static boolean isProcessAlreadyKilled(
			Map<GridServiceAgent, List<Integer>> processKilledByAgent,
			GridServiceAgent gsa, AgentProcessDetails processDetails) {
		List<Integer> list = processKilledByAgent.get(gsa);
		return (list != null && list.contains(processDetails.getAgentId()));
	}

	private static void addKilledProcess(
			Map<GridServiceAgent, List<Integer>> processKilledByAgent,
			GridServiceAgent gsa, int agentId) {
		List<Integer> list = processKilledByAgent.get(gsa);
		if (list == null) {
			list = new ArrayList<Integer>();
		}
		list.add(agentId);
		processKilledByAgent.put(gsa, list);
	}

    public static void dumpLogs(Admin... admins) {
        for (Admin a : admins) {
            if (a != null) {
                try {
                    DumpUtils.dumpLogs(a);
                } catch (Throwable t) {
                    log("failed to dump logs", t);
                }
            }
        }
    }

    public static void teardownAll(Admin ... admins) {
        dumpLogs(admins);
        for(Admin admin : admins){
            teardownAll(admin);
        }
    }

    public static void snapshot(Admin admin) {
        if (admin == null) {
        	log("> no snapshot. admin is null");
        	return;
        }
        
    	log("> snapshot " + admin.getGroups()[0] +": ");
        for (Machine machine : admin.getMachines()) {
            log("Machine: " + machine.getHostName() + "/"
                    + machine.getHostAddress());
            for (LookupService lus : machine.getLookupServices()) {
                log("\t LUS [ID: " + lus.getAgentId() + "] [PID: "+lus.getVirtualMachine().getDetails().getPid() +"]"  + ToStringUtils.getZones(lus));
            }
            for (GridServiceAgent gsa : machine.getGridServiceAgents()) {
                log("\t GSA [" + gsa.getProcessesDetails() + "] [PID: "+gsa.getVirtualMachine().getDetails().getPid() +"]" + ToStringUtils.getZones(gsa));
            }
            for (GridServiceManager gsm : machine.getGridServiceManagers()) {
                log("\t GSM [ID: " + gsm.getAgentId() + "] [PID: "+gsm.getVirtualMachine().getDetails().getPid() +"]" + ToStringUtils.getZones(gsm));
            }
            for (ElasticServiceManager esm : machine.getElasticServiceManagers()) {
                log("\t ESM [ID: " + esm.getAgentId() + "] [PID: "+esm.getVirtualMachine().getDetails().getPid() +" ]"+ ToStringUtils.getZones(esm));
            }
            for (GridServiceContainer gsc : machine.getGridServiceContainers()) {
                log("\t " + ToStringUtils.gscToString(gsc));
                for (ProcessingUnitInstance puInstance : gsc
                        .getProcessingUnitInstances()) {
                    log("\t - " + ToStringUtils.puInstanceToString(puInstance));
                }
            }
            log("total PU instances: "
                    + machine.getProcessingUnitInstances().length);
            log("---");
        }
        log("\n");
        log("Processing Units:");
        for (ProcessingUnit pu : admin.getProcessingUnits()) {
        	log("\t PU " + pu.getName() + " status " + pu.getStatus().toString() + " actual instances:" + pu.getInstances().length + " , planned:" + pu.getTotalNumberOfInstances());
        }
        log("---\n");
    }

    public static void snapshot(Admin ... admins) {
        for(Admin admin : admins){
            snapshot(admin);
        }
    }
}
