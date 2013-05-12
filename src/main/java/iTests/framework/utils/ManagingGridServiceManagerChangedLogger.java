package iTests.framework.utils;

import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.events.GridServiceManagerAddedEventListener;
import org.openspaces.admin.gsm.events.GridServiceManagerRemovedEventListener;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEvent;
import org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEventListener;
import org.openspaces.admin.pu.events.ManagingGridServiceManagerChangedEvent;
import org.openspaces.admin.pu.events.ManagingGridServiceManagerChangedEventListener;

import java.util.logging.Logger;

public class ManagingGridServiceManagerChangedLogger implements ManagingGridServiceManagerChangedEventListener, BackupGridServiceManagerChangedEventListener, GridServiceManagerAddedEventListener, GridServiceManagerRemovedEventListener {

    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final ProcessingUnit pu;
    
    public ManagingGridServiceManagerChangedLogger(ProcessingUnit pu) {
        this.pu = pu;
        pu.getBackupGridServiceManagerChanged().add(this);
        pu.getManagingGridServiceManagerChanged().add(this);
        pu.getAdmin().getGridServiceManagers().getGridServiceManagerAdded().add(this);
        pu.getAdmin().getGridServiceManagers().getGridServiceManagerRemoved().add(this);
        
    }
    @Override
    public void processingUnitBackupGridServiceManagerChanged(
            BackupGridServiceManagerChangedEvent event) {
        logger.info(event.getProcessingUnit().getName() + " backup GSM " + event.getType() + " " + event.getGridServiceManager().getUid());
    }

    @Override
    public void processingUnitManagingGridServiceManagerChanged(
            ManagingGridServiceManagerChangedEvent event) {
        String prevUid = event.getPreviousGridServiceManager() != null ? event.getPreviousGridServiceManager().getUid() : "null";
        String newUid = event.getNewGridServiceManager() != null ? event.getNewGridServiceManager().getUid() : "null";
        logger.info(event.getProcessingUnit().getName() + " managing GSM changed from " + prevUid + " to " + newUid);
    }
    
    @Override
    public void gridServiceManagerRemoved(GridServiceManager gridServiceManager) {
        logger.info("GSM " + gridServiceManager.getUid() + " removed.");
        
    }
    @Override
    public void gridServiceManagerAdded(GridServiceManager gridServiceManager) {
        logger.info("GSM " + gridServiceManager.getUid() + " added.");
    }
    
    public void close() {
        pu.getBackupGridServiceManagerChanged().remove(this);
        pu.getManagingGridServiceManagerChanged().remove(this);
        pu.getAdmin().getGridServiceManagers().getGridServiceManagerAdded().remove(this);
        pu.getAdmin().getGridServiceManagers().getGridServiceManagerRemoved().remove(this);
    }
    

}
