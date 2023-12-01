import java.nio.channels.*;
import java.io.IOException;
import java.net.Socket;
import java.util.*; // for Set and Iterator

public class Dispatcher implements Runnable {

	private static final int TIMEOUT = 3000; // wait timeout (milliseconds)

	private Selector selector;

	private ServerConfig serverConfig;
	private Cache serverCache;
	private ServerState serverState;

	public Dispatcher(ServerConfig config, Cache cache, ServerState state) {
		// create selector
		try {
			serverConfig = config;
			serverCache = cache;
			serverState = state;
			selector = Selector.open();
		} catch (IOException ex) {
			System.out.println("Cannot create selector.");
			ex.printStackTrace();
			System.exit(1);
		} // end of catch
	} // end of Dispatcher

	public Selector selector() {
		return selector;
	}

	public void run() {
		while (serverState.isAcceptingRequests()) {
			try {
				selector.select(TIMEOUT); // Block while checking for events
			} catch (IOException ex) {
				ex.printStackTrace();
				break;
			}

			// readKeys is a set of ready events
			Set<SelectionKey> readyKeys = selector.selectedKeys();

			// create an iterator for the set
			Iterator<SelectionKey> iterator = readyKeys.iterator();

			// iterate over all events
			while (iterator.hasNext()) {
				SelectionKey key = (SelectionKey) iterator.next();
				iterator.remove();
				try {
					if (key.isAcceptable()) {
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						SocketChannel client = server.accept();
						client.configureBlocking(false);
						client.register(selector, SelectionKey.OP_READ);
					} // end of isAcceptable
					else if (key.isReadable()) {
						SocketChannel channel = (SocketChannel) key.channel();
						HttpRequest request = HttpRequestHandler.constructRequest(channel);
						HttpResponse response = HttpRequestHandler.constructResponse(request, serverConfig,
								serverCache);
						// Print the request
						System.out.println("Request " + request);
						System.out.println("Response " + response);

						// Attach the response to the key
						key.attach(response);

						// Change the key's interest ops to WRITE
						key.interestOps(SelectionKey.OP_WRITE);
					} else if (key.isWritable()) {
						SocketChannel channel = (SocketChannel) key.channel();
						HttpResponse response = (HttpResponse) key.attachment();
						HttpRequestHandler.sendResponse(channel, response);
						channel.close();
					}
				} catch (IOException ex) {
					key.cancel();
					try {
						key.channel().close();
						// in a more general design, call have a handleException
					} catch (IOException cex) {
					}
				} // end of catch
			} // end of while (iterator.hasNext()) {
		} // end of while (true)
	} // end of run
}
