package iTests.framework.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openspaces.admin.AgentGridComponent;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.VirtualMachineDetails;
import org.openspaces.admin.zone.Zone;

/**
 * 
 * @author evgenyf
 * @since 9.7
 */
public class WebUIAdminUtils {
	
	public static String createGridServiceName( AgentGridComponent gridComponent ) {
		
		int agentID = gridComponent.getAgentId();
		VirtualMachine virtualMachine = gridComponent.getVirtualMachine();
		VirtualMachineDetails vmDetails = virtualMachine.getDetails();
		long pid = vmDetails.getPid();
		Map<String, Zone> zones = gridComponent.getZones();
		
		String gridServiceName = getGridServiceName( gridComponent ); 
		String name1 = agentID < 0 ? gridServiceName : gridServiceName + "-" + agentID;

		String zonesRepresentation = getZonesPresentation( zones );
		if( zonesRepresentation.length() > 0 )
		{
			zonesRepresentation = "[" + zonesRepresentation + "]";
		}

		String name2 = pid  < 0 ? name1 : name1 + "[" + pid + "]" + zonesRepresentation;

		return name2;
	}
	
    public static String getGridServiceName( AgentGridComponent gridComponent ){
    	
    	String retValue = null;
    	
    	if( gridComponent instanceof GridServiceAgent ){
			retValue = "gsa";
    	}
    	else if( gridComponent instanceof GridServiceManager ){
    		retValue = "gsm";
    	}
    	else if( gridComponent instanceof GridServiceContainer ){
    		retValue = "gsc";
    	}	
    	else if( gridComponent instanceof ElasticServiceManager ){
    		retValue = "esm";    		
    	}
    	else if( gridComponent instanceof LookupService ){
    		retValue = "lus";    		
    	}
    	
    	return retValue == null ? "n/a" : retValue;
    }
    
    public static String getZonesPresentation( Map<String, Zone> zones )
    {
        String retValue = "";
        if( zones != null )
        {
            Set<String> keySet = zones.keySet();
            Iterator<String> iterator = keySet.iterator();
            while( iterator.hasNext() )
            {
                retValue += iterator.next() + ",";
            }
            
            if( retValue.length() > 0 ){
            	retValue = retValue.substring( 0, retValue.length() - 1 );
            }
        }
        
        return retValue; 
    }    
}
