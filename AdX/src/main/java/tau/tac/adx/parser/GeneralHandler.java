package tau.tac.adx.parser;

import java.io.IOException;
import java.text.ParseException;

import se.sics.isl.util.IllegalConfigurationException;
import se.sics.tasim.logtool.LogHandler;
import se.sics.tasim.logtool.LogReader;

/**
 * <code>BankStatusHandler</code> is a simple example of a log handler that uses
 * a specific parser to extract information from log files.
 * 
 * @author - Lee Callender
 */
public class GeneralHandler extends LogHandler {

	public GeneralHandler() {
	}

	/**
	 * Invoked when a new log file should be processed.
	 * 
	 * @param reader
	 *            the log reader for the log file.
	 */
	@Override
	protected void start(LogReader reader)
			throws IllegalConfigurationException, IOException, ParseException {
		GeneralParser parser = new GeneralParser(reader, getConfig());
		parser.start();
		parser.stop();
	}
}
