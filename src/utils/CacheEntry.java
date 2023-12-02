package utils;
public class CacheEntry {
	private byte[] content;
	private long timeAdded;

	public CacheEntry(byte[] content) {
		this.content = content;
		this.timeAdded = System.currentTimeMillis();
	}

	public byte[] getContent() {
		return content;
	}

	public long getTimeAdded() {
		return timeAdded;
	}
}
