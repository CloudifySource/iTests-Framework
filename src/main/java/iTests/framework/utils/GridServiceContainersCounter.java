package iTests.framework.utils;

import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerAddedEventListener;
import org.openspaces.admin.gsc.events.GridServiceContainerRemovedEventListener;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.grid.gsm.containers.ContainersSlaUtils;
import org.testng.log4testng.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GridServiceContainersCounter implements GridServiceContainerAddedEventListener, GridServiceContainerRemovedEventListener {

	Logger logger = Logger.getLogger(this.getClass());
	private final Admin admin;
	private final Queue<GridServiceContainer> added = new ConcurrentLinkedQueue<GridServiceContainer>();
	private final Queue<GridServiceContainer> removed = new ConcurrentLinkedQueue<GridServiceContainer>();
	private final String zone;
    
    public GridServiceContainersCounter(final ProcessingUnit pu) {
    	this(pu.getAdmin(),ContainersSlaUtils.getContainerZone(pu));
    }
    public GridServiceContainersCounter(final Admin admin) {
    	this(admin,null);
    }
    
	public GridServiceContainersCounter(final Admin admin, final String zone) {
		this.zone = zone;
		final CountDownLatch addedLatch = new CountDownLatch(1);
	    ((InternalAdmin)admin).scheduleNonBlockingStateChange(new Runnable() {
	    	//TODO: Fix all openspaces eventmanagers to schedule at the correct thread, and use it also to send events when include=true. 
	    	public void run() {
				admin.getGridServiceContainers().getGridServiceContainerAdded().add(GridServiceContainersCounter.this, true);
		        admin.getGridServiceContainers().getGridServiceContainerRemoved().add(GridServiceContainersCounter.this);
		        addedLatch.countDown();
			}});
		
        this.admin = admin;
        try {
			addedLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while waiting for added scheduled runnable", e);
		}
    }
    
    public void close() {
        admin.getGridServiceContainers().getGridServiceContainerAdded().remove(this);
        admin.getGridServiceContainers().getGridServiceContainerRemoved().remove(this);
    }
    
    public int getNumberOfGSCsAdded() {
        return added.size();
    }
    
    public int getNumberOfGSCsRemoved() {
        return removed.size();
    }

	public void gridServiceContainerAdded(
			GridServiceContainer container) {
		if (isContainerMatchesZone(container)) {
			added.add(container);
		}
		
	}

	public void gridServiceContainerRemoved(
			GridServiceContainer container) {
		if (isContainerMatchesZone(container)) {
			removed.add(container);
		}
	}
	
	private boolean isContainerMatchesZone(GridServiceContainer container) {
		return zone == null || ContainersSlaUtils.isContainerMatchesZone(container, zone);
	}
	
	public void repetitiveAssertNumberOfGridServiceContainersAdded(final int expected, long timeoutMilliseconds) {
		if (added.size() > expected) {
			AssertUtils.assertFail("Expected " + expected + " GSCs Added. actual " + added.size() + " : " + ToStringUtils.gscsToString(added));
		}
		
		AssertUtils.repetitiveAssertTrue("Expected " + expected + " GSCs Added.", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                List<GridServiceContainer> copy = new ArrayList<GridServiceContainer>(added);
                int actual = copy.size();
                boolean condition = expected == actual;
                if (!condition) {
                    LogUtils.log("Expected " + expected + " GSCs Added. actual " + actual + " : " + ToStringUtils.gscsToString(copy));
                }
                return condition;
            }
        },
                timeoutMilliseconds);
		
		LogUtils.log("Found " + added.size() + " GSCs added");
	}
	
	
	
	public void repetitiveAssertNumberOfGridServiceContainersRemoved(final int expected, long timeoutMilliseconds) {
		
		repetitiveAssertNumberOfGridServiceContainersRemoved("", expected, timeoutMilliseconds);
		
	}
	
	public void repetitiveAssertNumberOfGridServiceContainersRemoved(
			final String messagePrefix, final int expected, long timeoutMilliseconds) {

		if (removed.size() > expected) {
			AssertUtils.assertFail(messagePrefix + "Expected " + expected + " GSCs Removed. actual " + removed.size() + " : " + ToStringUtils.gscsToString(removed));
		}
		
		AssertUtils.repetitiveAssertTrue(messagePrefix + "Expected " + expected + " GSCs Removed.", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                List<GridServiceContainer> copy = new ArrayList<GridServiceContainer>(removed);
                int actual = copy.size();
                boolean condition = expected == actual;
                if (!condition) {
                    LogUtils.log(messagePrefix + "Expected " + expected + " GSCs Removed. actual " + actual + " : " + ToStringUtils.gscsToString(copy));
                }
                return condition;
            }
        },
                timeoutMilliseconds);
		
		LogUtils.log("Found " + removed.size() + " GSCs removed");
		
	}
	public void repetitiveAssertNumberOfGridServiceContainersHolds(final int expectedAdded, final int expectedRemoved, long timeout, TimeUnit timeunit) {
		if (added.size() > expectedAdded) {
			AssertUtils.assertFail("Expected " + expectedAdded + " GSCs Added. actual " + added.size() + " : " + ToStringUtils.gscsToString(added));
		}
		
		if (removed.size() > expectedRemoved) {
			AssertUtils.assertFail("Expected " + expectedRemoved + " GSCs Removed . actual " + removed.size() + " : " + ToStringUtils.gscsToString(removed));
		}
		
		AssertUtils.repetitiveAssertConditionHolds("Expected " + expectedAdded + " GSCs Added and " + expectedRemoved + " GSCs Removed", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                boolean condition = true;

                List<GridServiceContainer> copyAdded = new ArrayList<GridServiceContainer>(added);
                int actualAdded = copyAdded.size();
                if (expectedAdded != actualAdded) {
                    condition = false;
                    LogUtils.log("Expected " + expectedAdded + " GSCs Added. actual " + actualAdded + " : " + ToStringUtils.gscsToString(copyAdded));
                }

                List<GridServiceContainer> copyRemoved = new ArrayList<GridServiceContainer>(removed);
                int actualRemoved = copyRemoved.size();
                if (expectedRemoved != actualRemoved) {
                    condition = false;
                    LogUtils.log("Expected " + expectedRemoved + " GSCs Removed. actual " + actualRemoved + " : " + ToStringUtils.gscsToString(copyRemoved));
                }

                return condition;
            }
        },
                timeunit.toMillis(timeout), timeout);
		
	}
	
}
