package iTests.framework.utils;

import com.gigaspaces.internal.dump.heap.HeapDumpProcessor;
import com.gigaspaces.internal.dump.log.LogDumpProcessor;
import com.gigaspaces.internal.dump.pu.ProcessingUnitsDumpProcessor;
import com.gigaspaces.internal.dump.summary.SummaryDumpProcessor;
import com.gigaspaces.internal.dump.thread.ThreadDumpProcessor;
import com.j_spaces.kernel.PlatformVersion;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import iTests.framework.tools.SGTestHelper;
import org.apache.commons.io.FileUtils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.dump.DumpResult;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DumpUtils {

    private static File testFolder;
    private static File zipFile;
    private static File buildFolder;
    private static final boolean enableLogstash = Boolean.parseBoolean(System.getProperty("iTests.enableLogstash", "false"));

    public static void dumpALL(Admin admin) {
        dump(admin, null, getAllDumpOptions());
    }

    public static void dumpLogs(Admin admin) {
        dump(admin, null, LogDumpProcessor.NAME);
    }

    public static void dumpThreads(Admin admin) {
        dump(admin, null, ThreadDumpProcessor.NAME);
    }

    public static void dumpHeap(Admin admin) {
        dump(admin, null, HeapDumpProcessor.NAME);
    }

    public static void dumpProcessingUnit(Admin admin) {
        dump(admin, null, ProcessingUnitsDumpProcessor.NAME);
    }

    public static void dump(Admin admin, String cause, String... dumpOptions) {

        if (SGTestHelper.isDevMode()) {
            return;
        }

        try {
            DumpResult result = admin.generateDump(cause, null, dumpOptions);
            Date date = new Date();
            DateFormat date1 = new SimpleDateFormat("dd-MM-yyyy");
            DateFormat hour = new SimpleDateFormat("HH-mm-ss.SSS");

            //add suffix so we can distinguish between two separate calls.
            String fileSuffix;
            if (dumpOptions.length == 1) {
                fileSuffix = dumpOptions[0];
            } else {
                fileSuffix = "all" + dumpOptions.length;
            }

            zipFile = new File(getTestFolder().getAbsolutePath() + "/" + date1.format(date) + "_" + hour.format(date) + "_" + fileSuffix + "_dump.zip");
            result.download(zipFile, null);
            if (!zipFile.exists()) {
                throw new FileNotFoundException(zipFile.getPath() + " cannot be found. Probably dump download failed.");
            }
            LogUtils.log("> Logs: " + zipFile.getAbsolutePath() + "\n");

        } catch (Throwable t) {
            LogUtils.log("Dump Failed", t);
        }
    }

    private static String[] getAllDumpOptions() {
        List<String> dumpOptionsList = new ArrayList<String>(5);
        dumpOptionsList.add(SummaryDumpProcessor.NAME);
        dumpOptionsList.add(ThreadDumpProcessor.NAME);
        dumpOptionsList.add(LogDumpProcessor.NAME);
        dumpOptionsList.add(ProcessingUnitsDumpProcessor.NAME);
        dumpOptionsList.add(HeapDumpProcessor.NAME);
        String[] dumpOptionsArray = new String[dumpOptionsList.size()];
        dumpOptionsList.toArray(dumpOptionsArray);
        return dumpOptionsArray;
    }

    public static File createTestFolder(String testName, String suiteName) {
        String buildNumber = PlatformVersion.getBuildNumber();
        if (buildNumber == null) {
            return null;
        }

        buildFolder = new File(SGTestHelper.getSGTestRootDir() + "/../");

        if (!buildFolder.exists())
            buildFolder.mkdir();
        testFolder = new File(buildFolder.getAbsolutePath() + "/" + suiteName + "/" + testName);
        if (!testFolder.exists()) {
            testFolder.mkdirs();
            LogUtils.log("Folder created : --> " + testFolder.getAbsolutePath());
        }

        return testFolder;
    }

    public static File getTestFolder() {
        if (testFolder == null) {
            File buildFolder = new File(SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" +
                    PlatformVersion.getBuildNumber());
            testFolder = new File(buildFolder.getAbsolutePath());
            testFolder.mkdirs();
        }
        return testFolder;
    }

    public static File getDumpFile() {
        return zipFile;
    }

    public static void unzipDump() {
        ZipUtils.unzipArchive(zipFile, testFolder);
    }

    public static void copyBeforeConfigurationsLogToTestDir(String testName, String suiteName) {

        if(!enableLogstash){
            if (buildFolder == null) {
                return;
            }
            File beforeConfigurationsLogDir = new File(buildFolder.getAbsolutePath() + "/" + suiteName + "/" + testName);
            if (beforeConfigurationsLogDir.exists()) {
                try {
                    LogUtils.log("Copying files from source dir:" + beforeConfigurationsLogDir.getAbsolutePath() + " to target dir:" + testFolder.getAbsolutePath());
                    FileUtils.copyDirectory(beforeConfigurationsLogDir, testFolder);
                    LogUtils.log("Deleting directory:" + beforeConfigurationsLogDir.getAbsolutePath());
                    FileUtils.deleteDirectory(beforeConfigurationsLogDir);
                } catch (IOException e) {
                    LogUtils.log("Failed to copy before configurations to test dir : " + testFolder.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * Calls the specified callable. If it does not return within 1 minute, then a thread dump is generated (every minute)
     */
	public static <T> T dumpThreadsEveryMinute(final Admin admin, final Callable<T> callable) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "DumpScheduler thread");
                t.setDaemon(true);
                return t;
            }
        });
        executor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                if (!completed.get()) {
                    DumpUtils.dumpThreads(admin);
                    DumpUtils.dumpLocalThreads();
                }
            }
        }, 1, 1, TimeUnit.MINUTES);

        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            completed.set(true);
            executor.shutdown();
        }
    }
	
	protected static void dumpLocalThreads() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        try {
            new ThreadDumpProcessor().processAllThreads(pw);
        } catch (final Throwable t) {
            LogUtils.log("Failed to generate local thread dump", t);
        }
        pw.flush();
        LogUtils.log("Local Thread Dump:" + sw.toString());
    }

    public static HistoClass getClassCountFromDumpHisto(int pid, String className) {
        String value = dumpHeapHisto(pid);
        StringTokenizer tokenizer = new StringTokenizer(value, "\n");
        tokenizer.nextToken();
        tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            String itStr = tokenizer.nextToken();
            if (itStr.contains("Total")) {
                LogUtils.log("Class: " + className + " is not found on this JVM PID  v [ " + pid + " ]");
                return null;
            }
            StringTokenizer innerTokenizer = new StringTokenizer(itStr, " ");
            while (innerTokenizer.hasMoreTokens()) {
                String indexStr = innerTokenizer.nextToken();
                String index = indexStr.substring(0, indexStr.length() - 1);
                String instances = innerTokenizer.nextToken();
                String totalSizeInBytes = innerTokenizer.nextToken();
                String clazzName = innerTokenizer.nextToken();
                HistoClass histoClass = new HistoClass(Integer.valueOf(index), Integer.valueOf(instances),
                        Integer.valueOf(totalSizeInBytes), clazzName);
                if (histoClass.getClassName().equalsIgnoreCase(className)) {
                    return histoClass;
                }
            }
        }
        LogUtils.log("Class: " + className + " is not found on this JVM PID[ " + pid + " ]");
        return null;
    }

    public static String dumpHeapHisto(int pid) {
        try {
            ClassLoader parent = Thread.currentThread().getContextClassLoader();
            GroovyClassLoader loader = new GroovyClassLoader(parent);

            Class groovyClass = loader.parseClass(new File("src/main/resources/test/HeapDumpUtils.groovy"));
            GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();

            Object[] args = {String.valueOf(pid)};
            return (String) groovyObject.invokeMethod("heapHisto", args);
        } catch (Exception e) {
            LogUtils.log("Failed to generate memory histo heap dump", e);
        }
        return null;
    }

    static class HistoClass {
        int index;
        int instances;
        int totalSizeInBytes;
        String className;

        HistoClass() {
        }

        HistoClass(int index, int instances, int totalSizeInBytes, String className) {
            this.index = index;
            this.instances = instances;
            this.totalSizeInBytes = totalSizeInBytes;
            this.className = className;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getInstances() {
            return instances;
        }

        public int getTotalSizeInBytes() {
            return totalSizeInBytes;
        }

        public String getClassName() {
            return className;
        }
    }

//    public static void main(String[] args) throws IOException {
//    	try {
//			dumpMachines("http://15.185.169.193:8100");
//			System.out.println("sdf");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        DateFormat date1 = new SimpleDateFormat("dd-MM-yyyy");
//        DateFormat hour = new SimpleDateFormat("HH-mm-ss-SSS");
//        Date date = new Date();
//        zipFile = new File(getTestFolder().getAbsolutePath() + "/" + date1.format(date) + "_" + hour.format(date) + "_dump.zip");
//        zipFile.createNewFile();
//
//    }

}
