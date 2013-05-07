package iTests.framework.utils;

import com.j_spaces.kernel.PlatformVersion;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.control.CompilationFailedException;
import org.testng.Assert;

import javax.xml.stream.*;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptUtils {

    /**
     * this method should be used to forcefully kill windows processes by pid
     * @param pid - the process id of witch to kill
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int killWindowsProcess(int pid) throws IOException, InterruptedException {
        String cmdLine = "cmd /c call taskkill /pid " + pid + " /F";
        String[] parts = cmdLine.split(" ");
        ProcessBuilder pb = new ProcessBuilder(parts);
        LogUtils.log("Executing Command line: " + cmdLine);
        Process process = pb.start();
        int result = process.waitFor();
        return result;
    }

//	public static void killLinuxProcess(long pid, String hostName) {
//		SSHUtils.runCommand(hostName, WANemUtils.SSH_TIMEOUT, "kill -9 " + pid , WANemUtils.SSH_USERNAME, WANemUtils.SSH_PASSWORD);
//	}

    @SuppressWarnings("deprecation")
    public static String runScriptRelativeToGigaspacesBinDir(String args, long timeout) throws Exception {
        RunScript runScript = new RunScript(args, true);
        Thread script = new Thread(runScript);
        script.start();
        Thread.sleep(timeout);
        script.stop();

        runScript.kill();
        return runScript.getScriptOutput();
    }

    public static String runScriptWithAbsolutePath(String args, long timeout) throws Exception {
        RunScript runScript = new RunScript(args, false);
        Thread script = new Thread(runScript);
        script.start();
        script.join(timeout);
        runScript.kill();
        return runScript.getScriptOutput();
    }

    public static RunScript runScriptWithAbsolutePath(String args) {
        RunScript runScript = new RunScript(args, false);
        Thread script = new Thread(runScript);
        script.start();
        return runScript;
    }

    public static RunScript runScriptRelativeToGigaspacesBinDir(String args) throws Exception {
        RunScript runScript = new RunScript(args, true);
        Thread script = new Thread(runScript);
        script.start();
        return runScript;
    }

    @SuppressWarnings("deprecation")
    public static String runScript(String args, long timeout) throws Exception {
        RunScript runScript = new RunScript(args);
        Thread script = new Thread(runScript);
        script.start();
        Thread.sleep(timeout);
        script.stop();

        runScript.kill();
        return runScript.getScriptOutput();
    }

    public static RunScript runScript(String args) throws Exception {
        RunScript runScript = new RunScript(args);
        Thread script = new Thread(runScript);
        script.start();
        return runScript;
    }

    public static class RunScript implements Runnable {

        private final String[] args;
        private String scriptFilename;
        private String binPath;
        private final StringBuilder sb = new StringBuilder();
        private Process process;

        public RunScript(String args) {
            this.args = args.split(" ");
            scriptFilename = this.args[0];
            scriptFilename += getScriptSuffix();
            binPath = getBuildBinPath();
            this.args[0] = binPath + "/" + scriptFilename;
        }

        public RunScript(String args, boolean relativeToGigaspacesBinDir) {
            this.args = args.split(" ");
            if (relativeToGigaspacesBinDir) {
                scriptFilename = this.args[0];
                scriptFilename += getScriptSuffix();
                binPath = getBuildBinPath();
                this.args[0] = binPath + "/" + scriptFilename;
            }
        }

        public void run() {
            String line;
            ProcessBuilder pb = new ProcessBuilder(args);
            if (binPath != null) {
                pb.directory(new File(binPath));
            }
            pb.redirectErrorStream(true);

            try {
                process = pb.start();
                InputStreamReader isr = new InputStreamReader(process.getInputStream());
                BufferedReader br = new BufferedReader(isr);

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getScriptOutput() {
            return sb.toString();
        }

        public void kill() throws IOException, InterruptedException {
            String scriptOutput = sb.toString();

            //This regular expression captures the script's pid
            String regex = "(?:Log file:.+-)([0-9]+)(?:\\.log)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(scriptOutput);

            while (matcher.find()) {
                int pid = Integer.parseInt(matcher.group(1));
                String processIdAsString = Integer.toString(pid);
                try {
                    if (isLinuxMachine()) {
                        new ProcessBuilder("kill", "-9", processIdAsString).start().waitFor();
                    } else {
                        new ProcessBuilder("taskkill", "/F", "/PID", processIdAsString).start().waitFor();
                    }
                } catch (IOException e) {
                    LogUtils.log("caught IOException while teminating process", e);
                }
            }

            process.destroy();
        }
    }

    public static void startLocalProcess(String dir, String ... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command).directory(new File(URLDecoder.decode(dir, "ISO-8859-1")));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String line;
        InputStream stdout = p.getInputStream ();
        BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
        while ((line = reader.readLine ()) != null) {
            System.out.println(line);
        }

    }

    public static String getScriptSuffix() {
        return isLinuxMachine() ? ".sh" : ".bat";
    }

    public static boolean isLinuxMachine() {
        return !isWindows();
    }

    public static String getBuildPath() {
        String referencePath = getClassLocation(PlatformVersion.class);
        String jarPath = referencePath.split("!")[0];
        int startOfPathLocation = jarPath.indexOf("/");
        if (isLinuxMachine()) {
            jarPath = jarPath.substring(startOfPathLocation);
        }
        else {
            jarPath = jarPath.substring(startOfPathLocation + 1);
        }
        int startOfJarFilename = jarPath.indexOf("gs-runtime.jar");
        jarPath = jarPath.substring(0, startOfJarFilename);
        jarPath += "../..";
        return jarPath;
    }

    public static String getBuildBinPath() {
        return getBuildPath() + File.separator + "bin";
    }

    public static String getBuildRecipesPath() {
        return getBuildPath() + "/recipes";
    }

    public static String getBuildRecipesServicesPath() {
        return getBuildRecipesPath() + "/services";
    }

    public static String getBuildRecipesApplicationsPath() {
        return getBuildRecipesPath() + "/apps";
    }

    public static String getClassLocation(Class<?> clazz) {
        String clsAsResource = clazz.getName().replace('.', '/').concat(".class");

        final ClassLoader clsLoader = clazz.getClassLoader();

        URL result = clsLoader != null ? clsLoader.getResource(clsAsResource) :
            ClassLoader.getSystemResource(clsAsResource);
        return result.getPath();
    }

    public static File getGigaspacesZipFile() throws IOException {

        File gigaspacesDirectory = new File ( ScriptUtils.getBuildPath());
        String gigaspacesFolderName = "gigaspaces-" + PlatformVersion.getEdition() + "-" + PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone();
        String gigaspacesZipFilename = gigaspacesFolderName + "-b" + PlatformVersion.getBuildNumber() + ".zip";
        LogUtils.log("zip name: " + gigaspacesZipFilename);
        File gigaspacesZip = new File(gigaspacesDirectory.getAbsoluteFile() +"/../" + gigaspacesZipFilename);
        if (!gigaspacesZip.exists()) {
            File[] excludeDirectories = new File[]{new File(gigaspacesDirectory,"work"),
                    new File(gigaspacesDirectory,"logs"),
                    new File(gigaspacesDirectory,"deploy"),
                    new File(gigaspacesDirectory,"clouds/ec2/upload")};
            File[] includeDirectories = new File[]{new File(gigaspacesDirectory,"deploy/templates")};
            File[] emptyDirectories = new File[]{new File(gigaspacesDirectory,"logs"),
                    new File(gigaspacesDirectory,"lib/platform/esm")};
            ZipUtils.zipDirectory(gigaspacesZip, gigaspacesDirectory, includeDirectories, excludeDirectories, emptyDirectories, gigaspacesFolderName);
        }

        if (!ZipUtils.isZipIntact(gigaspacesZip)) {
            throw new IOException(gigaspacesZip + " is not a valid zip file.");
        }

        return gigaspacesZip.getCanonicalFile();
    }

    //////////////////////

    public static class ScriptPatternIterator implements Iterator<Object[]> {

        private final InputStream is;
        private final XMLStreamReader parser;
        private int event;

        public ScriptPatternIterator(String xmlPath) throws XMLStreamException, FactoryConfigurationError, IOException {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlPath);
            parser = XMLInputFactory.newInstance().createXMLStreamReader(is);
            nextScript();
        }

        public boolean hasNext() {
            return (event != XMLStreamConstants.END_DOCUMENT);
        }

        public Object[] next() {
            String currentScript = parser.getAttributeValue(null, "name");
            long timeout = Long.parseLong(parser.getAttributeValue(null, "timeout"));

            List<String[]> patterns = new ArrayList<String[]>();

            try {
                nextPattern();
            } catch (XMLStreamException e1) {
                e1.printStackTrace();
            }
            while (event != XMLStreamConstants.END_ELEMENT ||
                    !parser.getName().toString().equals("script")) {

                String currentRegex = parser.getAttributeValue(null, "regex");
                String currentExpectedAmount = parser.getAttributeValue(null, "expected-amount");

                patterns.add(new String[] { currentRegex, currentExpectedAmount });

                try {
                    nextPattern();
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                }
            }

            String[][] patternObjects = new String[patterns.size()][];
            for (int i = 0; i < patterns.size(); i++) {
                patternObjects[i] = patterns.get(i);
            }

            try {
                nextScript();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new Object[] { currentScript, patternObjects , timeout };
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }


        private void nextScript() throws XMLStreamException, IOException {
            while (true) {
                event = parser.next();
                if (event == XMLStreamConstants.END_DOCUMENT) {
                    parser.close();
                    is.close();
                    return;
                }
                if (event == XMLStreamConstants.START_ELEMENT
                        && parser.getName().toString().equals("script")) {
                    return;
                }
            }
        }

        private void nextPattern() throws XMLStreamException {
            while (true) {
                event = parser.next();
                if (event == XMLStreamConstants.END_ELEMENT
                        && parser.getName().toString().equals("script")) {
                    return;
                }
                if (event == XMLStreamConstants.START_ELEMENT
                        && parser.getName().toString().equals("pattern")) {
                    return;
                }
            }
        }

    }

    public static Map<String,String> loadPropertiesFromClasspath(String resourceName) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(resourceName);
        Properties properties = null;
        if (in != null) {
            properties = new Properties();
            properties.load(in);
        }
        return convertPropertiesToStringProperties(properties);
    }

    private static Map<String,String> convertPropertiesToStringProperties(Properties properties) {
        Map<String,String> stringProperties = new HashMap<String,String>();
        for (Object key : properties.keySet()) {
            stringProperties.put(key.toString(),properties.get(key).toString());
        }
        return stringProperties;
    }

    public static Map<String, String> loadPropertiesFromFile(File file) throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(file));
        return convertPropertiesToStringProperties(properties);
    }

    public static GroovyObject getGroovyObject(String className) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException{
        ClassLoader parent = ScriptUtils.class.getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(className);
        if (is == null) {
            Assert.fail("Failed finding " + className);
        }
        try {
            Class<?> groovyClass = loader.parseClass(convertStreamToString(is));
            return (GroovyObject) groovyClass.newInstance();
        } finally {
            is.close();
        }
    }

    public static String convertStreamToString(InputStream is) {
        String ans = "";
        Scanner s = new Scanner(is);
        s.useDelimiter("\\A");
        ans = s.hasNext() ? s.next() : "";
        s.close();
        return ans;
    }

    /**
     * @param className the name of the .groovy file with path starting from sgtest base dir
     * @param methodName the name of the method in className we wnt to invoke
     * @throws org.codehaus.groovy.control.CompilationFailedException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static void runGroovy(String className, String methodName) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException{
        GroovyObject obj = getGroovyObject(className);
        Object[] args = {};
        obj.invokeMethod(methodName, args);
    }

    /**
     * @param className  the name of the .groovy file with path starting from sgtest base dir
     * @param methodName the name of the method in className we wnt to invoke
     * @param args the arguments for the method to invoke
     * @throws org.codehaus.groovy.control.CompilationFailedException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static void runGroovy(String className, String methodName, Object...args) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException{
        GroovyObject obj = getGroovyObject(className);
        obj.invokeMethod(methodName, args);
    }

    /**
     * this method runs a .groovy file with a relative path to current working directory
     * use example: <code>ScriptUtils.runGroovy("src/test/maven/copyMuleJars.groovy")</code>
     *
     * @param className the name of the .groovy file with path starting from current working directory
     * @throws org.codehaus.groovy.control.CompilationFailedException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static void runGroovy(String className) throws CompilationFailedException, IOException, InstantiationException, IllegalAccessException{
        GroovyObject obj = getGroovyObject(className);
        Object[] args = {};
        obj.invokeMethod("run", args);
    }

    public static boolean isWindows() {
        return (System.getenv("windir") != null);
    }

}
