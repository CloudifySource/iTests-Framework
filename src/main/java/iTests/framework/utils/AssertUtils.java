package iTests.framework.utils;

import junit.framework.AssertionFailedError;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows placing a "catch all" breakpoint before all assert failures
 * 
 * @author giladh
 * 
 */
public class AssertUtils {

	public static boolean skipException() {
		final boolean skip = false;
		return skip; // PLACE BREAKPOINT HERE
	}

	public interface RepetitiveConditionProvider {
		boolean getCondition();
	}

	static public void repetitiveAssertTrue(final String message,
			final RepetitiveConditionProvider condition,
			final long timeoutMilliseconds) {
        repetitiveAssertTrue(message, condition, timeoutMilliseconds, 1000);
    }

    static public void repetitiveAssertTrue(String message,
                                            RepetitiveConditionProvider condition,
                                            long timeoutMilliseconds, long intervalMilliseconds) {
        long end = System.currentTimeMillis() + timeoutMilliseconds;
        while (System.currentTimeMillis() < end) {
            try {
                _assertTrue(message, condition.getCondition(), false);
                return;
            } catch (AssertionError e) {
                try {
                    sleep(intervalMilliseconds);
                } catch (InterruptedException e1) { }
            }
        }
        assertTrue(message, condition.getCondition());
    }

    public static void sleep(long durationMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread.sleep(durationMillis);
        long actualDurationMillis = System.currentTimeMillis() - start;
        if (Math.abs(actualDurationMillis - durationMillis) > TimeUnit.MINUTES.toMillis(1)) {
            throw new IllegalStateException("Sleep " + durationMillis + " resulted in actual sleep for " + actualDurationMillis + ". Could be system clock drift or GC, etc...");
        }
    }

    static public void debugRepetitiveAssertTrue(String message,
                                                 RepetitiveConditionProvider condition,
                                                 long timeoutMilliseconds) {
        LogUtils.log("entering repetitive assert, timeout for this method is :" + timeoutMilliseconds);
        long end = System.currentTimeMillis() + timeoutMilliseconds;
        LogUtils.log("timeout end time is : " + end);
        while (System.currentTimeMillis() < end) {
            try {
                LogUtils.log("current time is :" + System.currentTimeMillis());
                _assertTrue(message, condition.getCondition(), false);
                LogUtils.log("condition satisfied, exisiting repetitive assert");
                return;
            } catch (AssertionError e) {
                LogUtils.log("Caught assertion error, condition not satisfied");
                try {
                    LogUtils.log("sleeping for one second before trying again");
                    sleep(1000);
                    LogUtils.log("waking up");
                } catch (InterruptedException e1) { }
            }
        }
        LogUtils.log("timeout was reached, asserting one last time");
        assertTrue(message, condition.getCondition());
    }

    /**
     * Passes 100 as the default value for intervalMilliseconds.
     * @param message the message to log on fail
     * @param condition the condition to check
     * @param timeoutMilliseconds the time the condition should hold
     * @see {@link #repetitiveAssertConditionHolds(String, RepetitiveConditionProvider, long, long)}
     */
    static public void repetitiveAssertConditionHolds(String message,
                                                      RepetitiveConditionProvider condition,
                                                      long timeoutMilliseconds) {
        repetitiveAssertConditionHolds(message, condition, timeoutMilliseconds, 100);
    }

    /**
     * Passes 100 as the default value for intervalMilliseconds.
     * @param message the message to log on fail
     * @param condition the condition to check
     * @param timeoutMilliseconds the time the condition should hold
     * @param intervalMilliseconds the time interval between assertions
     */
	static public void repetitiveAssertConditionHolds(final String message,
			final RepetitiveConditionProvider condition,
			final long timeoutMilliseconds, final long intervalMilliseconds) {
		final long end = System.currentTimeMillis() + timeoutMilliseconds;
		while (System.currentTimeMillis() < end) {
			_assertTrue(message, condition.getCondition(), false);
			try {
				sleep(intervalMilliseconds);
			} catch (final InterruptedException e) {
			}
		}
		assertTrue(message, condition.getCondition());
	}

	public static void assertEquals(final Object expected, final Object actual) {
		assertEquals("", expected, actual);
	}

	public static void assertEquals(final String msg, final Object expected, final Object actual) {
		try {
			org.testng.AssertJUnit.assertEquals(msg, expected, actual);
		}
        catch (final AssertionFailedError e) {
			if (!skipException()) {
				throw e;
			}
		}
	}

	public static void assertEquals(final int expected, final int actual) {
		assertEquals("", expected, actual);
	}

	public static void assertEquals(final String msg, final int expected, final int actual) {
		if (expected != actual) {
			if (!skipException()) {
				org.testng.AssertJUnit.assertEquals(msg, expected, actual);
			}
		}
	}

	public static void assertFail(final String msg) {
		assertFail(msg, null);
	}

	public static void assertFail(final String msg, final Throwable t) {
		if (!skipException()) {
			if (t != null) {
				Assert.fail(msg, t);
			} else {
				Assert.fail(msg);
			}
		}
	}

	public static void assertNotNull(final Object obj) {
		assertNotNull("", obj);
	}

	public static void assertNull(final Object obj) {
		assertNull("", obj);
	}

	public static void assertNotNull(final String msg, final Object obj) {
		if (obj == null) {
			if (!skipException()) {
				org.testng.AssertJUnit.assertNotNull(msg, obj);
			}
		}
	}

	public static void assertNull(final String msg, final Object obj) {
		if (obj != null) {
			if (!skipException()) {
				org.testng.AssertJUnit.assertNull(msg, obj);
			}
		}
	}

	public static void assertTrue(final boolean cond) {
		_assertTrue("", cond, true);
	}

    public static  void assertFalse(final boolean cond) {
        _assertFalse("", cond, true);
    }

	public static void assertTrue(final String msg, final boolean cond) {
		_assertTrue(msg, cond, true);
	}

    public static  void assertFalse(final String msg, final boolean cond) {
        _assertFalse(msg, cond, true);
    }

	public static void _assertTrue(final String msg, final boolean cond, final boolean checkForSkip) {
		if (!cond) {
			final boolean skip = checkForSkip ? skipException() : false;
			if (!skip) {
				org.testng.AssertJUnit.assertTrue(msg, cond);
			}
		}
	}

    public static void _assertFalse(final String msg, final boolean cond, final boolean checkForSkip) {
        if (cond) {
            boolean skip = checkForSkip ? skipException() : false;
            if (!skip) {
                org.testng.AssertJUnit.assertFalse(msg, cond);
            }
        }
    }

	static public void assertEquivalenceArrays(final String message, final Object[] expected, final Object[] actual)

	{

		if (Arrays.equals(expected, actual)) {

			return;

		}

		final String formatted = format(message);

		org.testng.AssertJUnit.assertNotNull(formatted + "expected array: <not null> but was <null>", expected);

		org.testng.AssertJUnit.assertNotNull(formatted + "expected array: <not null> but was <null>", actual);

		final ArrayList<Object> missing = new ArrayList<Object>();

		for (int i = 0; i < expected.length; i++) {

			missing.add(expected[i]);

		}

		final ArrayList<Object> extra = new ArrayList<Object>();

		for (int i = 0; i < actual.length; i++) {

			extra.add(actual[i]);

		}

		@SuppressWarnings("unchecked")
		final ArrayList<Object> missingClone = (ArrayList<Object>) missing.clone();

		missing.removeAll(extra);

		extra.removeAll(missingClone);

		org.testng.AssertJUnit.assertTrue(formatted + "[ " + missing.size() + " Missing elements: " + missing + "]", missing.size() == 0);

		org.testng.AssertJUnit.assertTrue(formatted + "[ " + extra.size() + " Extra elements: " + extra + "]", extra.size() == 0);

	}

	static String format(final String message){

		if (message != null) {
			return message + " - ";
		} else {
			return "";
		}
	}

    private AssertUtils() {}

	public static void repetitive(final IRepetitiveRunnable repeatedAssert,
			final int timeout) {
		for (int delay = 0; delay < timeout; delay += 5) {
			try {
				repeatedAssert.run();
				return;
			} catch (final Throwable e) { // catch any exception and error
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e1) {
				}
			}
		}
		try {
			repeatedAssert.run();
		} catch (final AssertionError e) {
			throw e;
		} catch (final Exception e) {
			assertFail(e.getMessage(), e);
		}
	}

	public static void repetitiveAssertFalse(
            final String message,
			final RepetitiveConditionProvider condition,
			final long timeoutMilliseconds) {
		repetitiveAssertTrue(message, new RepetitiveConditionProvider() {

			@Override
			public boolean getCondition() {
				return !condition.getCondition();
			}
		}, timeoutMilliseconds);
	}

    public static void reptitiveCountdownLatchAwait(final CountDownLatch countdownLatch, final String name, long timeout, TimeUnit timeunit) {
        repetitiveAssertTrue("latch count is not zero", new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                try {
                    LogUtils.log("Waiting for latch " + name + " . Current count: " + countdownLatch.getCount());
                    return countdownLatch.await(0, TimeUnit.MILLISECONDS);
                } catch (final InterruptedException e) {
                    Assert.fail("Interrupted while waiting for latch " + name, e);
                    return false;
                }
            }
        }, timeunit.toMillis(timeout));
    }

	public static ProcessingUnitInstance[] repetitiveAssertNumberOfInstances(
			final ProcessingUnit pu, final int expectedNumberOfInstances) {

		final AtomicReference<ProcessingUnitInstance[]> instances = new AtomicReference<ProcessingUnitInstance[]>();
		repetitiveAssertTrue("Failed waiting for " + expectedNumberOfInstances + " " + pu.getName() + " instances.",
				new RepetitiveConditionProvider() {

					@Override
					public boolean getCondition() {
						instances.set(pu.getInstances());
						final int numberOfInstances = instances.get().length;
						if (numberOfInstances != expectedNumberOfInstances) {
							LogUtils.log("Waiting for "
                                    + expectedNumberOfInstances + " "
                                    + pu.getName() + " instances. " + "Actual "
                                    + numberOfInstances + " instances.");
						}
						return numberOfInstances == expectedNumberOfInstances;
					}
				}, TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
		return instances.get();
	}

}
