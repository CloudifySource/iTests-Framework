package iTests.framework.testng.report;

import iTests.framework.utils.LogUtils;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.List;


/***
 * 
 * Used to control order of tests
 * 
 * @author Dan Kilman
 *
 */

public class SGTestNGMethodInterceptor implements IMethodInterceptor {

    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        
        LogUtils.log("From SGTestNGMethodInterceptor");
        LogUtils.log("Number of tests to be run: " + methods.size());
        
        List<IMethodInstance> result = new ArrayList<IMethodInstance>();
        
        // this contains the method that should run before any other test
        List<IMethodInstance> startTests = new ArrayList<IMethodInstance>();
        
        
        // these are the cli and script tests and they should run last
        List<IMethodInstance> cliTests = new ArrayList<IMethodInstance>();
        List<IMethodInstance> scriptTests = new ArrayList<IMethodInstance>();
        
        List<IMethodInstance> otherTests = new ArrayList<IMethodInstance>();
        
        for (IMethodInstance m : methods) {
            String name = m.getMethod().getMethod().getDeclaringClass().getCanonicalName();
            
            if (name.contains("test.cli")) {
                cliTests.add(m);
            } else if (name.contains("test.scripts")) {
                scriptTests.add(m);
            } else if (name.contains("StartTest")) {
              startTests.add(m);  
            } else {
                otherTests.add(m);
            }
            
        }
        
        result.addAll(startTests);
        result.addAll(otherTests);
        result.addAll(cliTests);
        result.addAll(scriptTests);

        return result;
    }
    
}
