package iTests.framework.utils;

import com.j_spaces.kernel.PlatformVersion;
import com.thoughtworks.selenium.Selenium;
import iTests.framework.tools.SGTestHelper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains Utilities for testing the web-ui with the Selenium object. 
 * Hence each method added to this class should have a selenium object as one of its parameters.
 * @author elip
 *
 */
public class WebUiUtils {

    public static int ajaxWaitingTime = 5;

    public static WebElement waitForElement(WebDriver driver, By by, int timeout) {
        Wait<WebDriver> wait = new WebDriverWait(driver, timeout);
        return wait.until(visibilityOfElementLocated(by));
    }

    private static ExpectedCondition<WebElement> visibilityOfElementLocated(final By by) {
        return new ExpectedCondition<WebElement>() {
            public WebElement apply(WebDriver driver) {
                WebElement element = driver.findElement(by);
                return element.isDisplayed() ? element : null;
            }
        };
    }


    public static String retrieveAttribute(By by, String attribute,WebDriver driver) throws ElementNotVisibleException {
        double seconds = 0;
        while (seconds < ajaxWaitingTime) {
            try {
                WebElement element = driver.findElement(by);
                return element.getAttribute(attribute);
            }
            catch (StaleElementReferenceException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                seconds += 0.5;
            }
        }
        throw new ElementNotVisibleException("Could not retrieve attribute from DOM");
    }

    /**
     * Given a div element index, return the GSC number within that div.
     * Assumes that the given div index really holds a GSC, and not something else
     *
     * @param - GscDivIndex : the index of the div element witch holds the GSC in question
     * @param - selenium : the selenium object for acsses to selenium Commands, this ensures that the method is invoked with the correct page
     * @return the GSC number
     */
    public static int extractGSCNumber(int GscDivIndex , Selenium selenium) {
        int i = 0;
        String GscText = selenium.getText("//div[" + GscDivIndex + "]/table/tbody/tr[1]/td[1]/div/div/div/span[2]");
        while (GscText.charAt(i) != '[') {
            i++;
        }
        int GscNumber = Integer.parseInt((String)GscText.subSequence(4, i));
        return GscNumber;
    }

	public static void useAlertXmlConfigurationFile(String filepath) {
		
		try {
			String outFilePath = ScriptUtils.getBuildPath() + "/config/alerts/alerts.xml";
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(filepath);

			// override alert.xml in config/alerts
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result =  new StreamResult(new File(outFilePath));
			transformer.transform(source, result);

		} catch(ParserConfigurationException pce){
			pce.printStackTrace();
		} catch(TransformerException tfe){
			tfe.printStackTrace();
		} catch(IOException ioe){
			ioe.printStackTrace();
		} catch(SAXException sae){
			sae.printStackTrace();
		}
	}
	
	/**
	 * Constructs a 2 dimentional array of strings, witch will be used as the parameters of the test
	 * Each row in the array is a complete parameter set to be run with the test
	 * The @dataProvider annotation ensures the test each time with the next row of the array
	 * @param iter
	 * @param testName
	 * @return
	 * @throws java.io.IOException
	 * @throws javax.xml.stream.FactoryConfigurationError
	 * @throws javax.xml.stream.XMLStreamException
	 */
	public static String[][] getData(Iterator<List<String>> iter, String testName) throws XMLStreamException, FactoryConfigurationError, IOException {

        String xmlPath;

        if(PlatformVersion.getOfficialVersion().contains("XAP")){
            xmlPath = SGTestHelper.getSGTestSrcDir() + "/main/resources/webui/test-param.xml";
        }
        else{
            xmlPath = "test/webui/resources/test-param.xml";
        }

        Iterator<List<String>> tempIter1 = new TestParamIterator(xmlPath);
		Iterator<List<String>> tempIter2 = new TestParamIterator(xmlPath);
		int numberOfParameters = getNumberOfParameters(tempIter1, testName);
		int numberOfCombinations = getNumberOfCombinations(tempIter2, testName);
		String[][] data = new String[numberOfCombinations][numberOfParameters];
		int i = 0;
		while (iter.hasNext()) {
			List<String> combi = iter.next();
			if ((combi != null) && (combi.get(0).equals(testName))) {
				for (int j = 0 ; j < numberOfParameters ; j++) {
					data[i][j] = combi.get(j+1);
				}
				i++;
			}
		}
		return data;
	}

	/**
	 * retrieves the number of combinations that exist in the xml file for each test
	 * @param iter
	 * @param testName
	 * @return
	 */
	private static int getNumberOfCombinations(Iterator<List<String>> iter,
			String testName) {
		List<String> combination = null;
		int count = 0;
		while (iter.hasNext()) {
			combination = iter.next();
			if ((combination != null) && (combination.get(0).equals(testName))) {
				count++;
			}
		}
		return count;
	}

	/**
	 * retrieves the number of parameters to be entered in each test
	 * @param iter
	 * @param testName
	 * @return
	 */
	private static int getNumberOfParameters(Iterator<List<String>> iter,
			String testName) {
		List<String> combination = null;
		int result = 0;
		while (iter.hasNext()) {
			combination = iter.next();
			if ((combination != null) && (combination.get(0).equals(testName))) {
				result = combination.size() - 1;
				break;
			}
		}
		return result;
	}

	public static void waitForHost(String hostName, Selenium selenium) throws InterruptedException {
		for (int i = 0 ; i < 15 ; i++) {
			if (selenium.isTextPresent(hostName)) break;
			Thread.sleep(1000);
		}
	}

	/**

	 * An iterator for extracting data from the test-param.xml file witch contains parameters for various tests
	 * @author elip
	 *
	 */
	public static class TestParamIterator implements Iterator<List<String>> {

		private InputStream is;
        private XMLStreamReader parser;
        int element;

		private List<String> combination = null;

		public TestParamIterator(String xmlPath) throws XMLStreamException, FactoryConfigurationError, IOException {
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlPath);
            parser = XMLInputFactory.newInstance().createXMLStreamReader(is);
            nextTest();
		}

		public boolean hasNext() {
            return (element != XMLStreamConstants.END_DOCUMENT);
		}

		public List<String> next() {
			combination = new ArrayList<String>();
			// add test name to the combination
			if (element != XMLStreamConstants.END_DOCUMENT) combination.add(parser.getAttributeValue(null, "name"));
			// add all the parameters to combination
			while (true) {
				if (element == XMLStreamConstants.END_DOCUMENT) break;
				try {
					element = parser.next();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
				if (element == XMLStreamConstants.START_ELEMENT
						&& parser.getName().toString().equals("param")) {
					combination.add(parser.getAttributeValue(null, "value"));
				}
				if (element == XMLStreamConstants.END_ELEMENT
						&& parser.getName().toString().equals("test")) break;
			}

			// set element to next test(or at END_DOCUMENT)
			try {
				nextTest();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return combination;
		}

		public void remove() {
			throw new UnsupportedOperationException();
			}

		/**
		 * parses the parser until it reaches the next <test> element
		 * If no <test> elements are found, element will be XMLConstants.END_DOCUMENT
		 * @throws javax.xml.stream.XMLStreamException
		 * @throws java.io.IOException
		 */
		private void nextTest() throws XMLStreamException, IOException {
			while (element != XMLStreamConstants.END_DOCUMENT) {
				element = parser.next();
				if (element == XMLStreamConstants.START_ELEMENT
						&& parser.getName().toString().equals("test")) {
					return;
				}
			}
		}
	}
	
	public static boolean isInRangeOfNumber(double number, double numberToCompareTo, double percent) {
		boolean upperLimit = (number <= (numberToCompareTo + numberToCompareTo * percent / 100));
		boolean lowerLimit = (number >= (numberToCompareTo - numberToCompareTo * percent / 100));
		return (lowerLimit && upperLimit);
		
	}	
}