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

## Reflection 

Whilst we were able to hit all major specification points, we were unable to fully implement `nSelectLoops`. 



