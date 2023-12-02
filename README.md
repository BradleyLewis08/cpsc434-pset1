# HTTP Server Implemented in Java

### Contributors: Bradley Lewis and Alex Shin

## Rubric items

1. Caching - our server uses a caching mechanism to ensure that content from files that have not been modified and/or hae been fetched recently does not need to be loaded from disk. This is implemented in `Cache.java` and `CacheEntry.java`. Of particular note is line 317 in HttpRequestHandler.java, which checks the cache for a file before attempting to load it from disk.

2. Chunked Responses - our server supports chunked responses, as can be seen in `HTTPResponse.java` on line 64. The server will automatically chunk responses if the content length is not specified in the response headers. As seen on line 428 of HttpRequestHandler.java (and the corresponding definition of sendChunkedResponse on line 391) the server will chunk responses if specified in hte headers.

3. Heartbeat monitoring - our server supports heartbeat monitoring. If a GET request is made to the '/load' endpoint, this ia handled on line 364 of HttpRequestHandler.java. An instance of the ServerState class is passed to every Dispatcher, which maintains a list of active tasks as well as defining a maximum number of concurrent tasks. The '/load' handler will then use this instance to determine the current load on the server, and return the appropriate response.

4. Async I/O using select structures - our server uses the asymmetric design, where the main server thread listens for incoming connections and distributes these connections to listening Dispatchers. Each Dispatcher runs a Select loop that will listen for incoming data on each of the client SocketChannels it has been assigned to, reading and writing data whenever it is available. This is implemented in `Dispatcher.java`. We also implement a management terminal in ManagementThread.java, which allows an operator to gracefully shut down the server. This is implemented using a shared instance of ServerState, which is passed to each Dispatcher. The management terminal will then unset a flag in the ServerState instance, which will be checked by each Dispatcher on each iteration of the select loop. If the flag is unset, the Dispatcher will gracefully shut down.

### Running the server

The main server resides in `Server.java`. To run the server, navigate to the `HTTPServer` directory and run:

`bash start.sh <config_file>`.

Where config_file is the path to a valid configuraton file, as per the [Apache standard](http://httpd.apache.org/docs/2.4/vhosts/examples.html).

Running the file as such creates a management terminal that allows an operator to manage the server. The terminal currently supports the `shutdown` command, which will gracefull shut down a running instance of the HTTP server.

## Repository structure

Below we outline the files within the project repository, as well as an overview of their function.

`/home`: A toy directory that stores files that are obtainable through GET requests to our server, provided that its absolute path on the machine running the server is provided as a virtual host in the provided configuration file.

`/src/handlers`:

`HttpRequest.java`: Represents a standard HTTP request.

`HTTPRequestHandler.java`: Stateless handler that allows for the reading and parsing of HTTP requests, as well as the sending of HTTP responses back to clients.

`/src/configs`:

`ConfigParser.java`: Parses and creates a hashmap from the configuration file provided, used by the server for virtual host mapping.

`HTAccessParser`: Parses .htaccess files and determines whether a client side request has the appropriate authorization headers and credentials set to fetch files from within directories with a `.htaccess` file.

`/src/network`:

`Dispatcher.java`: Class that implements the Dispatcher pattern, allowing for the asynchronous handling of multiple client connections.

`HttpResponse.java`: Represents a standard HTTP response.

`HttpResponseSender.java`: Class that handles the sending of HTTP responses back to clients.

`/src/server`:

`ManagementThread.java`: Class that implements the Management terminal as described above.

`HttpServer.java`: Server driver class that creates a HTTP server to listen on the port specified in the configuration file. Main thread will hand off incoming connections to a Dispatcher, which will handle the connection asynchronously.

## Part 1.C Answers

a) The boss group is responsible for accepting incoming connections, and typically has fewer event loops since its primary job is to accept the connections and hand the work on to the worker threads. The second worker group is primarily responsible for handling all the traffic of the accepted connection after the boss has created the accepted connection, and when an operation is ready, the loop calls the corresponding handler, i.e. fires events to the ChannelHandlers that are added to the ChannelPipeline. Netty achieves synchronization among them by assigning each network connection to a specific event loop within the worker group, ensuring all operations for a particular connection are handled by a single thread. This design eliminates the need for complex synchronization mechanisms, as each event loop operates independently, managing its own connections and tasks in a thread-safe manner.

b) If ioRatio is set to N, the event loop aims to spend approximately N% of its time on I/O tasks, and the remaining (100 - N)% on non-I/O tasks. The event loop then continuously cycles through and during each iteration of the event loop Netty performs I/O tasks (such as reading from or writing to sockets) for a period of time, then it will switch to perform non-I/O tasks (such as executing scheduled tasks or tasks added to the event loop). After performing I/O tasks, the event loop checks the time it spent on these tasks. Then, based on the time spent on I/O tasks and the ioRatio setting, the event loop calculates how much time it should now spend on non-I/O tasks.

c) Netty’s ChannelPipeline is implemented as a double-linked list of context objects, each encapsulating a ChannelHandler, managing an ordered series of ChannelHandler instances to process inbound and outbound I/O events and offering efficient operations for insertion, removal, and traversal, crucial for dynamic pipeline modifications at runtime. The HTTP Hello World Server includes a simpler set of handlers, such as HttpRequestDecoder for converting byte streams into HTTP requests, HttpResponseEncoder for converting HTTP responses back into byte streams, and a custom handler to generate a simple "Hello World" HTTP response. On the other hand, the HTTP Snoop Server, includes a much more extensive/complex setup, incorporating handlers such as SslHandler for SSL/TLS encryption (if SSL is enabled), the HttpSnoopServerHandler for detailed logging and inspection of HTTP headers and content, and optionally, handlers such as HttpObjectAggregator and HttpContentCompressor to handle HTTP message aggregation and automatic content compression, respectively. It seems the HTTP Snoop server is more complex and utilizes a broader range of Netty’s capabilities and caters to more advanced use cases.

d) To implement the sync method of a future, you can first check if the I/O operation associated with the ChannelFuture is completed. If the future is already completed, you would return immediately, but if the future is not yet completed, then you would block the current thread by waiting on a synchronization primitive until the operation is complete. Once the operation associated with the future completes, another thread would update the state of the future and notify any other threads that are waiting on it. Then, the thread that called sync would then wake up, check the result of the operation, and either return normally if the operation was successful, or throw an exception if the operation failed.

e) A key difference between ByteBuffer and ByteBuf is how they handle the read and write operations. In a ByteBuffer, there is a single position pointer which moves for both the read and write operations, making it inconvenient to switch between reading from and writing to the buffer, without calling flip() or rewind(). On the other hand, the byteBuf data structure in Netty has two separate points for reading and writing (readerIndex and writerIndex respectively). This design allows for more flexible and efficient handling of the data since you can read from and write to the buffer without need to switch modes or manipulate the position pointers explicitly.
