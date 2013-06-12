package iTests.framework.utils;

import org.testng.ITestResult;

public class TestNGUtils {
	
	public static String constructTestMethodName(ITestResult iTestResult) {
		String parameters = extractParameters(iTestResult);
        if(ScriptUtils.isWindows()){
            LogUtils.log("class-simple: " + iTestResult.getTestClass().getClass().getSimpleName());
            LogUtils.log("class: " + iTestResult.getTestClass().getClass().getName());
            LogUtils.log("class-real: " + iTestResult.getTestClass().getRealClass().getSimpleName());

            return iTestResult.getTestClass().getRealClass().getSimpleName() + "." + iTestResult.getMethod().getMethodName() + "(" + parameters + ")";
        }
        else{
            return iTestResult.getTestClass().getName() + "." + iTestResult.getMethod().getMethodName() + "(" + parameters + ")";
        }
	}
	
	/**
	 * @param iTestResult
	 * @return a string  of the test's invoked parameters separated by a comma (',')
	 */
	public static String extractParameters(ITestResult iTestResult) {
		String parameters = "";
        Object[] params = iTestResult.getParameters();
        if (params.length != 0) {
        	parameters = params[0].toString();
            for (int i = 1 ; i < params.length ; i++) {
            	parameters += parameters + "_";
            }
        }
        return parameters;	
	}

}
