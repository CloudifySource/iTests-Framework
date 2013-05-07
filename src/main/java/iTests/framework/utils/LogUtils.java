package iTests.framework.utils;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import com.gigaspaces.log.LogEntryMatchers;
import iTests.framework.testng.report.SGTestNGReporter;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import junit.framework.Assert;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogUtils {
    
    public static void log(String message) {
        log(message, null);
    }

    public static void log(String message, Throwable t) {
        SGTestNGReporter.log(message, t, true);
    }

    public static void assertLogExists(GridServiceContainer container , String log) {
        LogEntryMatcher matcher = LogEntryMatchers.containsString(log);
        LogEntries logEntriesContainer1 = container.logEntries(matcher);
        if (logEntriesContainer1.logEntries().size() == 0) {
            Assert.fail("Expected String : [" + log +"] is not found on GSC PID [ " + container.getVirtualMachine().getDetails().getPid()
                    +" ] Machine [" + container.getMachine().getHostName() +"]");
        }
    }

    public static void assertLogExistsOnAllGSCs(Admin admin , String log) {
        for(GridServiceContainer gsc : admin.getGridServiceContainers()){
            assertLogExists(gsc , log);
        }
    }

    public static void assertLogNotExistsOnAllGSCs(Admin admin , String log) {
        for(GridServiceContainer gsc : admin.getGridServiceContainers()){
            assertLogNotExists(gsc , log);
        }
    }

    public static void assertLogNotExists(GridServiceContainer container , String log) {
        LogEntryMatcher matcher = LogEntryMatchers.containsString(log);

        LogEntries logEntriesContainer1 = container.logEntries(matcher);
        if (logEntriesContainer1.logEntries().size() > 0) {
            log("container.logEntries( " +log+ " ).logEntries().size(): " + logEntriesContainer1.logEntries().size());
            for (LogEntry logEntry : logEntriesContainer1.getEntries()) {
                log("Log entry: " + logEntry.getText());
            }
            Assert.fail("Expected String : [" + log +"] found on GSC PID [ " + container.getVirtualMachine().getDetails().getPid()
                    +" ] Machine [" + container.getMachine().getHostName() +"]");
        }
    }

    public static void scanContainerLogsFor(GridServiceContainer container , String error) {
        scanContainerLogsFor(container, error, new String[] {});
    }

    /**
     * scans the logs of the GSC for a specific error message
     * prints the corresponding log entries of the error
     * @param container - the container to scan
     * @param error - witch error to look for
     */
    public static void scanContainerLogsFor(GridServiceContainer container , String error, String[] ignoredErrors) {

        log("Scanning container " + container.getUid() + "for text : " + error);

        LogEntryMatcher matcher = LogEntryMatchers.containsString(error);

        List<LogEntry> logEntries = container.logEntries(matcher).logEntries();
        logEntries = filterEntries(logEntries, ignoredErrors);
        if (logEntries.size() > 0) {
            log("container.logEntries("+error+").getEntries().size(): " + logEntries.size());
            for (LogEntry logEntry : logEntries) {
                log(logEntry.getText());
            }
            Assert.fail("unexpected String : [" + error +"] found on GSC PID [ " + container.getVirtualMachine().getDetails().getPid()
                    +" ] Machine [" + container.getMachine().getHostName() +"]");
        }
    }

    /**
     * returns a filtered list of entries without those containing one or more of the specified errors to ignore
     */
    private static List<LogEntry> filterEntries(List<LogEntry> logEntries, String[] ignoredErrors) {
        List<LogEntry> filteredEntries = new ArrayList<LogEntry>();
        for (LogEntry logEntry : logEntries) {
            boolean ignore = isLogEntryContainsOneOrMore(logEntry,ignoredErrors);
            if (ignore) {
                log("Ignoring log error (contains one of "+ Arrays.toString(ignoredErrors) +"): "+ logEntry.getText());
            }
            else {
                filteredEntries.add(logEntry);
            }
        }
        return filteredEntries;
    }

    private static boolean isLogEntryContainsOneOrMore(
            LogEntry logEntry,
            String[] ignoredErrors) {

        boolean ignore = false;
        for (final String ignoredError : ignoredErrors) {
            if (logEntry.getText().contains(ignoredError)) {
                ignore = true;
                break;
            }
        }
        return ignore;
    }
    
    /**
     * scans the logs of the GSM for a specific error message
     * prints the corresponding log entries of the error
     * @param gsm - the container to scan
     * @param error - witch error to look for
     */
    
    public static void scanManagerLogsFor(GridServiceManager gsm , String error) {
    	
    	LogEntryMatcher matcher = LogEntryMatchers.containsString(error);
    	LogEntries logEntriesGsm = gsm.logEntries(matcher);
        if (logEntriesGsm.logEntries().size() > 0) {
            log("gsm.logEntries("+error+").logEntries().size(): " + logEntriesGsm.getEntries().size());
			for (LogEntry logEntry : logEntriesGsm.logEntries()) {
				log(logEntry.getText());		
			}
            Assert.fail("unexpected String : [" + error +"] found on GSM PID [ " + gsm.getVirtualMachine().getDetails().getPid()
                    +" ] Machine [" + gsm.getMachine().getHostName() +"]");
		}
    }
    
    
    /**
     * scans the logs of the GSM repetatively for a specific text message
     * assert fails if text is not found in logs after timeout passes
     * @param gsm - the gsm to scan
     * @param text - the text to look for
     */
    
    public static void repetativeScanManagerLogsFor(final GridServiceManager gsm , final String text, long timeoutInMillis) {
        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                LogEntryMatcher matcher = LogEntryMatchers.containsString(text);
                LogEntries logEntriesGsm = gsm.logEntries(matcher);
                return logEntriesGsm.getEntries().size() > 1;
            }
        };
        AssertUtils.repetitiveAssertTrue("Failed finding: " + text + " in the gsm logs", condition, timeoutInMillis);
    }
    
    /**
     * scans the logs of the GSC repetatively for a specific text message
     * assert fails if text is not found in logs after timeout passes
     * @param gsc - the gsm to scan
     * @param text - the text to look for
     */
    
    public static void repetativeScanContainerLogsFor(final GridServiceContainer gsc , final String text, long timeoutInMillis) {
        RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
            public boolean getCondition() {
                LogEntryMatcher matcher = LogEntryMatchers.containsString(text);
                LogEntries logEntriesGSC = gsc.logEntries(matcher);
                return logEntriesGSC.getEntries().size() > 1;
            }
        };
        AssertUtils.repetitiveAssertTrue("Failed finding: " + text + " in the gsc logs", condition, timeoutInMillis);
    }

    public static int countAgentLogs(GridServiceAgent agent, String containsString) {

        LogEntryMatcher matcher = LogEntryMatchers.containsString(containsString);
        List<LogEntry> logEntries = agent.logEntries(matcher).logEntries();
        return logEntries.size();
    }
    /**
     *
     * Scans for the text message in the logs written since lastLogEntriesTaken was taken
     * @param gsc
     * @param lastLogEntriesTaken - If null will scan newly taken log entries without filtering any old entries
     * @param text
     * @param checkLogAppeared - A flag for this method's operation. If passed true asserts that text appeared since lastLogEntriesTaken was taken.
     * If passed false asserts text didn't appear since lastLogEntriesTaken was taken.
     * @return return a List<LogEntry> - Can be used in a later check
     */
    public static List<LogEntry> logAppearedSinceLastCheck(GridServiceContainer gsc, List<LogEntry> lastLogEntriesTaken, String text , boolean checkLogAppeared) {

        LogEntryMatcher matcher = LogEntryMatchers.containsString(text);
        List<LogEntry> logEntriesSinceLastCheck = getLogEntriesDelta(gsc, lastLogEntriesTaken, matcher);

        String errorMsg = lastLogEntriesTaken == null ? "" : "since lastLogEntriesTaken was taken";
        if(checkLogAppeared)
            Assert.assertTrue("Didn't find the string '" + text + "' in the gsc log entries " + errorMsg,logEntriesSinceLastCheck.size() > 0);
        else
            Assert.assertTrue("Found the string '" + text + "' in the gsc log entries " + errorMsg, logEntriesSinceLastCheck.size() == 0);

        return logEntriesSinceLastCheck;
    }

    private static List<LogEntry> getLogEntriesDelta(GridServiceContainer gsc, List<LogEntry> lastLogEntriesTaken ,LogEntryMatcher matcher)
    {
        if(lastLogEntriesTaken == null)
        {
            return gsc.logEntries(matcher).getEntries();
        }
        List<LogEntry> currentLogEntries = gsc.logEntries(matcher).logEntries();
        for(int i=0 ; i < currentLogEntries.size() ; ++i)
        {
            for(LogEntry olderLogEntry : lastLogEntriesTaken)
            {
                if(currentLogEntries.get(i).getText().equals(olderLogEntry.getText()))
                    currentLogEntries.remove(i);
            }
        }
        return currentLogEntries;
    }

}
