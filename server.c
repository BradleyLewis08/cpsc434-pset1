#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#define PORT 8080

void parse_request(char *buffer)
{
	//Use strtok_r to tokenize the request --> strtok_r is thread safe (strtok is not)

	char *saveptr;
	printf("Request: %s\n", buffer);
	//Toeknize the request line
	char *token = strtok_r(buffer, " ", &saveptr);
	char *method = token;
	token = strtok_r(NULL, " ", &saveptr);
	char *path = token;
	token = strtok_r(NULL, " ", &saveptr);
	char *version = token;

	//Print the request line
	printf("Method: %s\n", method);
	printf("Path: %s\n", path);
	printf("Version: %s\n", version);

	//Tokenize the headers
	token = strtok_r(NULL, "\r\n", &saveptr);
	while (token != NULL)
	{
		printf("Header: %s\n", token);
		token = strtok_r(NULL, "\r\n", &saveptr);
	}

	//Tokenize the body
	token = strtok_r(NULL, "\r\n", &saveptr);
	while (token != NULL)
	{
		printf("Body: %s\n", token);
		token = strtok_r(NULL, "\r\n", &saveptr);
	}

	//Print the body
	printf("Body: %s\n", token);
}

void handle_request(char *buffer, int client_fd)
{
	// Parse the request
	parse_request(buffer);

	char *response = "HTTP/1.1 200 OK\nContent-Type: text/plain\nContent-Length: 12\n\nHello world!";

	write(client_fd, response, strlen(response));
	close(client_fd);
}

int main(int argc, char const *argv[])
{
	struct sockaddr_in address;
	int socket_fd, valread;

	int opt = 1;
	int addrlen = sizeof(address);

	// Create new socket file descriptor
	socket_fd = socket(AF_INET, SOCK_STREAM, 0);

	if (socket_fd == 0)
	{
		perror("socket failed");
		exit(EXIT_FAILURE);
	}

	// Set socket options. REUSEADDR and REUSERPORT for easier development
	if (setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)))
	{
		perror("setsockopt");
		exit(EXIT_FAILURE);
	}

	// Set address family, port, and IP address
	address.sin_family = AF_INET;
	address.sin_addr.s_addr = INADDR_ANY;
	address.sin_port = htons(PORT);

	// Bind socket to address
	if (bind(socket_fd, (struct sockaddr *)&address, sizeof(address)) < 0)
	{
		perror("bind failed");
		exit(EXIT_FAILURE);
	}

	if (listen(socket_fd, 3) < 0)
	{
		perror("failed to start listening");
		exit(EXIT_FAILURE);
	}

	while (1) // TODO: Multithreading to handle multiple clients
	{
		// Accept incoming connections
		int client_fd;
		if ((client_fd = accept(socket_fd, (struct sockaddr *)&address, (socklen_t *)&addrlen)) < 0)
		{
			perror("failed to accept connection");
			exit(EXIT_FAILURE);
		}

		// Read incoming message and print it
		char buffer[1024] = {0};
		valread = read(client_fd, buffer, 1024);

		handle_request(buffer, client_fd);
	}
}
