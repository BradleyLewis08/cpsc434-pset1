#!/usr/bin/env python3
import cgi

print("Content-Type: text/html")    # HTML is following
print()                             # Blank line, end of headers

# Use the CGI library to parse query parameters
form = cgi.FieldStorage()

# Get the values of the query parameters, default to zero if parameters are not provided
number1 = form.getvalue('number1', '0')
number2 = form.getvalue('number2', '0')

# Attempt to convert parameters to float and calculate the sum
try:
    sum_numbers = float(number1) + float(number2)
    result = str(sum_numbers)
except ValueError:
    result = "Invalid input. Please provide numeric values."

print("<html>")
print("<head><title>Add Numbers</title></head>")
print("<body>")
print("<h1>Sum of Numbers</h1>")
print(f"<p>The sum of {number1} and {number2} is: {result}</p>")
print("</body>")
print("</html>")
