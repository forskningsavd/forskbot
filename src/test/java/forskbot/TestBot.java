package forskbot;

import java.util.Properties;

import junit.framework.Assert;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import forskbot.irc.IrcBot;

public class TestBot {

	@BeforeClass
	public static void init() throws Exception {
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));

		Logger.getRootLogger().getLoggerRepository().getLogger("forskbot").setLevel(Level.ALL);
		Logger.getRootLogger().getLoggerRepository().getLogger("forskbot").setAdditivity(false);
		Logger.getRootLogger().getLoggerRepository().getLogger("forskbot").addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
	}

	@Test
	public void testUrlMatch() {
		String[] toMatch = { "www.google.coM", "www.google.com/one?two=three", "http://www.google.com", "http://www.google.com/hellothere", "google.com/hello" };

		for (String match : toMatch) {
			Assert.assertTrue(IrcBot.URL_PATTERN.matcher(match).matches());
		}

		String[] dontMatch = { "google.com", "google/com" };

		for (String match : dontMatch) {
			Assert.assertFalse(IrcBot.URL_PATTERN.matcher(match).matches());
		}
	}

	@Ignore
	@Test
	public void testConnectAndDoStuff() throws Exception {

		Properties props = new Properties();
		props.setProperty(Configuration.PROP_CHANNELS, "#nine13132"); // nine1238
		props.setProperty(Configuration.PROP_HOST, "irc.freenode.org");
		props.setProperty(Configuration.PROP_PORT, "6667");
		props.setProperty(Configuration.PROP_NICK, "Gordon16k");

		Configuration config = Configuration.getSelf();
		config.parseRawConfig(props);

		IrcBot conn = new IrcBot();
		conn.connect();
		conn.rwLoop();
	}

}
