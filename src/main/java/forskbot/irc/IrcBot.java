package forskbot.irc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;

import org.apache.log4j.Logger;

import forskbot.Configuration;

/**
 * 
 */
public class IrcBot {

	private Logger log = Logger.getLogger(IrcBot.class);
	private String host;
	private int port;
	private String nick;
	private String name;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private static final int MAX_SERVER_LINE_LENGTH = 2000;
	private static final int MAX_URLMATCHES_PERLINE = 3;
	private boolean reconnect = true;
	//
	private String nl = "\r\n";

	public IrcBot() {
		Configuration config = Configuration.getSelf();

		this.host = config.getHost();
		this.port = config.getPort();
		this.nick = config.getNick();
		this.name = config.getName();
	}

	private void setRequestProperties(URLConnection conn) {
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Unknown x86_64; rv:199.0) Gecko/20990101 Firefox/199.0");
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml");
		conn.setRequestProperty("Accept-Language", "en");
		conn.setRequestProperty("Accept-Encoding", "identify");
	}

	/**
	 * Connect to an irc server
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public void connect() throws UnknownHostException, IOException {
		socket = SocketFactory.getDefault().createSocket(host, port);

		if (!socket.isConnected()) {
			throw new IOException("Failed to establish connection");
		}

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		write("NICK " + nick);
		write("USER " + name + " " + name + " " + host + " :" + name);
	}

	private void write(String line) throws IOException {
		synchronized (writer) {
			line = line.replaceAll("\\s+", " ");
			log.info("out: " + line);
			writer.write(line + nl);
			writer.flush();
		}
	}

	/**
	 * Read a raw line from irc.
	 */
	private String read() throws IOException {
		return reader.readLine();
	}

	/**
	 * Loop over reads from server. The lines received will be in the form of
	 * "<server id> <irc protocol command> <the rest ...>"
	 * 
	 * @throws IOException
	 */
	public void rwLoop() throws IOException {
		try {
			while (socket.isConnected()) {
				synchronized (this) {
					String line = read();

					if (line == null) {
						continue;
					}

					if (line != null && line.length() > MAX_SERVER_LINE_LENGTH && !line.isEmpty()) {
						// Maybe warn?
						continue;
					}

					String[] parts = line.split("\\s+");
					line = null;

					/**
					 * Respond to PING
					 */
					if (parts[0].startsWith("PING")) {
						write("PONG " + parts[1].substring(1));
						continue;
					} else if (parts.length >= 3) {
						// [:nick!~user@host/mask, PRIVMSG, #channel,
						// :messagePart, messagePart, ...]
						if (parts.length >= 4 && parts[0].substring(1).contains("!") && parts[1].equals("PRIVMSG")) {

							String chanOrNick = parts[2];
							String[] messageParts = Arrays.copyOfRange(parts, 3, parts.length);

							// Handle only public
							if (chanOrNick.startsWith("#")) {
								// Bot command
								if (messageParts.length >= 2 && messageParts[0].matches(":" + Configuration.getSelf().getNick() + ".?")) {
									if (messageParts[1].equals("!quit")) {
										write("QUIT :Byte");
										reconnect = false;
										break;
									}
								} else {
									detectParseUrls(chanOrNick, messageParts);
								}
							}

							log.info("PRIVMSG: " + Arrays.toString(parts));

							continue;
						} else if (parts[1].equals("255")) {
							write("PING " + parts[0].substring(1));
							for (String channel : Configuration.getSelf().getChannels()) {
								write("JOIN " + channel);
							}
							continue;
						}
					}

					log.warn("Unhandled: " + Arrays.toString(parts));
				}
			}
		} finally {
			reader.close();
			writer.close();
			log.info("Closing connection.");
		}
	}

	public static final Pattern URL_PATTERN = Pattern.compile("^((?i:http://.*)|(?i:www\\..*)|([a-zA-Z0-9\\-]+?(\\.[a-zA-Z0-9\\-]+?)+?/.*))$");
	public static final Pattern TITLE_PATTERN = Pattern.compile("^.*(<[\\s+]?(?i:title)[\\s+]?>(.*?)<[\\s+]?/[\\s+]?(?i:title)[\\s+]?>).*$");

	/**
	 * May be flooded if in multiple channels
	 */
	private void detectParseUrls(String chanOrNick, String[] messageParts) {
		int currMatches = 0;
		for (int i = 0; i < messageParts.length; i++) {
			String part = messageParts[i];
			if (i == 0) {
				part = part.substring(1);
			}

			if (URL_PATTERN.matcher(part).matches()) {
				if (currMatches++ >= MAX_URLMATCHES_PERLINE) {
					break;
				}

				try {
					new Thread(new TitleHandler(chanOrNick, part)).start();
				} catch (Throwable t) {
					log.error(t);
				}
			}
		}
	}

	private class TitleHandler implements Runnable {

		private String chanOrNick;
		private URI uri;

		public TitleHandler(String chanOrNick, String uriStr) throws IllegalArgumentException {

			if (!uriStr.toLowerCase().startsWith("http://")) {
				uriStr = "http://" + uriStr;
			}
			uri = URI.create(uriStr);

			this.chanOrNick = chanOrNick;

		}

		@Override
		public void run() {
			if (this.chanOrNick == null || this.chanOrNick.isEmpty() || !this.chanOrNick.startsWith("#") || this.chanOrNick.equals("null")) {
				log.error("Chan or nick must not be empty.");
				return;
			}

			try {
				URLConnection conn = uri.toURL().openConnection();
				conn.setConnectTimeout(2000);
				conn.setReadTimeout(2000);
				conn.setUseCaches(false);
				conn.setDoOutput(false);
				conn.setDoInput(true);

				setRequestProperties(conn);

				BufferedReader urlReader = null;

				try {

					urlReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					StringBuilder response = new StringBuilder();
					String line = null;
					// Could use a max_nr_lines_read limit
					while ((line = urlReader.readLine()) != null) {

						response.append(line);
						line = null;

						Matcher matcher = TITLE_PATTERN.matcher(response);
						if (matcher.find()) {
							String pageTitle = matcher.group(2).trim();
							if (!isSelfExplanatory(pageTitle)) {
								log.info("Writeback of url to " + chanOrNick + " : " + chanOrNick);
								// write("PRIVMSG " + chanOrNick + " :" +
								// pageTitle);
								synchronized (writer) {
									pageTitle = pageTitle.replaceAll("\\s+", " ");
									writer.write("PRIVMSG " + chanOrNick + " :" + pageTitle + "\n");
									writer.flush();
								}
								break;
							} else {
								break;
							}
						}
					}

				} finally {
					if (urlReader != null) {
						urlReader.close();
					}
				}

			} catch (Throwable e) {
				log.error(e);
			}
		}

		private boolean isSelfExplanatory(String pageTitle) {
			String[] uriWords = uri.toASCIIString().split("\\p{Punct}");
			String[] titleWords = filterSmallWords(pageTitle.split("[\\p{Punct}\\s+]"));

			if (uriWords.length > titleWords.length) {
				return isSimilar(uriWords, titleWords);
			} else {
				return isSimilar(titleWords, uriWords);
			}
		}

		private String[] filterSmallWords(String[] arr) {
			List<String> longWordsArr = new ArrayList<String>();

			for (String word : arr) {
				if (word.length() > 2) {
					longWordsArr.add(word);
				}
			}

			return longWordsArr.toArray(new String[] {});
		}

		/**
		 * 30% match
		 */
		public boolean isSimilar(String[] biggerArr, String[] smallerArr) {
			int nrMatched = 0;
			for (String smallArrWord : smallerArr) {
				if (containsWordIgnoreCase(biggerArr, smallArrWord)) {
					nrMatched++;
				}
			}

			float percentContained = (float) nrMatched / smallerArr.length;

			if (percentContained >= 0.3f) {
				log.debug("Similar arrays (" + percentContained + "%): " + Arrays.toString(biggerArr) + " with " + Arrays.toString(smallerArr));
				return true;
			} else {
				return false;
			}

		}

		private boolean containsWordIgnoreCase(String[] arr, String word) {
			String low = word.toLowerCase();
			for (String arrWord : arr) {
				if (arrWord.toLowerCase().equals(low)) {
					return true;
				}
			}
			return false;
		}
	}

	public boolean isReconnect() {
		return reconnect;
	}
}
