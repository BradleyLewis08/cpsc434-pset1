import java.nio.channels.*;
import java.io.IOException;
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
					if (key.isAcceptable()) { // a new connection is ready to be
						SocketChannel clientSocketChannel = serverSocketChannel.accept();


						IAcceptHandler aH = (IAcceptHandler) key.attachment();
						aH.handleAccept(key);
					} // end of isAcceptable

					if (key.isReadable() || key.isWritable()) {
						IReadWriteHandler rwH = (IReadWriteHandler) key.attachment();
							rwH.handleRead(key);
						} // end of if isReadable

						if (key.isWritable()) {
							rwH.handleWrite(key);
						} // end of if isWritable
					} // end of readwrite
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
