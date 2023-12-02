package network;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

public class HttpResponseSender {
	public static void sendResponse(HttpResponse response, OutputStream out) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		// Write the status line
		writer.write(response.getVersion() + " " + response.getStatusCode() + " " + response.getStatusMessage());
		writer.newLine();

		// Write the headers
		for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
			writer.write(entry.getKey() + ": " + entry.getValue());
			writer.newLine();
		}

		// Blank line
		writer.newLine();
		writer.flush();

		// Write the body
		if (response.getBody() != null) {
			out.write(response.getBody());
			out.flush();
		}
	}
}
