import java.nio.channels.*;
import java.io.IOException;

public class AcceptHandler {
	public void handleAccept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel clientSocketChannel = serverSocketChannel.accept();
		clientSocketChannel.configureBlocking(false);
		SelectionKey clientKey = clientSocketChannel.register(key.selector(), SelectionKey.OP_READ);
		clientKey.attach(new ReadWriteHandler());
	}
}
