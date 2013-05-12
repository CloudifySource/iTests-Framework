package iTests.framework.utils;

import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.gsa.events.GridServiceAgentRemovedEventListener;
import org.openspaces.admin.internal.admin.InternalAdmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class GridServiceAgentsCounter implements GridServiceAgentAddedEventListener, GridServiceAgentRemovedEventListener {

	Admin admin;
	private final Queue<GridServiceAgent> added = new ConcurrentLinkedQueue<GridServiceAgent>();
	private final Queue<GridServiceAgent> removed = new ConcurrentLinkedQueue<GridServiceAgent>();
	
    
	
	public GridServiceAgentsCounter(final Admin admin) {
		final CountDownLatch addedLatch = new CountDownLatch(1);
		((InternalAdmin)admin).scheduleNonBlockingStateChange(new Runnable() {
	    	//TODO: Fix all openspaces eventmanagers to schedule at the correct thread, and use it also to send events when include=true. 
	    	public void run() {
	    		admin.getGridServiceAgents().getGridServiceAgentAdded().add(GridServiceAgentsCounter.this, true);
	            admin.getGridServiceAgents().getGridServiceAgentRemoved().add(GridServiceAgentsCounter.this);
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
        admin.getGridServiceAgents().getGridServiceAgentAdded().remove(this);
        admin.getGridServiceAgents().getGridServiceAgentRemoved().remove(this);
    }
    
    public int getNumberOfGSAsAdded() {
    	return added.size();
    }
    
    public int getNumberOfGSAsRemoved() {
    	return removed.size();
    }

	public void gridServiceAgentAdded(
			GridServiceAgent agent) {
		added.add(agent);
		
	}

	public void gridServiceAgentRemoved(
			GridServiceAgent agent) {
		removed.add(agent);
	}
	
	public void repetitiveAssertNumberOfGridServiceAgentsAdded(final int expected, long timeoutMilliseconds) {
		if (added.size() > expected) {
			AssertUtils.assertFail("Expected " + expected + " GSAs Added. actual " + added.size() + " : " + ToStringUtils.gsasToString(added));
		}
		AssertUtils.repetitiveAssertTrue("Expected " + expected + " GSAs Added.", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                List<GridServiceAgent> copy = new ArrayList<GridServiceAgent>(added);
                int actual = copy.size();
                boolean condition = expected == actual;
                if (!condition) {
                    LogUtils.log("Expected " + expected + " GSAs Added. actual " + actual + " : " + ToStringUtils.gsasToString(copy));
                }
                return condition;
            }
        },
                timeoutMilliseconds);
	}
	
	public void repetitiveAssertNumberOfGridServiceAgentsRemoved(final int expected, long timeoutMilliseconds) {
		if (removed.size() > expected) {
			AssertUtils.assertFail("Expected " + expected + " GSAs Removed. actual " + ToStringUtils.gsasToString(removed));
		}
		AssertUtils.repetitiveAssertTrue("Expected " + expected + " GSAs Removed.", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                List<GridServiceAgent> copy = new ArrayList<GridServiceAgent>(removed);
                int actual = copy.size();
                boolean condition = expected == actual;
                if (!condition) {
                    LogUtils.log("Expected " + expected + " GSAs Removed. actual " + actual + " : " + ToStringUtils.gsasToString(copy));
                }
                return condition;
            }
        },
                timeoutMilliseconds);
	}
	
	public void repetitiveAssertGridServiceAgentRemoved(final GridServiceAgent agent, long timeoutMilliseconds) {
		AssertUtils.repetitiveAssertTrue("Expected " + ToStringUtils.gsaToString(agent) + " to be removed.", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                boolean condition = removed.contains(agent);
                if (!condition) {
                    LogUtils.log("Expected " + ToStringUtils.gsaToString(agent) + " to be removed.");
                }
                return condition;
            }
        },
                timeoutMilliseconds);
	}

	public void repetitiveAssertNumberOfGSAsHolds(final int expectedAdded,
			final int expectedRemoved, final long timeoutMilliseconds) {
		
		if (removed.size() > expectedRemoved) {
			AssertUtils.assertFail("Expected " + expectedRemoved + " GSAs Removed. actual " + ToStringUtils.gsasToString(removed));
		}
		
		if (added.size() > expectedAdded) {
			AssertUtils.assertFail("Expected " + expectedAdded + " GSAs Added. actual " + ToStringUtils.gsasToString(added));
		}
		
		AssertUtils.repetitiveAssertConditionHolds("Expected " + expectedAdded + " GSAs Added and " + expectedRemoved + " GSAs Removed.", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                boolean condition = true;
                List<GridServiceAgent> removedCopy = new ArrayList<GridServiceAgent>(removed);
                int actualRemoved = removedCopy.size();
                if (expectedRemoved != actualRemoved) {
                    condition = false;
                    LogUtils.log("Expected " + expectedRemoved + " GSAs Removed. actual " + actualRemoved + " : " + ToStringUtils.gsasToString(removedCopy));
                }

                List<GridServiceAgent> addedCopy = new ArrayList<GridServiceAgent>(added);
                int actualAdded = addedCopy.size();
                if (expectedAdded != actualAdded) {
                    condition = false;
                    LogUtils.log("Expected " + expectedAdded + " GSAs Removed. actual " + actualAdded + " : " + ToStringUtils.gsasToString(addedCopy));
                }

                return condition;
            }
        },
                timeoutMilliseconds, timeoutMilliseconds);
	}
}
