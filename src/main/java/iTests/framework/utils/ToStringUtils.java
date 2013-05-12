package iTests.framework.utils;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.zone.Zone;
import org.openspaces.admin.zone.ZoneAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static iTests.framework.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;

public class ToStringUtils {
	
	/** returns host/address */
	public static String machineToString(Machine machine) {
		return machine.getHostName()+"/"+machine.getHostAddress();
	}
	
	/** returns GSM-agentId[pid]@host <zones>*/
	public static String gsmToString(GridServiceManager gsm) {
		if (gsm == null) return "GSM is null" ;
		
		String toString = "GSM-"+gsm.getAgentId()+"["+gsm.getVirtualMachine().getDetails().getPid()+"]@" + gsm.getMachine().getHostName();
		Map<String, Zone> zones = gsm.getZones();
		if (!zones.isEmpty()) {
			toString += " " + zones.keySet();
		}
		return toString;
	}
	
	public static String gsmToStringWithUidOrNull(GridServiceManager gsm) {
    	String toString = ToStringUtils.gsmToString(gsm);
    	if (gsm != null) {
    		toString += " uid="+gsm.getUid();
    	}
    	return toString;
    }
	
	/** returns GSC-agentId[pid]@host <zones>*/
	public static String gscToString(GridServiceContainer gsc) {
		if (gsc == null) return "GSC is null" ;
		
		String toString = "GSC-"+gsc.getAgentId()+"["+gsc.getVirtualMachine().getDetails().getPid()+"]@" + gsc.getMachine().getHostName() + getZones(gsc);
        Map<String, Zone> zones = gsc.getZones();
        if (!zones.isEmpty()) {
            toString += " " + zones.keySet();
        }
        return toString;
	}
	
	/** returns GSA[pid]@host <zones>*/
	public static String gsaToString(GridServiceAgent gsa) {
		if (gsa == null) return "GSA is null" ;
		
		String toString = "GSA["+gsa.getVirtualMachine().getDetails().getPid()+"]@" + gsa.getMachine().getHostName()+ getZones(gsa);
		return toString;
	}
	
	public static String gscsToString(Iterable<GridServiceContainer> containers) {
		List<String> s = new ArrayList<String>();
		for (GridServiceContainer container : containers) {
			s.add(gscToString(container));
		}
		return s.toString();
	}
	

	public static String gsasToString(Iterable<GridServiceAgent> agents) {
		List<String> s = new ArrayList<String>();
		for (GridServiceAgent agent : agents) {
			s.add(gsaToString(agent));
		}
		return s.toString();
	}
	
	public static String gscSnapshotToString(GridServiceContainer gsc) {
		String out = gscToString(gsc);
        for (ProcessingUnitInstance puInstance : gsc
                .getProcessingUnitInstances()) {
        	out += "\t - " + puInstanceToString(puInstance);
        }
        return out;
	}

	public static String getZones(ZoneAware component) {
		final Map<String, Zone> zones = component.getZones();
		if (!zones.isEmpty()) {
			return " Zones:" + zones.keySet().toString();
		}
		return "";
	}
	
	/** returns PUname SpaceMode */
	public static String puInstanceToString(ProcessingUnitInstance pu) {
		if (pu == null) return "PU is null" ;
		
		String toString = getProcessingUnitInstanceName(pu);
		if (pu.getSpaceInstance()!=null)
			toString += " " + pu.getSpaceInstance().getMode();
		return toString;
	}
	
	/** returns presentation name of space - instance id and backup id like shown in UI*/
	public static String spaceInstanceToString(SpaceInstance spaceInstance) {
		if (spaceInstance == null) return "space instacne is null";
		String name = spaceInstance.getSpace().getName();
		Integer id = spaceInstance.getInstanceId();
		if (spaceInstance.getSpace().getNumberOfBackups() > 0) {
			Integer bid = spaceInstance.getBackupId();
			if (bid == null) {
				bid = Integer.valueOf(0);
			}
			name += "."+id+" ["+(bid+1)+"]";
		} else {
			name += " ["+id+"]";
		}
		
		return name;
	}

}
