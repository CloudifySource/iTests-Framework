package iTests.framework.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

public class SGConsoleHandler extends ConsoleHandler {

	@Override
	public void publish(LogRecord record) {
		String msg = getFormatter().format(record);
		LogUtils.log(msg);
	}
	
	

}
