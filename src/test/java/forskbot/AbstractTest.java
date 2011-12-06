
package forskbot;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.BeforeClass;

/**
 * 
 * @author interhack
 * 
 */
public class AbstractTest {

	@BeforeClass
	public static void init() throws Exception {

		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));

		Logger.getRootLogger().getLoggerRepository().getLogger("forskbot").setLevel(Level.ALL);
		Logger.getRootLogger().getLoggerRepository().getLogger("forskbot").setAdditivity(false);
		Logger.getRootLogger().getLoggerRepository().getLogger("forskbot")
				.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
	}

}
