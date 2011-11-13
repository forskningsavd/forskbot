package forskbot;

import java.io.File;

import org.apache.log4j.Logger;

import forskbot.irc.IrcBot;

/**
 * Entry point.
 * 
 * 
 */
public class Main {

	private static Logger log = Logger.getLogger(Main.class);

	/**
	 * 
	 * 
	 * @param args
	 *            <path/to/configuration.properties>
	 * @throws IrcException
	 */
	public static void main(String[] args) throws Exception {

		File config = new File(args[0]);

		Configuration.getSelf().parseConfiguration(config);

		IrcBot conn = new IrcBot();

		int reconnectCount = 0;
		while (conn.isReconnect()) {
			conn = new IrcBot();
			conn.connect();
			conn.rwLoop();

			if (conn.isReconnect()) {
				Configuration.getSelf().setReconnect(reconnectCount++);
				log.info("Reconnecting.");
			}
		}
	}

}
