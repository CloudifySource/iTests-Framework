package iTests.framework.utils;

import com.j_spaces.core.client.ReadModifiers;
import iTests.framework.resources.Person;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.esm.ElasticServiceManagers;
import org.openspaces.admin.esm.events.ElasticServiceManagerRemovedEventListener;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsc.events.GridServiceContainerRemovedEventListener;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.gsm.events.GridServiceManagerRemovedEventListener;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.lus.LookupServices;
import org.openspaces.admin.lus.events.LookupServiceAddedEventListener;
import org.openspaces.admin.lus.events.LookupServiceRemovedEventListener;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.machine.events.MachineAddedEventListener;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.grid.gsm.capacity.CapacityRequirementsPerAgent;
import org.openspaces.grid.gsm.capacity.DriveCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MachineCapacityRequirements;
import org.openspaces.grid.gsm.containers.ContainersSlaUtils;
import org.openspaces.grid.gsm.rebalancing.RebalancingUtils;
import org.testng.Assert;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static iTests.framework.utils.AssertUtils.sleep;


public class GsmTestUtils {

	public static void waitForScaleToComplete(final ProcessingUnit pu,
			final int expectedNumberOfContainers,
			final int expectedNumberOfMachines,
			long operationTimeout) {
		
		boolean validateCpuSla = true;
		waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, validateCpuSla, operationTimeout);
	}
	
	public static void waitForScaleToCompleteIgnoreCpuSla(ProcessingUnit pu,
			final int expectedNumberOfContainers,
			final int expectedNumberOfMachines,
			long operationTimeout) {
		
		boolean validateCpuSla = false;
		waitForScaleToComplete(pu, expectedNumberOfContainers, expectedNumberOfMachines, validateCpuSla, operationTimeout);
	}
	
	private static void waitForScaleToComplete(final ProcessingUnit pu,
			final int expectedNumberOfContainers,
			final int expectedNumberOfMachines,
			final boolean validateCpuSla,
			long durationInMilliseconds) {
		
		final RepetitiveConditionProvider condition =
		    newScaleConditionProvider(pu, expectedNumberOfContainers, expectedNumberOfMachines, validateCpuSla);
		AssertUtils.repetitiveAssertTrue("Waiting for scale to complete",
                condition, durationInMilliseconds);
		LogUtils.log("Done waiting. Repetitive assert is true");
	}

	public static void killContainer(final GridServiceContainer container) {
		if (container.isDiscovered()) {
			final CountDownLatch latch = new CountDownLatch(1);
			GridServiceContainerRemovedEventListener eventListener = new GridServiceContainerRemovedEventListener() {

				public void gridServiceContainerRemoved(
						GridServiceContainer gridServiceContainer) {
					if (gridServiceContainer.equals(container)) {
						latch.countDown();
					}
				}
			};
			GridServiceContainers containers = container.getAdmin()
					.getGridServiceContainers();
			containers.getGridServiceContainerRemoved().add(eventListener);
			try {
				LogUtils.log("Killing container " + container.getVirtualMachine().getDetails().getPid());
				container.kill();
				latch.await();
			} catch (InterruptedException e) {
				Assert.fail("Interrupted while killing container", e);
			} finally {
				containers.getGridServiceContainerRemoved().remove(eventListener);
			}
		}
	}
	
	public static void restartContainerAndWait(final GridServiceContainer container, boolean elastic, long timeout, TimeUnit timeunit) {
		if (container.isDiscovered()) {
			final CountDownLatch latch = new CountDownLatch(1);
			GridServiceContainerRemovedEventListener eventListener = new GridServiceContainerRemovedEventListener() {

				public void gridServiceContainerRemoved(
						GridServiceContainer gridServiceContainer) {
					if (gridServiceContainer.equals(container)) {
						latch.countDown();
					}
				}
			};
			GridServiceContainers containers = container.getAdmin()
					.getGridServiceContainers();
			containers.getGridServiceContainerRemoved().add(eventListener);
			try {
				restartContainer(container, elastic);
				Assert.assertTrue(latch.await(timeout,timeunit));
			} catch (InterruptedException e) {
				Assert.fail("Interrupted while killing container", e);
			} finally {
				containers.getGridServiceContainerRemoved().remove(eventListener);
			}
		}
	}

	public static void restartContainer(final GridServiceContainer container, boolean elastic) {
		if (elastic) {
			LogUtils.log("killing container " + container.getVirtualMachine().getDetails().getPid());
			container.kill(); // is restarted by ESM
		}
		else {
			LogUtils.log("restarting container " + container.getVirtualMachine().getDetails().getPid());
			container.restart();
		}
	
	}

	public static LookupService restartLookupService(final LookupService lus) {
		final CountDownLatch latch = new CountDownLatch(2);
		final AtomicReference<LookupService> newLus = new AtomicReference<LookupService>();
		LookupServiceRemovedEventListener removedEventListener = new LookupServiceRemovedEventListener() {

			public void lookupServiceRemoved(LookupService lookupService) {
				if (lookupService.equals(lus)) {
					latch.countDown();
				}
			}
		};
		
		LookupServiceAddedEventListener addedEventListener = new LookupServiceAddedEventListener() {

			public void lookupServiceAdded(LookupService lookupService) {
				newLus.set(lookupService);
				latch.countDown();
			}
		};

		LookupServices services = lus.getAdmin().getLookupServices();
		services.getLookupServiceRemoved().add(removedEventListener);
		services.getLookupServiceAdded().add(addedEventListener);
		try {
			LogUtils.log("restarting lus " + lus.getUid());
			lus.restart();
			latch.await();
		} catch (InterruptedException e) {
			Assert.fail("Interrupted while killing lus", e);
		} finally {
			services.getLookupServiceRemoved().remove(removedEventListener);
			services.getLookupServiceAdded().remove(addedEventListener);
		}
		return newLus.get();
	}

		public static void writeData(final ProcessingUnit pu, final int numToAdd) {
		writeData(pu, numToAdd, numToAdd);		
	}
	
	public static void writeData(final ProcessingUnit pu,
			final int numberOfObjectsToWrite, 
			final int totalObjectCountAfter) {
		
		// adding a thread that will dump threads every 1 minute
		// this is to debug deadlock or file system lock issues
		DumpUtils.dumpThreadsEveryMinute(pu.getAdmin(), new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                writeDataInternal(pu, numberOfObjectsToWrite, totalObjectCountAfter);
                return null;
            }
        });
	}
	
	private static void writeDataInternal(
			final ProcessingUnit pu,
			final int numberOfObjectsToWrite, 
			int totalObjectCountAfter) {
		
		final Space space = pu.getSpace();
		AssertUtils.assertNotNull("Failed getting space instance", space);
	    final GigaSpace gigaSpace = space.getGigaSpace();
        writeData(gigaSpace, numberOfObjectsToWrite, totalObjectCountAfter);
	}

	public static void writeData(final GigaSpace gigaSpace, final int numberOfObjectsToWrite, int totalObjectCountAfter) {
		final Person[] persons = new Person[numberOfObjectsToWrite];
        final int startInd = totalObjectCountAfter-numberOfObjectsToWrite;
        AssertUtils.assertEquals("Number of Person Pojos in space before write", startInd, countData(gigaSpace));
        for (int id = 0; id < numberOfObjectsToWrite; id++) {
            persons[id] = new Person(new Long(startInd+id));
        }
        LogUtils.log("Writing " + numberOfObjectsToWrite + " Person POJOs to space " + gigaSpace.getName());
        gigaSpace.writeMultiple(persons);        
        LogUtils.log("Wrote " + numberOfObjectsToWrite + " Person POJOs to space " + gigaSpace.getName());
        AssertUtils.assertEquals("Number of Person Pojos in space", totalObjectCountAfter, countData(gigaSpace));
	}
	
	/**
	 * Make sure the spaceproxy router introduced in v9.0.1 works like a charm
	 * count + dlb check by reading from space + increase logging level
	 */
	public static int countData(ProcessingUnit pu) {
		GigaSpace gigaSpace = pu.getSpace().getGigaSpace();
		return countData(gigaSpace);
	}

	private static int countData(GigaSpace gigaSpace) {
		final int count = gigaSpace.count(new Person(), ReadModifiers.READ_COMMITTED);
		final Person[] readData = GsmTestUtils.readData(gigaSpace);
		if (count != readData.length) {
			LogUtils.log("readmultiple=" + Arrays.toString(readData));
		}
		AssertUtils.assertEquals("Count person (" + count + ")!= readMultiple person(" + readData.length + ")",
                readData.length, count);
		return count;
	}

	public static Person[] readData(ProcessingUnit pu) {
		GigaSpace gigaSpace = pu.getSpace().getGigaSpace();
		return readData(gigaSpace);
	}

	private static Person[] readData(GigaSpace gigaSpace) {
		return gigaSpace.readMultiple(new Person(), ReadModifiers.READ_COMMITTED);
	}


	public static void waitForScaleToComplete(ProcessingUnit pu,
			int expectedNumberOfContainers, long operationTimeout) {
		boolean validateCpuSla = true;
		waitForScaleToComplete(pu,expectedNumberOfContainers, validateCpuSla, operationTimeout);
	}
	
	public static void waitForScaleToCompleteIgnoreCpuSla(ProcessingUnit pu,
			int expectedNumberOfContainers, long operationTimeout) {
		boolean validateCpuSla = false;
		waitForScaleToComplete(pu, expectedNumberOfContainers, validateCpuSla, operationTimeout);
	}
	
	private static void waitForScaleToComplete(ProcessingUnit pu,
			int expectedNumberOfContainers, boolean validateCpuSla, long operationTimeout) {
		waitForScaleToComplete(pu, expectedNumberOfContainers, 0, validateCpuSla, operationTimeout);
		
	}

    public static ElasticServiceManager restartElasticServiceManager(final ElasticServiceManager esm) {
    	
    	final GridServiceAgent agent = esm.getGridServiceAgent();
        killElasticServiceManager(esm);
        return AdminUtils.loadESM(agent);
    }

	public static void killElasticServiceManager(final ElasticServiceManager esm) {
		if (!esm.isDiscovered()) {
        	throw new IllegalStateException("Cannot restart esm " + esm.getUid() +" since it has been undiscovered");
        }

        final CountDownLatch latch = new CountDownLatch(1);
        
        final ElasticServiceManagerRemovedEventListener removedEventListener = new ElasticServiceManagerRemovedEventListener() {

            public void elasticServiceManagerRemoved(
                    final ElasticServiceManager elasticServiceManager) {
                if (elasticServiceManager.equals(esm)) {
                    latch.countDown();
                }
            }
        };
        final ElasticServiceManagers managers = esm.getAdmin().getElasticServiceManagers();
        managers.getElasticServiceManagerRemoved().add(removedEventListener);
        try {
            esm.kill();
            latch.await();
        } catch (final InterruptedException e) {
            Assert.fail("Interrupted while killing esm", e);
        } finally {
            managers.getElasticServiceManagerRemoved().remove(removedEventListener);
        }
	}
    
    public static GridServiceManager restartGridServiceManager(final GridServiceManager gsm) throws InterruptedException {
        if (!gsm.isDiscovered()) {
        	throw new IllegalStateException("Cannot restart gsm since it has been undiscovered");
        }

        GridServiceAgent agent = null;
        while (agent == null){ 
        	agent = gsm.getGridServiceAgent();
        	sleep(1000);
        }
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        GridServiceManagerRemovedEventListener removedEventListener = new GridServiceManagerRemovedEventListener() {

            public void gridServiceManagerRemoved(
                    GridServiceManager gridServiceManager) {
                if (gridServiceManager.equals(gsm)) {
                    latch.countDown();
                }
            }
        };
        GridServiceManagers managers = gsm.getAdmin().getGridServiceManagers();
        managers.getGridServiceManagerRemoved().add(removedEventListener);
        try {
            gsm.kill();
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted while killing esm", e);
        } finally {
            managers.getGridServiceManagerRemoved().remove(removedEventListener);
        }
        
        return AdminUtils.loadGSM(agent);
    }
    
    
    public static void killGsm(final GridServiceManager gsm) {
        Admin admin = gsm.getAdmin();
        final CountDownLatch latch = new CountDownLatch(1);
        GridServiceManagerRemovedEventListener eventListener = new GridServiceManagerRemovedEventListener() {
            
        	@Override
        	public void gridServiceManagerRemoved(
                    GridServiceManager gridServiceManager) {
                if (gridServiceManager.equals(gsm)) {
                    latch.countDown();
                }
                
            }};
        admin.getGridServiceManagers().getGridServiceManagerRemoved().add(eventListener);
        try {
            gsm.kill();
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted while killing gsm", e);
        } finally {
            admin.getGridServiceManagers().getGridServiceManagerRemoved().remove(eventListener);
        }
    }
    
    public static void killEsm(final ElasticServiceManager esm) {
        Admin admin = esm.getAdmin();
        final CountDownLatch latch = new CountDownLatch(1);
        ElasticServiceManagerRemovedEventListener eventListener = new ElasticServiceManagerRemovedEventListener() {
            
        	@Override
        	public void elasticServiceManagerRemoved(
                    ElasticServiceManager gridServiceManager) {
                if (gridServiceManager.equals(esm)) {
                    latch.countDown();
                }
                
            }};
        admin.getElasticServiceManagers().getElasticServiceManagerRemoved().add(eventListener);
        try {
            esm.kill();
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted while killing gsm", e);
        } finally {
            admin.getElasticServiceManagers().getElasticServiceManagerRemoved().remove(eventListener);
        }
    }
    
    public static boolean isEvenlyDistributedAcrossMachines(ProcessingUnit pu, Machine[] machines) {
        
        boolean rebalancedMachines = RebalancingUtils
                .isEvenlyDistributedAcrossMachines(pu, getClusterCapacity(machines));
        return rebalancedMachines;
    }

	private static CapacityRequirementsPerAgent getClusterCapacity(Machine[] machines) {
		CapacityRequirementsPerAgent clusterCapacityRequirements = new CapacityRequirementsPerAgent();
        for (final Machine machine : machines) {
        	if (machine.getGridServiceAgent() != null /* machine not going down*/) { 
            clusterCapacityRequirements = clusterCapacityRequirements.add(
                        machine.getGridServiceAgent().getUid(),
                        new MachineCapacityRequirements(machine));
        	}
        }
		return clusterCapacityRequirements;
	}

	public static void waitForDrives(Admin admin, final String drive, final int driveCapacityInMB) {
		
		final AtomicLong totalMBs = new AtomicLong();
		final CountDownLatch latch = new CountDownLatch(1);
        MachineAddedEventListener eventListener = new MachineAddedEventListener() {
            
			public void machineAdded(Machine machine) {
			
				MachineCapacityRequirements machineCapacityRequirements = new MachineCapacityRequirements(machine);
				LogUtils.log(machineCapacityRequirements.toString());
				long driveCapacityInMB = machineCapacityRequirements.getRequirement(new DriveCapacityRequirement(drive).getType()).getDriveCapacityInMB();
				LogUtils.log("machine " + machine.getHostAddress() + " has " + driveCapacityInMB + "MB total disk space on " + drive);
				totalMBs.addAndGet(
					driveCapacityInMB);
				
				LogUtils.log("All machines have " + totalMBs.get() + "MB total disk space on " + drive + " test target is " + driveCapacityInMB + "MB");
				if (totalMBs.get() >= driveCapacityInMB) {
					latch.countDown();
				}
				
			}};
        admin.getMachines().getMachineAdded().add(eventListener);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted while waiting for " + driveCapacityInMB + "GB drives", e);
        } finally {
            admin.getMachines().getMachineAdded().remove(eventListener);
        }
		
	}
	
	public static RepetitiveConditionProvider newScaleConditionProvider(
	        final ProcessingUnit pu,
            final int expectedNumberOfContainers,
            final int expectedNumberOfMachines,
            final boolean validateCpuSla) {
	    
	    return new RepetitiveConditionProvider() {
            
            private String lastMessage = "";
            
            public boolean getCondition() {
                String message = "waitForScaleToComplete: ";
                
                Admin admin = pu.getAdmin();
                GridServiceContainer[] containers = getContainersMatchesProcessingUnitZone(pu);
                Machine[] machines = RebalancingUtils.getMachinesHostingContainers(containers);
                boolean expectedMachines;
                if (expectedNumberOfMachines == 0) {
                    // in cases we are not using Xen, we may deploy on only part of the machines.
                    expectedMachines = true;
                }
                else {
                    expectedMachines = expectedNumberOfMachines == machines.length;
                    if (!expectedMachines) {
                        message += "expected " + expectedNumberOfMachines +" machines, actual " + machines.length + " machines. ";
                    }
                }
                
                boolean expectedContainers = (containers.length == expectedNumberOfContainers);
                if (!expectedContainers) {
                    message += "expected " + expectedNumberOfContainers +" containers, actual " + containers.length + " containers. ";
                }
                boolean rebalancedMachines = true;
                if (validateCpuSla) {
                    // only when cpu sla is involved the esm actually distributes evenly accross machines
                    rebalancedMachines = isEvenlyDistributedAcrossMachines(pu, machines);
                    if (!rebalancedMachines) {
                        message += "expected primary instances per machine core to be rebalanced, but they are not. ";
                    }
                }
                boolean rebalancedContainers = RebalancingUtils
                        .isEvenlyDistributedAcrossContainers(pu, containers);
                if (!rebalancedContainers) { 
                    message += "expected instances per container to be rebalanced, but they are not. ";
                }
                
                
                
                if (!lastMessage.equals(message)) {
                    LogUtils.log(message);
                    lastMessage = message;
                    LogUtils.log(getClusterCapacity(machines).toDetailedString());
                    TeardownUtils.snapshot(admin);
                }
                return expectedMachines && expectedContainers && 
                       rebalancedMachines && rebalancedContainers;
            }

			private GridServiceContainer[] getContainersMatchesProcessingUnitZone(ProcessingUnit pu) {
				return ContainersSlaUtils.getContainersByZone(
                        pu.getAdmin(),
                        ContainersSlaUtils.getContainerZone(pu)).toArray(new GridServiceContainer[0]);
			}
        };
	}
	
	
    public static void assertScaleCompletedIgnoreCpuSla(ProcessingUnit pu,
            int expectedNumberOfContainers) {
        boolean validateCpuSla = false;
        assertScaleCompleted(pu, expectedNumberOfContainers, validateCpuSla);
    }

    private static void assertScaleCompleted(ProcessingUnit pu,
            int expectedNumberOfContainers, boolean validateCpuSla) {
        assertScaleCompleted(pu, expectedNumberOfContainers, 0, validateCpuSla);
    }

    private static void assertScaleCompleted(final ProcessingUnit pu,
            final int expectedNumberOfContainers,
            final int expectedNumberOfMachines, final boolean validateCpuSla) {

        final RepetitiveConditionProvider condition = newScaleConditionProvider(
                pu, expectedNumberOfContainers, expectedNumberOfMachines,
                validateCpuSla);
        AssertUtils.assertTrue("Scale condition not met",
                condition.getCondition());
    }
    
    public static void assertUndeployAndWait(ProcessingUnit pu, long timeout, TimeUnit timeUnit) {
		LogUtils.log("Undeploying processing unit " + pu.getName());
		Assert.assertTrue(pu.undeployAndWait(timeout, timeUnit),pu.getName() + " undeploy failed");
		LogUtils.log("Undeployed processing unit " + pu.getName());
	}
}
