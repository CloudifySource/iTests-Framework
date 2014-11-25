package iTests.framework.testng.report;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.*;
import iTests.framework.utils.TestNGUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.jruby.embed.ScriptingContainer;
import org.testng.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

//import org.apache.commons.io.FileUtils;


public class SGTestNGListener extends TestListenerAdapter {

    protected String testMethodName;
    protected String suiteName;
    protected String buildNumber;
    protected String version;
    protected ScriptingContainer container;
    private Process process = null;
    private Process process2 = null;
    private String confFilePath;
    private String confFilePath2;
    private String backupFilePath;
    private String backupFilePath2;
    private String logstashLogPath;
    private String logstashLogPath2;
    private static final boolean enableLogstash = Boolean.parseBoolean(System.getProperty("iTests.enableLogstash", "false"));
    protected static final String CREDENTIALS_FOLDER = System.getProperty("iTests.credentialsFolder",SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials");
    private static File propsFile = new File(CREDENTIALS_FOLDER + "/logstash/logstash.properties");
    protected String logstashHost;

    private static long testInvocationCounter = 1;
    private static final long maxCount = Long.getLong("sgtest.webui.numberOfTestRetries", 3) + 1;

    public SGTestNGListener(){
        if(enableLogstash){
            LogUtils.log("in SGTestNGListener constructor");
        }
    }

    @Override
    public void onStart(ITestContext iTestContext) {
    	suiteName = System.getProperty("iTests.suiteName");
        LogUtils.log("suite number is now (on start) - " + suiteName);

        if(enableLogstash){
            buildNumber = System.getProperty("iTests.buildNumber");
            LogUtils.log("build number is now (on start) - " + buildNumber);
            version = System.getProperty("cloudifyVersion");
        }
    }

    private void initLogstash2(ITestResult tr) {

        initLogstashHost();

        String simpleClassName = tr.getTestClass().getRealClass().getSimpleName();
        String pathToLogstash = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/src/main/resources/logstash";
        confFilePath2 = pathToLogstash + "/logstash-shipper-client-2.conf";
        backupFilePath2 = pathToLogstash + "/logstash-shipper-client-2-" + simpleClassName + ".conf";
        File backupFile2 = new File(backupFilePath2);

        LogUtils.log("trying to start logstash agent number 2. simple class name is " + simpleClassName);
        if(backupFile2.exists()){
            LogUtils.log("the file " + backupFilePath2 + " already exists. not starting logstash");
        }

        if(!isAfter(tr) && !backupFile2.exists()){

            try {
//                backupFilePath2 = IOUtils.backupFile(confFilePath2);
                LogUtils.log("copying file " + confFilePath2 + " to " + backupFilePath2);
                IOUtils.copyFile(confFilePath2, backupFilePath2);
                IOUtils.replaceTextInFile(backupFilePath2, "<path_to_build>", SGTestHelper.getBuildDir());
                IOUtils.replaceTextInFile(backupFilePath2, "<suite_number>", "suite_" + System.getProperty("iTests.suiteId", "0"));
                IOUtils.replaceTextInFile(backupFilePath2, "<path_to_test_class_folder>", SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/../" + suiteName + "/" + tr.getTestClass().getName());
                IOUtils.replaceTextInFile(backupFilePath2, "<suite_name>", suiteName);
                IOUtils.replaceTextInFile(backupFilePath2, "<test_name>", simpleClassName);
                IOUtils.replaceTextInFile(backupFilePath2, "<build_number>", buildNumber);
                IOUtils.replaceTextInFile(backupFilePath2, "<version>", version);
                IOUtils.replaceTextInFile(backupFilePath2, "<host>", logstashHost);


                String logstashJarPath = DeploymentUtils.getLocalRepository() + "net/logstash/1.2.2/logstash-1.2.2.jar";
                logstashLogPath2 = pathToLogstash + "/logstash-" + simpleClassName + "-2.txt";
                String cmdLine = "java -jar " + logstashJarPath + " agent -f " + backupFilePath2 + " -l " + logstashLogPath2;

                final String[] parts = cmdLine.split(" ");
                final ProcessBuilder pb = new ProcessBuilder(parts);
                LogUtils.log("Executing Command line: " + cmdLine);

                process2 = pb.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initLogstash(String testName) {

        initLogstashHost();

        String pathToLogstash = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/src/main/resources/logstash";
        confFilePath = pathToLogstash + "/logstash-shipper-client.conf";
        String fixedTestName = testName.substring(0, testName.length() - 2);
        backupFilePath = pathToLogstash + "/logstash-shipper-client-" + fixedTestName + ".conf";

        if(process == null){

            try {

                LogUtils.log("copying file " + confFilePath + " to " + backupFilePath);
                IOUtils.copyFile(confFilePath, backupFilePath);
//                backupFilePath = IOUtils.backupFile(confFilePath);
                IOUtils.replaceTextInFile(backupFilePath, "<path_to_test_folder>", SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/../" + suiteName + "/" + testName);
                IOUtils.replaceTextInFile(backupFilePath, "<suite_name>", suiteName);
                IOUtils.replaceTextInFile(backupFilePath, "<test_name>", testName);
                IOUtils.replaceTextInFile(backupFilePath, "<build_number>", buildNumber);
                IOUtils.replaceTextInFile(backupFilePath, "<version>", version);
                IOUtils.replaceTextInFile(backupFilePath, "<host>", logstashHost);

                String logstashJarPath = DeploymentUtils.getLocalRepository() + "net/logstash/1.2.2/logstash-1.2.2.jar";
                logstashLogPath = pathToLogstash + "/logstash-" + fixedTestName + ".txt";
                String cmdLine = "java -jar " + logstashJarPath + " agent -f " + backupFilePath + " -l " + logstashLogPath;

                final String[] parts = cmdLine.split(" ");
                final ProcessBuilder pb = new ProcessBuilder(parts);
                LogUtils.log("Executing Command line: " + cmdLine);

                TimeUnit.SECONDS.sleep(1);
                process = pb.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void beforeConfiguration(ITestResult tr) {
        if(enableLogstash){
            super.beforeConfiguration(tr);
            if (suiteName == null) { // this is in case the suite has a @BeforeSuite method. which is invoked before the onStart is.
                suiteName = System.getProperty("iTests.suiteName");
                buildNumber = System.getProperty("iTests.buildNumber");
                LogUtils.log("build number is now - " + buildNumber);
                version = System.getProperty("cloudifyVersion");
            }
        }
    }

    @Override
    public void onConfigurationSuccess(ITestResult iTestResult) {
        super.onConfigurationSuccess(iTestResult);
        String testName = iTestResult.getTestClass().getName();
        String configurationName = iTestResult.getMethod().toString().split("\\(|\\)")[0];
        if (isAfter(iTestResult) && !enableLogstash) {
        	DumpUtils.copyBeforeConfigurationsLogToTestDir(testName, suiteName);
        	testName = testMethodName;
        }
        if (suiteName == null) { // this is in case the suite has a @BeforeSuite method. which is invoked before the onStart is.
        	suiteName = System.getProperty("iTests.suiteName");
        }
        LogUtils.log("Configuration Succeeded: " + configurationName);

        if(enableLogstash && iTestResult.getMethod().isBeforeClassConfiguration()){
            initLogstash2(iTestResult);
        }

        ZipUtils.unzipArchive(testMethodName, suiteName);

        if (enableLogstash && isAfter(iTestResult) && !iTestResult.getMethod().isAfterClassConfiguration() && !iTestResult.getMethod().isAfterSuiteConfiguration()) {
            testName = testMethodName;
        }

        write2LogFile(iTestResult, DumpUtils.createTestFolder(testName, suiteName));

        if (isAfter(iTestResult)) {
            if(enableLogstash){
                if(process != null){
                    killLogstashAgent(1, logstashLogPath);
                }
                if(process2 != null && iTestResult.getMethod().isAfterClassConfiguration()){
                    killLogstashAgent(2, logstashLogPath2);
                }
            }
        }
    }

	@Override
    public void onConfigurationFailure(ITestResult iTestResult) {
        super.onConfigurationFailure(iTestResult);
        String testName = iTestResult.getTestClass().getName();
        String configurationName = iTestResult.getMethod().toString().split("\\(|\\)")[0];
        if (!enableLogstash && isAfter(iTestResult)) {
        	DumpUtils.copyBeforeConfigurationsLogToTestDir(testName, suiteName);
        	testName = testMethodName;
        }
        if (suiteName == null) { // this is in case the suite has a @BeforeSuite method. which is invoked before the onStart is.
        	suiteName = System.getProperty("iTests.suiteName");
        }
        LogUtils.log("Configuration Failed: " + configurationName, iTestResult.getThrowable());

        if(enableLogstash && iTestResult.getMethod().isBeforeClassConfiguration()){
            initLogstash2(iTestResult);
        }

        ZipUtils.unzipArchive(testMethodName, suiteName);

        if (enableLogstash && isAfter(iTestResult) && !iTestResult.getMethod().isAfterClassConfiguration() && !iTestResult.getMethod().isAfterSuiteConfiguration()) {
            testName = testMethodName;
        }
        write2LogFile(iTestResult, DumpUtils.createTestFolder(testName, suiteName));

        if (isAfter(iTestResult)) {
            if(enableLogstash){
                if(process != null){
                    killLogstashAgent(1, logstashLogPath);
                }
                if(process2 != null && iTestResult.getMethod().isAfterClassConfiguration()){
                    killLogstashAgent(2, logstashLogPath2);
                }
            }
        }
    }
	
	@Override
    public void onConfigurationSkip(ITestResult iTestResult) {
        super.onConfigurationFailure(iTestResult);
        String testName = iTestResult.getTestClass().getName();
        String configurationName = iTestResult.getMethod().toString().split("\\(|\\)")[0];
        if (!enableLogstash && isAfter(iTestResult)) {
        	DumpUtils.copyBeforeConfigurationsLogToTestDir(testName, suiteName);
        	testName = testMethodName;
        }
        LogUtils.log("Configuration Skipped: " + configurationName, iTestResult.getThrowable());

        if(enableLogstash && iTestResult.getMethod().isBeforeClassConfiguration()){
            initLogstash2(iTestResult);
        }

        ZipUtils.unzipArchive(testMethodName, suiteName);

        if (enableLogstash && isAfter(iTestResult) && !iTestResult.getMethod().isAfterClassConfiguration() && !iTestResult.getMethod().isAfterSuiteConfiguration()) {
            testName = testMethodName;
        }
        write2LogFile(iTestResult, DumpUtils.createTestFolder(testName, suiteName));

        if (isAfter(iTestResult)) {
            if(enableLogstash){
                if(process != null){
                    killLogstashAgent(1, logstashLogPath);
                }
                if(process2 != null && iTestResult.getMethod().isAfterClassConfiguration()){
                    killLogstashAgent(2, logstashLogPath2);
                }
            }
        }
    }

    @Override
    public void onTestStart(ITestResult iTestResult) {
    	super.onTestStart(iTestResult);
        String testName = TestNGUtils.constructTestMethodName(iTestResult);
        LogUtils.log("Test Start: " + testName);
        if(enableLogstash){
            initLogstash(testName);
        }
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        if (suiteName.toLowerCase().contains("webui")){
            if(testInvocationCounter < maxCount) {
                testInvocationCounter++;
                iTestResult.setAttribute("retry", true);
            }
            else {
                LogUtils.log("Number of retries expired.");
                iTestResult.setStatus(ITestResult.FAILURE);
                // reset count
                testInvocationCounter = 1;
                testMethodName = TestNGUtils.constructTestMethodName(iTestResult);
                LogUtils.log("Test Failed: " + testMethodName, iTestResult.getThrowable());
                write2LogFile(iTestResult, DumpUtils.createTestFolder(testMethodName, suiteName));
            }
        }
        else {
            testMethodName = TestNGUtils.constructTestMethodName(iTestResult);
            LogUtils.log("Test Failed: " + testMethodName, iTestResult.getThrowable());
            write2LogFile(iTestResult, DumpUtils.createTestFolder(testMethodName, suiteName));
        }
        super.onTestFailure(iTestResult);
    }

    @Override
	public void onTestSkipped(ITestResult iTestResult) {
		super.onTestSkipped(iTestResult);
		testMethodName = TestNGUtils.constructTestMethodName(iTestResult);
		LogUtils.log("Test Skipped: " + testMethodName, iTestResult.getThrowable());
        write2LogFile(iTestResult, DumpUtils.createTestFolder(testMethodName, suiteName));
	}

	@Override
    public void onTestSuccess(ITestResult iTestResult) {
        super.onTestSuccess(iTestResult);
        if (suiteName.toLowerCase().contains("webui")){
            testInvocationCounter = 1;
        }
        testMethodName = TestNGUtils.constructTestMethodName(iTestResult);
        LogUtils.log("Test Passed: " + testMethodName);
        write2LogFile(iTestResult, DumpUtils.createTestFolder(testMethodName, suiteName));
    }

    @Override
    public void onFinish(ITestContext testContext) {
        super.onFinish(testContext);
        LogUtils.log("Finishing Suite: "+suiteName.toLowerCase());
        if (suiteName.toLowerCase().contains("webui")){
            onFinishWebUITests(testContext);
        }
    }

    private void onFinishWebUITests(ITestContext testContext){
        // List of test results which we will delete later because of duplication or because the test eventually passed
        List<ITestResult> testsToBeRemoved = new ArrayList<ITestResult>();

        // collect all id's from passed test
        Set <Integer> passedTestIds = new HashSet<Integer>();
        for (ITestResult passedTest : testContext.getPassedTests().getAllResults()) {
            passedTestIds.add(getTestId(passedTest));
        }

        Set <Integer> failedTestIds = new HashSet<Integer>();
        for (ITestResult failedTest : testContext.getFailedTests().getAllResults()) {

            int failedTestId = getTestId(failedTest);
            // if this test failed before mark as to be deleted
            // or delete this failed test if there is at least one passed version
            if (failedTestIds.contains(failedTestId) || passedTestIds.contains(failedTestId)) {
                testsToBeRemoved.add(failedTest);
            } else {
                failedTestIds.add(failedTestId);
            }
        }
        // finally delete all tests that are marked
        for (Iterator<ITestResult> iterator = testContext.getFailedTests().getAllResults().iterator(); iterator.hasNext(); ) {
            ITestResult testResult = iterator.next();
            if (testsToBeRemoved.contains(testResult)) {
                iterator.remove();
            }
        }
    }

    // returns an ID for each test result
    private int getTestId(ITestResult result) {
        int id = result.getTestClass().getName().hashCode();
        id = 31 * id + result.getMethod().getMethodName().hashCode();
        id = 31 * id + (result.getParameters() != null ? Arrays.hashCode(result.getParameters()) : 0);
        return id;
    }

    private void write2LogFile(ITestResult iTestResult, File testFolder) {
        try {
            if(testFolder == null){
                LogUtils.log("Can not write to file test folder is null");
                return;
            }
            String parameters = TestNGUtils.extractParameters(iTestResult);
            File testLogFile = new File(testFolder.getAbsolutePath() + "/" + iTestResult.getName() + "(" + parameters + ").log");
            if (!testLogFile.createNewFile()) {
                new RuntimeException("Failed to create log file [" + testLogFile + "];\n log output: " + Reporter.getOutput());
            }
            FileWriter fstream = new FileWriter(testLogFile);
            BufferedWriter out = new BufferedWriter(fstream);
            String output = SGTestNGReporter.getOutput();
            out.write(output);
            out.close();
        } catch (Exception e) {
            new RuntimeException(e);
        } finally {
            SGTestNGReporter.reset();
        }
    }
    
    private boolean isAfter(ITestResult iTestResult) {
    	ITestNGMethod method = iTestResult.getMethod();
    	return (
    			method.isAfterClassConfiguration() || 
    			method.isAfterMethodConfiguration() || 
    			method.isAfterSuiteConfiguration() || 
    			method.isAfterTestConfiguration()
    	);
    }

    private void killLogstashAgent(int logAgentNumber, String logstashLogPath) {

        FileObject listendir;
        CustomFileListener listener = new CustomFileListener();
        long TIMEOUT_BETWEEN_FILE_QUERYING = 1000;
        long LOOP_TIMEOUT_IN_MILLIS = 10 * 1000;

        try {
            FileSystemManager fileSystemManager = VFS.getManager();
            listendir = fileSystemManager.resolveFile(logstashLogPath);
        } catch (FileSystemException e) {
            e.printStackTrace();
            return;
        }

        DefaultFileMonitor fm = new DefaultFileMonitor(listener);
        fm.setRecursive(true);
        fm.addFile(listendir);
        fm.setDelay(TIMEOUT_BETWEEN_FILE_QUERYING);
        fm.start();

        LogUtils.log("waiting to destroy logger");
        long startTimeMillis = System.currentTimeMillis();

        while(true){

            if(!listener.isProcessUp()){
                break;
            }

            listener.setProcessUp(false);

            try {
                TimeUnit.MILLISECONDS.sleep(LOOP_TIMEOUT_IN_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        long endTimeMillis = System.currentTimeMillis();
        LogUtils.log("destroying logstash agent " + logAgentNumber);
        LogUtils.log("waited " + (endTimeMillis - startTimeMillis)/1000 + " seconds");
        fm.stop();

        File logstashOutputFile = new File(logstashLogPath);

        if(logAgentNumber == 1){

            process.destroy();
            process = null;
        }
        else{

            process2.destroy();
            process2 = null;
        }

        try {
            TimeUnit.SECONDS.sleep(5);

            LogUtils.log("returning logstash config file to initial state");
            if(logAgentNumber == 1){
//                IOUtils.replaceFileWithMove(new File(confFilePath), new File(backupFilePath));
//                FileUtils.deleteQuietly(new File(backupFilePath));
            }
            else{
//                IOUtils.replaceFileWithMove(new File(confFilePath2), new File(backupFilePath2));
//                FileUtils.deleteQuietly(new File(backupFilePath2));

            }
//        } catch (IOException e) {
//            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        if(logstashOutputFile.exists()){
//            FileUtils.deleteQuietly(logstashOutputFile);
//        }
    }

    private void initLogstashHost(){

        if(logstashHost != null){
            return;
        }

        Properties props;
        try {
            props = IOUtils.readPropertiesFromFile(propsFile);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed reading properties file : " + e.getMessage());
        }
        logstashHost = props.getProperty("logstash_server_host");
    }

}
