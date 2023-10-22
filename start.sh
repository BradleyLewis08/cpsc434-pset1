find . -name "*.java" > sources.txt
javac @sources.txt
java HttpServer -config httpd.config