package forskbot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * 
 * @author interhack
 * 
 */
public final class Configuration {

	public static final String PROP_HOST = "forskbot.host";
	public static final String PROP_PORT = "forskbot.port";
	public static final String PROP_NICK = "forskbot.nick";
	public static final String PROP_CHANNELS = "forskbot.channels";
	public static final String PROP_NAME = "forskbot.name";

	//
	//
	//

	private static Configuration self;
	private Properties props;
	private Logger log = Logger.getLogger(Configuration.class);
	private List<String> channels;
	private String nick;
	private String host;
	private int port;
	private String name;
	private int reconnectCount = 0;

	private Configuration() {
		channels = new ArrayList<String>();
		props = new Properties();
	}

	/**
	 * Parse configuration into this class.
	 * 
	 * @param props
	 */
	void parseRawConfig(Properties props) throws IllegalArgumentException {
		
		for(Object key : props.keySet()) {
			log.info("config: " + (String) key + ":" + props.getProperty((String) key));
		}
		
		this.props = props;

		String prop = "";
		if ((prop = propNotNullOrEmpty(PROP_CHANNELS)) != null) {
			String[] parts = prop.split(",");
			for (String part : parts) {
				String channel = part.trim();
				if (!channel.startsWith("#")) {
					channel = "#" + channel;
				}
				channels.add(channel);
			}
		}

		this.nick = propOptional(PROP_NICK, "Gordon9k");

		this.host = propNotNullOrEmpty(PROP_HOST);

		this.port = Integer.valueOf(propOptional(PROP_PORT, "6667"));

		this.name = propOptional(PROP_NAME, "gordon");

	}

	private String propNotNullOrEmpty(String propName) throws IllegalArgumentException {
		String prop = props.getProperty(propName);
		if (prop == null || prop.isEmpty()) {
			throw new IllegalArgumentException("Property not set: " + propName);
		}
		return prop;
	}

	private String propOptional(String propName, String defaultValue) {
		String prop = props.getProperty(propName);
		if (prop == null || prop.isEmpty()) {
			prop = defaultValue;
		}
		return prop;
	}

	public static Configuration getSelf() {
		if (self == null) {
			self = new Configuration();
		}
		return self;
	}

	public String getNick() {
		if(reconnectCount != 0) {
			return nick + "_" + reconnectCount;
		}
		return nick;
	}

	public List<String> getChannels() {
		return channels;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getName() {
		return name;
	}

	/**
	 * Get other configuration options. May return null if not set.
	 */
	public String getAdditionalProperty(String propName) {
		return props.getProperty(propName);
	}
	
	/**
	 * Parse a configuration file
	 */
	public void parseConfiguration(File configuration) throws IllegalArgumentException {
		BufferedInputStream bis = null;
		try {
			try {
				bis = new BufferedInputStream(new FileInputStream(configuration));
				props.load(bis);
			} finally {
				bis.close();
			}
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Failed to read configuration. " + ioe.getMessage(), ioe);
		}
		parseRawConfig(props);
	}

	public void setReconnect(int reconnectCount) {
		this.reconnectCount = reconnectCount;
	}
}
