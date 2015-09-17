package iTests.framework.utils;

import com.gigaspaces.logger.GSLogConfigLoader;
import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.WebConstants;
import com.j_spaces.kernel.PlatformVersion;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;
import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import iTests.framework.utils.ScriptUtils.RunScript;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.Assert;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static iTests.framework.utils.AdminUtils.loadGSM;
import static iTests.framework.utils.LogUtils.log;

/**
 * use this class to add webui testing capabilities to a test.
 * 
 * <p> call close() when done.
 */
public class WebuiTestUtils{
	
	private final String scriptName = "../tools/gs-webui/gs-webui";
	private final static String baseUrlApache = "http://localhost:" + System.getenv("apache.port")  + "/gs-webui/";
	private final static String apachelb = "../tools/apache/apache-lb-agent -apache " + '"' + System.getenv("apache.home") + '"';

	public final static String LOCALHOST_PREFIX = "http://localhost:";
	public final static int DEFAULT_PORT = 8099;
	public final static String baseUrl = LOCALHOST_PREFIX + DEFAULT_PORT;
	private final static String baseReverseProxyUrl = "http://localhost/reverse-proxy-testing/Gs_webui.html";
	public final static String originalAlertXml = SGTestHelper.getSGTestRootDir() + "/src/main/resources/webui/alerts/alerts.xml";
	public final static int FIREFOX = 0;
	public final static int CHROME = 1;

	public final static String SUB_TYPE_CONTEXT_PROPERTY = "com.gs.service.type";
	public final static String APPLICATION_CONTEXT_PROPERY = "com.gs.application";
	public final static String DEPENDS_ON_CONTEXT_PROPERTY = "com.gs.application.dependsOn";
	private final static String LICENSE_PATH = SGTestHelper.getBuildDir() + "/gslicense.xml";

	private Admin admin;
	private static long waitingTime = 30000;

	private RunScript scriptWebUI;
	private RunScript scriptLoadBalancer;
	private WebDriver driver;
	private Selenium selenium;
	private ProcessingUnit webSpace;
	private GridServiceManager webUIGSM;
	private ProcessingUnit gswebui;

    private final String browser = System.getProperty("selenium.browser");
    private final String defaultBrowser = (browser != null) ? browser : "Firefox";

	private List<Selenium> seleniumBrowsers = new ArrayList<Selenium>();
	private ChromeDriverService chromeService;
	
	private int port;

    public WebuiTestUtils() throws Exception {
		startup(true);
	}

	
	public WebuiTestUtils( int port, boolean startWebBrowser ) throws Exception {
		this.port = port;
		startup( startWebBrowser );
	}	

	public WebuiTestUtils( String url ) throws Exception {
		this(url, true);
	}	
	
	public WebuiTestUtils(String url,boolean startWebBrowser) throws Exception {
		startup(url, startWebBrowser);
	}

	public WebuiTestUtils(String url,boolean startWebBrowser, boolean cleanup) throws Exception {
		startup(url, startWebBrowser, cleanup);
	}

	private void startup(boolean startWebBrowser) throws Exception{
		startup(null, startWebBrowser);
	}

	private void startup(String url, boolean startWebBrowser) throws Exception{
		startup(url, startWebBrowser, true);
	}

	private void startup(String url, boolean startWebBrowser, boolean cleanup) throws Exception{
		if(cleanup){
            beforeSuite();
            beforeTest();
        }
		startWebServices(url, startWebBrowser);
		if( !startWebBrowser ){
			Thread.sleep( 3000 );
		}
	}

	public void close() throws Exception{
		killWebServices();
        if (admin != null) {
            admin.close();
            admin = null;
        }
	}

	public GridServiceManager getWebuiManagingGsm() {
		return webUIGSM;
	}
	
	public boolean isStartWebServerFromBatch() {
		return true;
	}

	/**
	 * temporary workaround because of GS-10075.
	 * this bug manifests only on a windows machine for some reason and frequently makes all the test fail on assertCleanSetup.
	 */

	public void beforeTest() {
		LogUtils.log("Test Configuration Started: " + this.getClass());
		LogUtils.log("Creating new Admin");
		admin = AdminUtils.createAdmin();
		try {
			LogUtils.log("Waiting for cleanup to finish");
			SetupUtils.assertCleanSetup(admin);
		}
		catch (AssertionError e) {
			int gridServiceManagersSize = admin.getGridServiceManagers().getSize();
			if (gridServiceManagersSize > 0) { // there is a leaked GSM
				LogUtils.log("Detected a leaked GSM that was not properly killed");
				GridServiceManagers gridServiceManagers = admin.getGridServiceManagers();
				GridServiceManager gridServiceManager = gridServiceManagers.getManagers()[0];
				Machine[] machines = admin.getMachines().getMachines();
				String gsmMachineHostAddress = gridServiceManager.getMachine().getHostAddress();
				long gsmPID = gridServiceManager.getVirtualMachine().getDetails().getPid();
				LogUtils.log("trying to forcefully kill GSM with PID " + gsmPID + " using native OS command");
				try {
					if (gsmMachineHostAddress.equals(InetAddress.getLocalHost().getHostAddress())) {
						killLocalProcessByPID(gsmPID, 5000);
					}
					else {
						for (Machine machine : machines) {
							if (gsmMachineHostAddress.equals(machine.getHostAddress())) {
								killRemoteProcessByPID(machine.getHostAddress(), gsmPID, 10 * 1000);
								LogUtils.log("Succesfully killed process with pid " + gsmMachineHostAddress);
								break;
							}
						}
					}
				} catch (UnknownHostException e1) {
					LogUtils.log("caugh an exception", e1);
				}
			}
			else { // if no leak was found, propogate the exception out.
				Assert.fail(e.getMessage(), e);
			}
		}
	}

	public void killRemoteProcessByPID(String hostAddress, long pid, long timeoutInMillis) {
		String pathToPsExec = SGTestHelper.getSGTestRootDir() + "\\src\\test\\webui\\resources\\psexec.exe";
		String host = "\\\\" + hostAddress;
		String credentials = "-u GSPACES\\ca -p password -c -f";
		String pathToKillByIdScript = SGTestHelper.getSGTestRootDir() + "\\deploy\\bin\\windows\\kill_by_pid.bat";
		String fullCommand = pathToPsExec + " " + host + " " + credentials + " " + pathToKillByIdScript + " " + pid;
		try {
			String runScriptWithAbsolutePath = ScriptUtils.runScriptWithAbsolutePath(fullCommand, timeoutInMillis);
			LogUtils.log(runScriptWithAbsolutePath);
		} catch (Exception e) {
			LogUtils.log("Failed killing Process with pid " + pid + " on host " + hostAddress, e);
		}
	}

	public void killLocalProcessByPID(long pid, long timeoutInMillis) {
		try {
			String pathToKillByIdScript = SGTestHelper.getSGTestRootDir() + "\\deploy\\bin\\windows\\kill_by_pid.bat";
			String fullCommand = pathToKillByIdScript + " " + pid;
			String runScriptWithAbsolutePath = ScriptUtils.runScriptWithAbsolutePath(fullCommand, timeoutInMillis);
			LogUtils.log(runScriptWithAbsolutePath);
		}
		catch (Exception e) {
			LogUtils.log("Failed killing Process with pid " + pid + " on localhost", e);
		}
	}

	public void beforeSuite() throws IOException {
		GSLogConfigLoader.getLoader("webuitf");
		LogUtils.log("default browser is : " + defaultBrowser);
	}

	/**
	 * starts the web-ui browser from the batch file in gigaspaces
	 * also opens a browser and connect to the server
	 * @throws Exception 
	 */
	public void startWebServices( boolean startWebBrowser ) throws Exception { 
		startWebServices(null, startWebBrowser);
	}

	/**
	 * starts the web-ui browser from the batch file in gigaspaces
	 * also opens a browser and connect to the server
	 * @throws Exception
	 */
	public void startWebServices(String url, boolean startWebBrowser ) throws Exception {
		if (isStartWebServerFromBatch()) {
			startWebServer();

			if( startWebBrowser ){
				if(url == null){
	                String isReverseProxy = System.getProperty("reverse.proxy");
	                url = baseUrl;
	                if(isReverseProxy != null && isReverseProxy.equals("true")){
	                    url = baseReverseProxyUrl;
	                }
				}
				LogUtils.log("starting web browser with url " + url);
				startWebBrowser(url);
			}
		}
		else {
			replaceBalancerTemplate();
			startLoadBalancerWebServer();
			if( startWebBrowser ){
				startWebBrowser(baseUrlApache);
			}
		}

	}

	/**
	 * stops the server and kills all open browsers
	 * @throws InterruptedException 
	 * @throws java.io.IOException
	 */
	public void killWebServices() throws IOException, InterruptedException {
		try {
			if (isStartWebServerFromBatch()) {
				stopWebServer();
			}
			else {
				stopLoadBalancerWebServer();
			}
			stopWebBrowser();
		}
		finally {
			restorePreviousBrowser();
		}
	}

	
	public void startWebServer() throws Exception {
		LogUtils.log( 
			"Starting webui server on port [" + ( ( port > 0 ) ? port : DEFAULT_PORT ) + "]..." );
		scriptWebUI = ScriptUtils.runScriptRelativeToGigaspacesBinDir( 
					scriptName + ( ( port > 0 ) ? " -port " + String.valueOf( port ) : "" ) );
	}

	public void stopWebServer() throws IOException, InterruptedException {

		LogUtils.log("writing webui logs file to test folder");
		File testFolder = DumpUtils.getTestFolder();
		if (testFolder != null) {
			FileUtils.write(new File(testFolder.getAbsolutePath() + "/webui.log"), scriptWebUI.getScriptOutput());
		}

		LogUtils.log("Killing web server...");
		if (scriptWebUI != null) {
			scriptWebUI.kill();
		}


	}

	private WebDriver initializeWebDriver() {
		LogUtils.log("Launching browser...");
		WebDriver driver = null;
		for (int i = 0 ; i < 3 ; i++) {
			try {
				if (browser == null) {
					driver = createFirefoxDriver();
				}
				else {
					if (browser.equals("Firefox")) {
						driver = createFirefoxDriver();
					}
					else {
						if (browser.equals("IE")) {
                            if(System.getProperty("webdriver.ie.driver") == null){
                                System.setProperty("webdriver.ie.driver", DefaultValues.get().getIEDriverPath());
                            }
							DesiredCapabilities desired = DesiredCapabilities.internetExplorer();
							desired.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
							driver = new InternetExplorerDriver(desired);
						}
						else {
							DesiredCapabilities desired = DesiredCapabilities.chrome();
							desired.setCapability("chrome.switches", Arrays.asList("--start-maximized"));
							String chromeDriverBinaryPath = DefaultValues.get().getChromeDriverPath();
							chromeService = new ChromeDriverService.Builder().usingAnyFreePort().usingDriverExecutable(new File(chromeDriverBinaryPath)).build();
							LogUtils.log("Starting Chrome Driver Server...");
							chromeService.start();
							driver = new RemoteWebDriver(chromeService.getUrl(), desired);
						}
					}
				}
				LogUtils.log("Current browser is " + browser);
				break;

			}
			catch (Exception e) {
				LogUtils.log("Failed to lanch browser, retyring...Attempt number " + (i + 1), e);
			}
		}
		if (driver == null) {
			LogUtils.log("unable to lauch browser, test will fail on NPE");
		}
		return driver;
	}

	public static FirefoxDriver createFirefoxDriver(){

		String newmanSystemProperty = System.getProperty("com.gs.test.use.newman");
		LogUtils.log("WebuiTestsUtils, passed newmanSystemProperty=" + newmanSystemProperty);

		boolean useNewman =
				Boolean.parseBoolean( System.getProperty( "com.gs.test.use.newman", Boolean.FALSE.toString() ) );
		LogUtils.log("useNewman=" + useNewman);
		if( useNewman ) {
			LogUtils.log("--- createFirefoxDriver ---");
			String Xport = System.getProperty("lmportal.xvfb.id", ":1");
			LogUtils.log("Xport=" + Xport);
			//final File firefoxPath = new File( System.getProperty( "lmportal.deploy.firefox.path", "/usr/bin/firefox" ) );
			FirefoxBinary firefoxBinary = new FirefoxBinary(/*firefoxPath*/);
			LogUtils.log("FirefoxBinary initialized");
			firefoxBinary.setEnvironmentProperty("DISPLAY", Xport);
			LogUtils.log("After set DISPLAY port");

			return new FirefoxDriver(firefoxBinary, null);
		}

		return new FirefoxDriver();
	}

	private void startWebBrowser(String uRL) throws InterruptedException {
		String browser = this.browser;
		driver = initializeWebDriver();
		if (driver != null) {
			driver.get(uRL);
			if ((browser == null) || browser.equals("Firefox")) {
				maximize(); // this method is supported only on Firefox
			}
			selenium = new WebDriverBackedSelenium(driver, uRL);
			seleniumBrowsers.add(selenium);
			Thread.sleep(3000);
            if(uRL.contains(":" + DEFAULT_PORT)){
                waitForServerConnection(driver);
            }
		}
		else {
			Assert.fail("Failed to launch Browser");
		}
	}

	public void openBrowser(String url) throws InterruptedException {
		if( selenium != null ) {
			selenium.open(url);
			Thread.sleep(3000);
		}
		else {
			Assert.fail("Failed to open Browser with url [" + url + "]");
		}
	}
	
	
	private void waitForServerConnection(WebDriver driver) throws InterruptedException {
		int seconds = 0;
		while (seconds < 40) {
			try {
				driver.findElement(By.xpath(WebConstants.Xpath.loginButton));
				LogUtils.log("Web server connection established");
				break;
			}
			catch (NoSuchElementException e) {
				LogUtils.log("Unable to connect to Web server, retrying...Attempt number " + (seconds + 1));
				driver.navigate().refresh();
				Thread.sleep(1000);
				seconds++;
			}
		}
		if (seconds == 30) {
			LogUtils.log("Could not establish a connection to webui server, Test will fail");
		}

	}

	private void maximize() {
		driver.manage().window().setSize(new Dimension(1280, 1024));
	}

	public void stopWebBrowser() throws InterruptedException {
		LogUtils.log("Killing browser...");
		for (Selenium selenium : seleniumBrowsers) {
			selenium.stop();
			selenium = null;
			Thread.sleep(1000);
		}
		if (chromeService != null && chromeService.isRunning()) {
			LogUtils.log("Chrome Driver Server is still running, shutting it down...");
			chromeService.stop();
			chromeService = null;
		}
	}

	private void startLoadBalancerWebServer() throws Exception {

		log("launching web server");
		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		Machine machineA = gsaA.getMachine();

		log("loading GSM");
		webUIGSM = loadGSM(machineA);

		log("loading 2 gsc's on one machine");
		AdminUtils.loadGSCWithSystemProperty(machineA, "-Dorg.eclipse.jetty.level=ALL");
		AdminUtils.loadGSCWithSystemProperty(machineA, "-Dorg.eclipse.jetty.level=ALL");
		log("deploying the space");
		webSpace = webUIGSM.deploy(new SpaceDeployment("webSpace").numberOfInstances(1)
				.numberOfBackups(1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "gs-webui"));
		ProcessingUnitUtils.waitForDeploymentStatus(webSpace, DeploymentStatus.INTACT);

		log("launching web-ui server");
		final Pattern gsWebUIpattern = Pattern.compile("gs-webui.*\\.war");
		String gswebuiWarFileName = findJarFilenameByRegexPattern(ScriptUtils.getBuildPath() + "/tools/gs-webui", gsWebUIpattern);
		String gswebuiWar = ScriptUtils.getBuildPath() + "/tools/gs-webui/" + gswebuiWarFileName;
		ProcessingUnitDeployment webuiDeployment = new ProcessingUnitDeployment(new File(gswebuiWar)).numberOfInstances(2).numberOfBackups(0)
				.maxInstancesPerVM(1).setContextProperty("jetty.sessions.spaceUrl", "jini://*/*/webSpace").setContextProperty("com.gs.application", "gs-webui");
		gswebui = webUIGSM.deploy(webuiDeployment);
		ProcessingUnitUtils.waitForDeploymentStatus(gswebui, DeploymentStatus.INTACT);
		log("starting gigaspaces apache load balancer client with command : " + apachelb);
		scriptLoadBalancer = ScriptUtils.runScriptRelativeToGigaspacesBinDir(apachelb);
		Thread.sleep(5000);
		log(scriptLoadBalancer.getScriptOutput());
		log("apache load balancer now running");
		log("web-ui clients should connect to : " + baseUrlApache);
	}

	private void stopLoadBalancerWebServer() throws IOException, InterruptedException {
		log("undeploying webui");
		gswebui.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(gswebui, DeploymentStatus.UNDEPLOYED);
		log("undeploying webSpace");
		webSpace.undeploy();
		ProcessingUnitUtils.waitForDeploymentStatus(webSpace, DeploymentStatus.UNDEPLOYED);
		scriptLoadBalancer.kill();
		Thread.sleep(2000);
		File gsconf = new File(System.getenv("apache.home") + "/conf/gigaspaces/gs-webui.conf");
		gsconf.delete();
	}

	private void replaceBalancerTemplate() throws IOException {
		String oldFile = ScriptUtils.getBuildPath() + "/tools/apache/balancer-template.vm";
		String newFile = SGTestHelper.getSGTestRootDir() + "/src/main/resources/webui/balancer-template.vm";
		IOUtils.replaceFile(oldFile, newFile);
	}

	public LoginPage getLoginPage() {
		return getLoginPage(this.selenium,this.driver);
	}

	public WebDriver getDriver() {
		return driver;
	}

	public Selenium getSelenium() {
		return selenium;
	}

	private LoginPage getLoginPage(Selenium selenium, WebDriver driver) {
		return new LoginPage(selenium, driver);
	}

	public boolean verifyAlertThrown() {
		return selenium.isElementPresent(WebConstants.Xpath.okAlert);
	}

	/**
	 * use AbstractSeleniumTest static browser fields
	 * @return
	 * @throws InterruptedException
	 */
	public LoginPage openAndSwitchToNewBrowser() throws InterruptedException {
		WebDriver drv = initializeWebDriver();
		if (drv != null) {
			drv.get(baseUrl);
			Selenium selenium_temp = new WebDriverBackedSelenium(drv, baseUrl);
			seleniumBrowsers.add(selenium_temp);
			waitForServerConnection(drv);
			return getLoginPage(selenium_temp, drv);
		}
		else {
			Assert.fail("Failed to start a new browser");
		}
		return null;

	}

	public void takeScreenShot(Class<?> cls, String testMethod, String picName) {

		String browser = System.getProperty("selenium.browser", "Firefox");
		if (!isDevMode() && browser.equals("Firefox")) {

			String suiteName = "webui-" + this.browser;

			File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);

			String buildDir = SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" + PlatformVersion.getBuildNumber();

			String testLogsDir = cls.getName() + "." + testMethod + "()";

			String to = buildDir + "/" + suiteName + "/" + testLogsDir + "/" + picName + ".png";

			try {
				FileUtils.copyFile(scrFile, new File(to));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setBrowser(String browser) {
		System.setProperty("selenium.browser", browser);
	}

	public void restorePreviousBrowser() {
		LogUtils.log("restoring browser setting to " + defaultBrowser);
		setBrowser(defaultBrowser);
	}

	/**
	 * retrieves the license key from gigaspaces installation license key
	 * @throws javax.xml.stream.FactoryConfigurationError
	 * @throws javax.xml.stream.XMLStreamException
	 * @throws java.io.IOException
	 */
	public String getLicenseKey() throws XMLStreamException, FactoryConfigurationError, IOException {

		String licensekey = LICENSE_PATH.replace("lib/required/../../", "");	
		InputStream is = new FileInputStream(new File(licensekey));
		XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(is);
		int element;
		while (true) {
			element = parser.next();
			if (element == XMLStreamReader.START_ELEMENT) {
				if (parser.getName().toString().equals("licensekey")) {
					return parser.getElementText();
				}
			}
			if (element == XMLStreamReader.END_DOCUMENT) {
				break;
			}
		}
		return null;

	}

	public void repetitiveAssertTrueWithScreenshot(String message, RepetitiveConditionProvider condition, Class<?> cls, String methodName, String picName) {

		repetitiveAssertTrueWithScreenshot(message, condition, waitingTime, cls, methodName, picName);
	}

	public void repetitiveAssertTrueWithScreenshot(String message, RepetitiveConditionProvider condition, long timeoutMilliseconds, Class<?> cls, String methodName, String picName) {

		try {
			AssertUtils.repetitiveAssertTrue(null, condition, timeoutMilliseconds);
		}
		catch (AssertionError err) {
			takeScreenShot(cls, methodName, picName);
			LogUtils.log(message, err);
			AssertUtils.assertFail(message);
		}

	}
	
	public void assertTrueWithScreenshot(boolean condition,String message, Class<?> cls, String methodName, String picName) {

		try {
			AssertUtils.assertTrue(condition);
		}
		catch (AssertionError err) {
			takeScreenShot(cls, methodName, picName);
			AssertUtils.assertFail(message);
		}

	}


	public void assertTrueWithScreenshot(boolean condition, Class<?> cls, String methodName, String picName) {
		assertTrueWithScreenshot(condition, "Test Failed", cls, methodName, picName);
	}

	public void assertTrueWithScreenshot(String message, boolean condition, Class<?> cls, String methodName, String picName) {

		try {
			AssertUtils.assertTrue(condition);
		}
		catch (AssertionError err) {
			takeScreenShot(cls, methodName, picName);
			LogUtils.log("Stacktrace: ", err);
			AssertUtils.assertFail(message);
		}

	}

	private static String findJarFilenameByRegexPattern(final String folderPath, final Pattern pattern) throws FileNotFoundException {
        
        final File folder = new File(folderPath);
        if(!folder.isDirectory()) {
            throw new FileNotFoundException(folder+" is not a directory.");
        }
            
        final File[] files = folder.listFiles(new FileFilter(){
            @Override
            public boolean accept(File file) {
                return pattern.matcher(file.getName()).matches();
            }
        });
        
        if (files.length != 1) {
            throw new FileNotFoundException("Folder " + folderPath + " should contain exactly one jar that satisfies the pattern " + pattern.toString());
        }
        
        return files[0].getName();
    }

	public boolean isDevMode() {
		return SGTestHelper.isDevMode();
	}

}