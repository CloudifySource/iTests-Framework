package iTests.framework.testng.report;

import com.j_spaces.kernel.JSpaceUtilities;

import java.text.DateFormat;
import java.util.Date;

@Deprecated
public class SGTestNGReporter {
    /**
     * All output logged in a sequential order.
     */
    private static StringBuilder output = new StringBuilder();

    public static String getOutput() {
        return output.toString();
    }

    public static void reset() {
        output.delete(0, output.length());
    }

    public static void log(String s, Throwable t, boolean logToStandardOut) {
        int startIdx = output.length();
        output.append(getCurrentTime()).append(" - ").append(s);
        if (t != null) {
            output.append("\n").append(JSpaceUtilities.getStackTrace(t));
        }
        if (logToStandardOut) {
            if (startIdx != 0) {
                startIdx--;
            }
            System.out.println(output.substring(startIdx, output.length()));
        }

        output.append("\n");
    }

    private static String getCurrentTime(){
        Date now = new Date();
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(now);
    }
}