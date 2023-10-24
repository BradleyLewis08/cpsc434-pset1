import java.io.*;

class InvalidConfigException extends Exception {
	public InvalidConfigException(String message) {
		super(message);
	}
}

class VirtualHost {
	String serverName;
	String rootDirectory;
}

public class ConfigParser {
	private enum ParseState {
		NONE,
		VIRTUAL_HOST,
	}

	public static ServerConfig parseConfig(String configFilePath) throws IOException, InvalidConfigException {
		ServerConfig config = new ServerConfig();
		ParseState state = ParseState.NONE;
		BufferedReader reader = new BufferedReader(new FileReader(configFilePath));

		VirtualHost currVirtualHost = null;

		String line;

		while ((line = reader.readLine()) != null) {
			line = line.trim();

			if (line.startsWith("#") || line.isEmpty()) { // Skip comments
				continue;
			}

			switch (state) {
				case NONE:
					if (line.startsWith("Listen")) {
						config.setPort(Integer.parseInt(line.split(" ")[1]));
					} else if (line.startsWith("CacheSize")) {
						config.setCacheSize(Integer.parseInt(line.split(" ")[1]));
					} else if (line.startsWith("<VirtualHost")) {
						state = ParseState.VIRTUAL_HOST;
						currVirtualHost = new VirtualHost();
					} else {
						throw new InvalidConfigException("Invalid config file: " + line);
					}
					break;
				case VIRTUAL_HOST:
					if (currVirtualHost == null) {
						throw new InvalidConfigException("Invalid config file: " + line);
					}
					if (line.startsWith("DocumentRoot")) {
						currVirtualHost.rootDirectory = line.split(" ")[1];
					} else if (line.startsWith("ServerName")) {
						currVirtualHost.serverName = line.split(" ")[1];
					} else if (line.startsWith("</VirtualHost>")) {
						config.getVirtualHosts().put(currVirtualHost.serverName, currVirtualHost.rootDirectory);
						if (config.getDefaultRootDirectory() == null) {
							// Set default root directory
							config.setDefaultRootDirectory(currVirtualHost.rootDirectory);
						}
						currVirtualHost = null;
						state = ParseState.NONE;
					} else {
						throw new InvalidConfigException("Invalid config file: " + line);
					}
					break;
			}
		}
		if (config.isValidConfig()) {
			return config;
		} else {
			throw new InvalidConfigException("Invalid config file");
		}
	}
}
