#!/usr/bin/env python3
import datetime

print("Content-Type: text/html")    # HTML is following
print()                             # Blank line, end of headers

print("<html>")
print("<head><title>Current Server Time</title></head>")
print("<body>")
print("<h1>Current Server Time</h1>")
print("<p>The current server time is: " + str(datetime.datetime.now()) + "</p>")
print("</body>")
print("</html>")
