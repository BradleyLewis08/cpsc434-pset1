import java.nio.channels.*;
import java.io.IOException;
import java.net.Socket;
import java.util.*; // for Set and Iterator

public class Dispatcher implements Runnable {

	private static final int TIMEOUT = 3000; // wait timeout (milliseconds)

	private Selector selector;

	public Dispatcher() {
		// create selector
		try {
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
		while (true) {
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
						// Print the request
						System.out.println(request);
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
