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
