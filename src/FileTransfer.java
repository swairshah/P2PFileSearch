import java.io.*;
import java.net.Socket;

/**
 * Created by swair on 7/25/14.
 */
public class FileTransfer extends Thread {
    private Socket _client;
    public FileTransfer(Socket client, FileServer fs) {
        _client = client;
    }

    public static void copyStream(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, bytesRead);
        }
    }

    @Override
    public void run() {
        try {
            InputStreamReader sin = new InputStreamReader(_client.getInputStream());
            BufferedReader read = new BufferedReader(sin);
            String filename = read.readLine().trim();

            FileInputStream fin = new FileInputStream(filename);
            BufferedInputStream bin = new BufferedInputStream(fin);

            OutputStream os = _client.getOutputStream();
            copyStream(bin,os);

            os.flush();
            _client.close();
            System.out.println("File transfer complete");
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}
