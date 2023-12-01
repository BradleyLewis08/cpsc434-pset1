import java.util.*;

public class ServerConfig {
	private int port;
	private Map<String, String> virtualHosts = new HashMap<>(); // Map of serverName to rootDirectory
	private String defaultRootDirectory;
	private int CacheSize;
	private int nSelectLoops;

	public int getPort() {
		return port;
	}

	public Map<String, String> getVirtualHosts() {
		return virtualHosts;
	}

	public String getDefaultRootDirectory() {
		return defaultRootDirectory;
	}

	public int getCacheSize() {
		return CacheSize;
	}

	public int getnSelectLoops() {
		return nSelectLoops;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setVirtualHosts(Map<String, String> virtualHosts) {
		this.virtualHosts = virtualHosts;
	}

	public void setDefaultRootDirectory(String defaultRootDirectory) {
		this.defaultRootDirectory = defaultRootDirectory;
	}

	public void setCacheSize(int CacheSize) {
		this.CacheSize = CacheSize;
	}

	public void setnSelectLoops(int nSelectLoops) {
		this.nSelectLoops = nSelectLoops;
	}

	public boolean isVirtualHostsMapValid() {
		if (virtualHosts == null || virtualHosts.isEmpty()) {
			return false;
		}
		for (String serverName : virtualHosts.keySet()) {
			// Check each serverName has a mapped rootDirectory
			if (virtualHosts.get(serverName) == null) {
				return false;
			}
		}
		return true;
	}

	public boolean isValidConfig() {
		return port != 0 && defaultRootDirectory != null && isVirtualHostsMapValid();
	}
}
