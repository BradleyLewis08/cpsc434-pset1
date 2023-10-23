import java.util.*;

public class ManagementThread extends Thread {

    private final ServerState serverState;

    public ManagementThread(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true){
            System.out.println("Management control console > ");
            String command = scanner.nextLine();
            switch(command){
                case "shutdown":
                    if (!serverState.isAcceptingRequests()) {
                        System.out.println("Server is already in the process of shutting down.");
                        return;
                    }
                    System.out.println("Initiating shutdown. The server will stop accepting new requests.");
                    serverState.setAcceptingRequests(false);
                    break;
                default:
                    System.out.println("Invalid command");
            }
        }
    }
    
}
