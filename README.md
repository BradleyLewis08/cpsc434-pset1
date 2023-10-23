# CPSC 434 - Assignment 1, Part 1

## Team Members

- Bradley Lewis
- Alex Shin

## Getting started

The shell script HttpServer.sh will compile and run the server. The server will run on port 8080.

To make the script executable, run the following command:

```
chmod +x HttpServer.sh
```

Then, simply run the following command (shell script) to start the server:

```
./HttpServer.sh
```

Once the server is running, you can test the server using `telnet`:

`telnet localhost 8080`

Once telnet connects, you can send any message and it should respond back with a `200 OK` message.

## Content Type

When there is a conflict between the Mime Type of the requested resource and the server-side resource, we elect to ignore the 'Accept' header and return the server-side resource in its original format for simplicity's sake. A more developed implementation may attempt content transformation here.

## Heartbeat

The HTTP Server also supports a heartbeat endpoint at `/load`. This endpoint will return a `200 OK` response if the server is able to process the request. However, if the server is overloaded, it will return a `503 Service Unavailable` response. This is achieved through the use of an Atomic counter that is incremented every time a request is received. If the counter exceeds the maximum number of requests (arbitratily set at 10), the server will return a `503 Service Unavailable` response.
