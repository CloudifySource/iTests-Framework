package iTests.framework.utils;

import com.gigaspaces.internal.sigar.SigarHolder;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CommandTestUtils {

	private static Sigar sigar = SigarHolder.getSigar();

	/**
	 * 
	 * Runs local commands and outputs the result to the log. Can't inspect command output as it runs.
	 * @param command
	 * @param wait - if true - waits for the command to return (and then the method will return the command's output).<P>
	 * if false - runs the command and returns null.
	 * @param failCommand
	 * @return
	 * @throws java.io.IOException
	 * @throws InterruptedException
	 */
    public static String runLocalCommand(final String command, boolean wait, boolean failCommand) throws IOException, InterruptedException {
    	AtomicReference<ThreadSignal> threadSignal = new AtomicReference<ThreadSignal>();
    	threadSignal.set(ThreadSignal.RUN_SIGNAL);
    	return runLocalCommand(command, null, 0, false, wait, failCommand, threadSignal, null,new String[0]).getOutput();
    }
    /**
	 *
	 * Runs local commands (from the specified workingDir) and outputs the result to the log. Can't inspect command output as it runs.
	 */
    public static String runLocalCommand(final String command, boolean wait, boolean failCommand, final String workingDir) throws IOException, InterruptedException {
    	AtomicReference<ThreadSignal> threadSignal = new AtomicReference<ThreadSignal>();
    	threadSignal.set(ThreadSignal.RUN_SIGNAL);
    	return runLocalCommand(command, workingDir, 0, false, wait, failCommand, threadSignal, null,new String[0]).getOutput();
    }

    /**
     * General method for running local commands and output the result to the log. Can be used in the following manors: <p>
     * 1) simply run command<p>
     * 2) run commands, wait for the output and return it<p>
     * 3) run commands and inspect the output as the command runs, waiting for some message
     *
     * @param command
     * @param expectedMessages
     * @param workingDirPath
     * @param timeoutMillis - timeout for repetitive assert on command's output containing expectedMessage in background console
     * @param backgroundConsole - If true then the command to be run is expected to not return
     * and keep the console in the background. Since we can't wait until the command will end to inspect the output
     * handleOutputOfBlockingCommand() is called to inspect the output on the fly, according to timeoutMillis and expectedMessage.
     * @param waitForForegroundConsole - If the console is foreground this will indicate if the method waits for the command to return or not.
     * @param failCommand - used for determining if the command is expected to fail (in foreground case)
     * @return
     * @throws java.io.IOException
     * @throws InterruptedException
     */
    public static ProcessOutputPair runLocalCommand(final String command, final String workingDirPath,
    		long timeoutMillis, boolean backgroundConsole, boolean waitForForegroundConsole,
    		boolean failCommand, AtomicReference<ThreadSignal> threadSignal, final Map<String, String> additionalProcessVariables,final String... expectedMessages) throws IOException, InterruptedException{

    	if(backgroundConsole == true && waitForForegroundConsole == true){
    		throw new IllegalStateException("Usage: The console can't be both background and foreground");
    	}

    	String cmdLine = command;
        if (isWindows()) {
            cmdLine = "cmd /c call " + cmdLine;
        }

        final String[] parts = cmdLine.split(" ");
        final ProcessBuilder pb = new ProcessBuilder(parts);

        if(workingDirPath != null){
        	File workingDir = new File(workingDirPath);
        	if(!workingDir.exists() || !workingDir.isDirectory()){
        		throw new IOException(workingDirPath + " should be a path of a directory");
        	}
        	pb.directory(workingDir);
        }
        if(additionalProcessVariables != null){
        	Map<String, String> env = pb.environment();
        	for(Entry<String, String> additinalVar : additionalProcessVariables.entrySet()){
        		env.put(additinalVar.getKey(), additinalVar.getValue());
        	}
        }
        pb.redirectErrorStream(true);

        LogUtils.log("Executing Command line: " + cmdLine);
        final Process process = pb.start();

        if(backgroundConsole){
        	return new ProcessOutputPair(process, handleOutputOfBackgroundCommand(threadSignal, process, command, timeoutMillis, expectedMessages));
        }
        if(waitForForegroundConsole){
            return new ProcessOutputPair(process, handleCliOutput(process, failCommand));
        }
        return new ProcessOutputPair(process, null);
    }

    public static String handleOutputOfBackgroundCommand(final AtomicReference<ThreadSignal> threadSignal ,Process process,
    		final String command , long timeoutMillis, final String... expectedMessages) throws IOException, InterruptedException{

		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		final StringBuilder consoleOutput = new StringBuilder("");
		final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
		final AtomicBoolean foundOutputMessages = new AtomicBoolean(false);

		Thread thread = new Thread(new Runnable() {

			String line = null;
			boolean foundAllExpectedMessages = false;

			@Override
			public void run() {
				try {
					while (threadSignal.get() == ThreadSignal.RUN_SIGNAL && (br.ready() ? ((line = br.readLine()) != null) : sleepAndReturnTrue())) {
						//prevents repetitive logging of the same line
						if(line != null){
							LogUtils.log(line);
							consoleOutput.append(line + "\n");
							line = null;
						}
						foundAllExpectedMessages = stringContainsMultipleStrings(consoleOutput.toString(), expectedMessages);
						foundOutputMessages.set(foundAllExpectedMessages);
					}
					threadSignal.set(ThreadSignal.STOPPED); // signal that the thread finished
				} catch (Throwable e) {
					exception.set(e);
				}
			}
		});
		thread.setDaemon(true);
		thread.start();


		AssertUtils.repetitiveAssertTrue("The expected messages were not returned by the command " + command
                , new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                return foundOutputMessages.get();
            }
        }, timeoutMillis);

		AssertUtils.assertTrue(exception.get() == null);

		return consoleOutput.toString();
	}

    private static boolean sleepAndReturnTrue() throws InterruptedException {
    	AssertUtils.sleep(100);
    	return true;
    }

	private static String handleCliOutput(Process process, boolean failCommand) throws IOException, InterruptedException{
		// Print CLI output if exists.
		final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		final StringBuilder consoleOutput = new StringBuilder("");
		final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();

		Thread thread = new Thread(new Runnable() {

			String line = null;

			@Override
			public void run() {
				try {
					while ((line = br.readLine()) != null) {
						LogUtils.log(line);
						consoleOutput.append(line + "\n");
					}
				} catch (Throwable e) {
					exception.set(e);
				}

			}
		});

		thread.setDaemon(true);

		thread.start();

		int result = process.waitFor();

		thread.join(5000);

		AssertUtils.assertTrue(exception.get() == null);

		if (result != 0 && !failCommand) {
			AssertUtils.assertFail("In RunCommand: Process did not complete successfully");
		}
		return consoleOutput.toString();
	}





	private static String getCommandExtention() {
		String osExtention;
		if (isWindows()) {
			osExtention = "bat";
		} else {
			osExtention = "sh";
		}
		return osExtention;
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("win");
	}

	public static class ProcessOutputPair{

		Process process;
		String output;

		public ProcessOutputPair(Process process, String output){
			this.process = process;
			this.output = output;
		}

		public Process getProcess(){
			return process;
		}
		public String getOutput(){
			return output;
		}

	}
	public static enum ThreadSignal{RUN_SIGNAL, STOP_SIGNAL, STOPPED}

	public static boolean stringContainsMultipleStrings(String s, String[] expecteds){

		for(String expected : expecteds){
			if(!s.contains(expected)){
				return false;
			}
		}
		return true;
	}
	/**
	 * Kills the entire process tree.
	 * @param rootPid
	 * @throws Exception
	 */
	public static void killProcessTree(long rootPid) throws Exception{

		Set<Long> allChildProcesses = getChildProcessesRecursively(rootPid);
		for(Long childPid : allChildProcesses)
			killProcess(childPid);
	}

	public static Set<Long> getChildProcessesRecursively(long rootPid) throws Exception{

		Set<Long> allChildProcesses = getChildProcesses(rootPid);

		Long tmpChildProcessArray[] = new Long[allChildProcesses.size()];
		allChildProcesses.toArray(tmpChildProcessArray);

		if(allChildProcesses.isEmpty())
			return allChildProcesses;

		for(Long childPid : tmpChildProcessArray){

			allChildProcesses.addAll(getChildProcessesRecursively(childPid));
		}

		return allChildProcesses;
	}

	/**
	 * Kills the processes immediate children.
	 * @param ppid
	 * @param killParent - flag to indicate if the parent should be killed too.
	 * @throws Exception
	 */
	public static void killChildProcesses(long ppid , boolean killParent) throws Exception
	{
		Set<Long> childrenPids = getChildProcesses(ppid);

		for(Long childPid : childrenPids){
			killProcess(childPid);
		}

		if(killParent)
			killProcess(ppid);
	}

	public static void killProcess(long pid) throws Exception{

		try {
			sigar.kill(pid, 9);
		} catch (SigarException e) {
			throw new Exception("Fialed to kill the process. Error was: " + e.getMessage(), e);
		}
	}

	public static Set<Long> getChildProcesses(long ppid) throws Exception
	{
		long[] pids = getAllPids();
		return getChildProcesses(ppid, pids);
	}

	private static long[] getAllPids() throws Exception
	{
		long[] pids;
		try {
			pids = sigar.getProcList();
		} catch (SigarException se) {
			throw new Exception("Failed to look up process IDs. Error was: " + se.getMessage(), se);
		}
		return pids;
	}

	private static Set<Long> getChildProcesses(long ppid, long[] pids)
	{
		Set<Long> children = new HashSet<Long>();
		for (long pid : pids) {
			try {
				if (ppid == sigar.getProcState(pid).getPpid())
					children.add(Long.valueOf(pid));
			}
			catch (SigarException e) {
				LogUtils.log("While scanning for child processes of process " + ppid + ", could not read process state of Process: " + pid + ". Ignoring.", e);
			}

		}
		return children;
	}

	/*************
	 * Executes a SIGAR PTQL query, returning the PIDs of the processes that match the query. For more info on
	 * SIGAR's PTQL - Process Table Query Language, see: http://support.hyperic.com/display/SIGAR/PTQL
	 *
	 * @param query the PTQL query.
	 * @return the pids.
	 * @throws org.hyperic.sigar.SigarException in case of an error.
	 */
	public static List<Long> getPidsWithQuery(final String query)throws SigarException {

		final ProcessFinder finder = new ProcessFinder(sigar);
		final long[] results = finder.find(query);
		final List<Long> list = new ArrayList<Long>(results.length);
		for (final long result : results) {
			list.add(result);
		}
		return list;
	}

	/*********
	 * Returns the pids of Java processes where the name specified in in the process arguments. This will usually be
	 * the java process' main class, though that may not always be the case.
	 *
	 * PTQL Query: "State.Name.eq=java,Args.*.eq=" + name
	 *
	 * @param name the java main class or jar file name.
	 * @return the pids that match the query, may be zero, one or more.
	 * @throws org.hyperic.sigar.SigarException in case of an error.
	 */
	public static List<Long> getPidsWithMainClass(final String name)
			throws SigarException {
		return getPidsWithQuery("State.Name.eq=java,Args.*.eq=" + name);
	}

	/*************
	 * Returns the pids of processes which have the specified environment variable. PTQL Query:
	 * "Env.varName.eq=varValue"
	 *
	 * @param
	 * @return the matching PIDs.
	 * @throws org.hyperic.sigar.SigarException in case of an error.
	 */
	public static List<Long> getPidsWithEnvVar(final String varName, final String varValue)
			throws SigarException {
		return getPidsWithQuery("Env." + varName + ".eq=" + varValue);
	}

	/*************
	 * Returns the pids of processes where the base name of the process executable is as specified. PTQL Query:
	 * "State.Name.eq=" + name
	 *
	 * @param name the process name.
	 * @return the matching PIDs.
	 * @throws org.hyperic.sigar.SigarException in case of an error.
	 */
	public static List<Long> getPidsWithName(final String name)
			throws SigarException {
		return getPidsWithQuery("State.Name.eq=" + name);
	}

	/*************
	 * Returns the pids of processes where the full name of the process executable is as specified. PTQL Query:
	 * "Exe.Name.eq=" + name.
	 *
	 * @param name the process name.
	 * @return the matching PIDs.
	 * @throws org.hyperic.sigar.SigarException in case of an error.
	 */
	public static List<Long> getPidsWithFullName(final String name)
			throws SigarException {
		return getPidsWithQuery("Exe.Name.eq=" + name);
	}

}
