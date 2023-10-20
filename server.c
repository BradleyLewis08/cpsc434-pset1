#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#define PORT 8080

int main(int argc, char const *argv[])
{
	struct sockaddr_in address;
	int socket_fd, new_socket, valread;

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

	// Accept incoming connections
	if ((new_socket = accept(socket_fd, (struct sockaddr *)&address, (socklen_t *)&addrlen)) < 0)
	{
		perror("failed to accept connection");
		exit(EXIT_FAILURE);
	}

	// Read incoming message and print it
	char buffer[1024] = {0};
	valread = read(new_socket, buffer, 1024);
	printf("%s\n", buffer);
}
