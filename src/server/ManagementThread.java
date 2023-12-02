package server;
import java.util.*;

import server.ServerState;

public class ManagementThread extends Thread {

    private final ServerState serverState;

    public ManagementThread(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        boolean isServerRunning = true;
        while (isServerRunning) {
            System.out.print("> ");
            String command = scanner.nextLine();
            switch (command) {
                case "shutdown":
                    if (!serverState.isAcceptingRequests()) {
                        System.out.println("Server is already in the process of shutting down.");
                        return;
                    }
                    System.out.println("Initiating shutdown. The server will stop accepting new requests.");
                    serverState.setAcceptingRequests(false);
                    isServerRunning = false;
                    break;
                default:
                    System.out.println("Invalid command");
            }
        }
    }
}
