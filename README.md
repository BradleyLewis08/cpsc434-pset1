# HTTP Server Implemented in Java

### Contributors: Bradley Lewis and Alex Shin

### Running the server

The main server resides in `Server.java`. To run the server, navigate to the `HTTPServer` directory and run:

`bash start.sh <config_file>`.

Where config_file is the path to a valid configuraton file, as per the [Apache standard](http://httpd.apache.org/docs/2.4/vhosts/examples.html).

Running the file as such creates a management terminal that allows an operator to manage the server. The terminal currently supports the `shutdown` command, which will gracefull shut down a running instance of the HTTP server.

## Repository structure

Below we outline the files within the project repository, as well as an overview of their function.

`/home`: A toy directory that stores files that are obtainable through GET requests to our server, provided that its absolute path on the machine running the server is provided as a virtual host in the provided configuration file.

`Cache.java`: Our server side caching implementation that will return cached file data if available and up to date.

`CacheEntry.java`: A simple class to represent a single entry in the server side cache.

`ConfigParser.java`: Parses and creates a hashmap from the configuration file provided, used by the server for virtual host mapping.

`HTAccessParser`: Parses .htaccess files and determines whether a client side request has the appropriate authorization headers and credentials set to fetch files from within directories with a `.htaccess` file.

`.httpd.config`: A sample configuration file.

`HttpRequest.java`: Represents a standard HTTP request.

`HTTPRequestHandler.java`: Main driver class that runs in a Thread to process incoming client requests.

`HTTPResponse.java`: Represents a standard HTTP response.

`HTTPServer.java`: Server driver class that creates a HTTP server to listen on the port specified in the configuration file.

`ManagementThread.java`: Class that implements the Management terminal as described above.

`MimeTypeResolver.java`: Resolves file extensions (e.g .jpg, .png) to their corresponding MIMETypes for HTTP responses and requests.

`ServerConfig.java`: Class that represents configuration variables read in from the configuration file.

`ServerState.java`: Class that represents the current state of the server.

## Part 1.C Answers

a) The boss group is responsible for accepting incoming connections, and typically has fewer event loops since its primary job is to accept the connections and hand the work on to the worker threads. The second worker group is primarily responsible for handling all the traffic of the accepted connection after the boss has created the accepted connection, and when an operation is ready, the loop calls the corresponding handler, i.e. fires events to the ChannelHandlers that are added to the ChannelPipeline. Netty achieves synchronization among them by assigning each network connection to a specific event loop within the worker group, ensuring all operations for a particular connection are handled by a single thread. This design eliminates the need for complex synchronization mechanisms, as each event loop operates independently, managing its own connections and tasks in a thread-safe manner.

b) If ioRatio is set to N, the event loop aims to spend approximately N% of its time on I/O tasks, and the remaining (100 - N)% on non-I/O tasks. The event loop then continuously cycles through and during each iteration of the event loop Netty performs I/O tasks (such as reading from or writing to sockets) for a period of time, then it will switch to perform non-I/O tasks (such as executing scheduled tasks or tasks added to the event loop). After performing I/O tasks, the event loop checks the time it spent on these tasks. Then, based on the time spent on I/O tasks and the ioRatio setting, the event loop calculates how much time it should now spend on non-I/O tasks.

c) Netty’s ChannelPipeline is implemented as a double-linked list of context objects, each encapsulating a ChannelHandler, managing an ordered series of ChannelHandler instances to process inbound and outbound I/O events and offering efficient operations for insertion, removal, and traversal, crucial for dynamic pipeline modifications at runtime. The HTTP Hello World Server includes a simpler set of handlers, such as HttpRequestDecoder for converting byte streams into HTTP requests, HttpResponseEncoder for converting HTTP responses back into byte streams, and a custom handler to generate a simple "Hello World" HTTP response. On the other hand, the HTTP Snoop Server, includes a much more extensive/complex setup, incorporating handlers such as SslHandler for SSL/TLS encryption (if SSL is enabled), the HttpSnoopServerHandler for detailed logging and inspection of HTTP headers and content, and optionally, handlers such as HttpObjectAggregator and HttpContentCompressor to handle HTTP message aggregation and automatic content compression, respectively. It seems the HTTP Snoop server is more complex and utilizes a broader range of Netty’s capabilities and caters to more advanced use cases.

d) To implement the sync method of a future, you can first check if the I/O operation associated with the ChannelFuture is completed. If the future is already completed, you would return immediately, but if the future is not yet completed, then you would block the current thread by waiting on a synchronization primitive until the operation is complete. Once the operation associated with the future completes, another thread would update the state of the future and notify any other threads that are waiting on it. Then, the thread that called sync would then wake up, check the result of the operation, and either return normally if the operation was successful, or throw an exception if the operation failed.

## Reflection

Whilst we were able to hit all major specification points, we were unable to fully implement `nSelectLoops`.
