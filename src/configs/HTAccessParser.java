package configs;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class HTAccessParser {

    private String authType;
    private String authName;
    private String user;
    private String password;

    public HTAccessParser(File htAccessFile) throws IOException {
        if (htAccessFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(htAccessFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("AuthType")) {
                        this.authType = line.split(" ")[1].trim();
                    } else if (line.startsWith("AuthName")) {
                        this.authName = line.split("\"")[1].trim();
                    } else if (line.startsWith("User")) {
                        this.user = new String(Base64.getDecoder().decode(line.split(" ")[1].trim()),
                                StandardCharsets.UTF_8);
                    } else if (line.startsWith("Password")) {
                        this.password = new String(Base64.getDecoder().decode(line.split(" ")[1].trim()),
                                StandardCharsets.UTF_8);
                    }
                }
            }
        }
    }

    public boolean isAuthenticationRequired() {
        return authType != null && authName != null && user != null && password != null;
    }

    public boolean authenticate(String credentials) {
        credentials = new String(Base64.getDecoder().decode(credentials), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);
        if (parts.length == 2) {
            String username = parts[0];
            String password = parts[1];
            return this.user.equals(username) && this.password.equals(password);
        }
        return false;
    }

    public String getAuthType() {
        return authType;
    }

}
