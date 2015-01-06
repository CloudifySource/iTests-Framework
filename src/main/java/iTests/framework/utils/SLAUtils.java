package iTests.framework.utils;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitPartition;

import static iTests.framework.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;
import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNotSame;

/**
 * Utility methods for SLA assertions.
 * 
 * @author Moran Avigdor
 */
public class SLAUtils {
	
	/**
	 * assert that the max-per-machine SLA is obeyed
	 */
	public static void assertMaxPerMachine(Admin admin) {
        for (ProcessingUnit pu : admin.getProcessingUnits()) {
            for (ProcessingUnitPartition partition : pu.getPartitions()) {
				if (partition.getBackup() != null) {
					ProcessingUnitInstance primary = partition.getPrimary();
					ProcessingUnitInstance backup = partition.getBackup();

					assertNotNull("primary ref. null", primary);
					assertNotNull("backup ref. null", backup);
					assertNotSame("max-per-machine SLA constraint for "
							+ getProcessingUnitInstanceName(primary) + " and "
							+ getProcessingUnitInstanceName(backup), backup
							.getMachine(), primary.getMachine());
				}
            }
        }
    }
	
	/**
	 * assert that the max-per-VM SLA is obeyed
	 */
	public static void assertMaxPerVM(Admin admin) {
        for (ProcessingUnit pu : admin.getProcessingUnits()) {
            for (ProcessingUnitPartition partition : pu.getPartitions()) {
				if (partition.getBackup() != null) {
					ProcessingUnitInstance primary = partition.getPrimary();
					ProcessingUnitInstance backup = partition.getBackup();

					assertNotNull("primary ref. null", primary);
					assertNotNull("backup ref. null", backup);
					assertNotSame("max-per-VM SLA constraint for "
							+ getProcessingUnitInstanceName(primary) + " and "
							+ getProcessingUnitInstanceName(backup), backup
							.getGridServiceContainer(), primary
							.getGridServiceContainer());
				}
            }
        }
    }
	
	/**
	 * assert max per zone
	 */
	public static void assertMaxPerZone(Admin admin) {
		for (ProcessingUnit pu : admin.getProcessingUnits()) {
			for (ProcessingUnitPartition partition : pu.getPartitions()) {
				if (partition.getBackup() != null) {
					ProcessingUnitInstance primary = partition.getPrimary();
					ProcessingUnitInstance backup = partition.getBackup();

					assertNotNull("primary ref. null", primary);
					assertNotNull("backup ref. null", backup);
					assertNotSame("zone SLA constraint for "
							+ getProcessingUnitInstanceName(primary) + " and "
							+ getProcessingUnitInstanceName(backup), backup
							.getZones(), primary.getZones());
				}
			}
        }
    }

	public static void assertRequiresIsolation(Admin admin) {
		for (GridServiceContainer gsc : admin.getGridServiceContainers()) {
			assertTrue(gsc.getProcessingUnitInstances().length == 0 || gsc.getProcessingUnitInstances().length == 1);
		}
	}
}
