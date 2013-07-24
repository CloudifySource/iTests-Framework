package iTests.framework.utils;

import java.text.DecimalFormat;

/**
 * 
 * @author evgenyf
 * @since 9.7
 */
public class FormattingUtils {

    private final static DecimalFormat _numberFormat = new DecimalFormat( "0" );
    private final static DecimalFormat _numberWithFloatFormat = new DecimalFormat( "0.0" );
    private final static DecimalFormat _numberFloatFormat2Digits = new DecimalFormat( "0.00" );
    private final static DecimalFormat _numberFloatFormat3Digits = new DecimalFormat( "0.000" );

    //constants for value calculations for big numbers
    private final static long K = 1000;
    private final static long M = K*K;
    private final static long G = M*K;

    //constants for value calculations for big memory numbers
    private final static long KB = 1024;
    private final static long MB = KB*KB;
    private final static long GB = MB*KB;
    private final static long TB = GB*KB;
    
    private final static String BYTES_NAME = "B";
    private final static String KB_NAME = "KB";
    private final static String MB_NAME = "MB";
    private final static String GB_NAME = "GB";
    private final static String TB_NAME = "TB";    
    
    //time units in msec
    private final static long SEC = 1000;
    private final static long MIN = SEC*60;
    private final static long HOUR = MIN*60;
    private final static long DAY = HOUR*24;

	
	/**
	 * Convenience method to return a String array as a CSV String. E.g. useful
	 * for toString() implementations.
	 * 
	 * @param arr
	 *            array to display. Elements may be of any type (toString will
	 *            be called on each element).
	 */
	public static String arrayToCommaDelimitedString(Object[] arr) {
		return arrayToDelimitedString(arr, ",");
	}

	/**
	 * Convenience method to return a String array as a delimited (e.g. CSV)
	 * String. E.g. useful for toString() implementations.
	 * 
	 * @param arr
	 *            array to display. Elements may be of any type (toString will
	 *            be called on each element).
	 * @param delim
	 *            delimiter to use (probably a ",")
	 */
	public static String arrayToDelimitedString(Object[] arr, String delim) {

		if (arr == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				sb.append(delim);
			}
			sb.append(arr[i]);
		}
		return sb.toString();
	}
    
    public static class FormattedValue{
    	private String unitOfMeasurement;
    	private String value;
    	
    	public FormattedValue( String unitOfMeasurement, String value ){
    		this.unitOfMeasurement = unitOfMeasurement;
    		this.value = value;
    	}
    	
    	public String getValue(){
    		return value;
    	}
    	
    	public String getUnitOfMeasurement(){
    		return unitOfMeasurement;
    	}
    	
    	public String toString(){
    		return value + unitOfMeasurement;
    	}
    }
    
    public static FormattedValue formatBytesBigValue( double val ){
    	String unitOfMeasurement;
    	double res;
    	if( val < KB ){
    		unitOfMeasurement = BYTES_NAME;
    		res = val;
    	}
    	else if( val < MB && val >= KB ){
    		unitOfMeasurement = KB_NAME;
    		res = val/KB;
    	}
    	else if( val < GB && val >= MB ){
    		unitOfMeasurement = MB_NAME;
    		res = val/MB;
    	}
    	else if( val < TB && val >= GB ){
    		unitOfMeasurement = GB_NAME;
    		res = val/GB;
    	}
    	else{
    		unitOfMeasurement = TB_NAME;
    		res = val/TB;
    	}
    	
   		return new FormattedValue( unitOfMeasurement, 
   				res < 10 && res != 0 ? formatWith1DigitAfterFloat( res ) : format( res ) );
    } 
    
    public static String formatBytesToMBValueWith1DigitAfterFloat( double val ){
    	double res = val/MB;
   		return res < 10 && res != 0 ? formatWith1DigitAfterFloat( res ) : format( res );
    }
    
    public static String formatBytesToMBValueWith2DigitsAfterFloat( double val ){
    	double res = val/MB;
   		return res < 10 && res != 0 ? formatWith2DigitsAfterFloat( res ) : format( res );
    }    
    
	public static FormattedValue formatDurationValue( long val ) {
    	String unitOfMeasurement;
    	double res;
    	if( val < SEC ){
    		unitOfMeasurement = "ms";
    		res = val;
    	}
    	else if( val < MIN && val >= SEC ){
    		unitOfMeasurement = "s";
    		res = val/SEC;
    	}
    	else if( val < HOUR && val >= MIN ){
    		unitOfMeasurement = "m";
    		res = val/MIN;
    	}
    	else{
    		unitOfMeasurement = "h";
    		res = val/HOUR;
    	}
    	
   		return new FormattedValue( unitOfMeasurement, 
   				res < 10 && res != 0 ? formatWith1DigitAfterFloat( res ) : format( res ) );
	}    
    
	public static String formatBigValue( double val  ){
		return formatBigValue( val, true );
	}
	
    public static String formatBigValue( double val, boolean withDigitsAfterFloat ){
    	double res;
    	String unitOfMeasurement;
    	if( val < K ){
    		res = val;
    		unitOfMeasurement = "";
    	}
    	else if( val < M && val >= K ){
    		res = val/K;
    		unitOfMeasurement = "K";
    	}
    	else if( val < G && val >= M ){
    		res = val/M;
    		unitOfMeasurement = "M";
    	}
    	else{
    		res = val/G;
    		unitOfMeasurement = "G";
    	}
    	
    	if( !withDigitsAfterFloat ){
    		return format( res ) + unitOfMeasurement;
    	}
    	
   		return ( res < 10 && res != 0 ? formatWith2DigitsAfterFloat( res ) : 
   								formatWith1DigitAfterFloat( res ) ) + unitOfMeasurement;
    }    
    

	
    public static String format( double num )
    {
        return _numberFormat.format( num );
    }
    
    public static String formatWith3DigitsAfterFloat( double num )
    {
    	return _numberFloatFormat3Digits.format( num );
    }    
    
    public static String formatWith2DigitsAfterFloat( double num )
    {
        return _numberFloatFormat2Digits.format( num );
    }    

    public static String formatWith1DigitAfterFloat( double num )
    {
        return _numberWithFloatFormat.format( num );
    }
}