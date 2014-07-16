import java.io.*;
import java.net.Socket;

public class ListenerThread extends Thread {
    private final Socket _client;
    private InputStream _instream;
    private OutputStream _outstream;
    private Connector _connector_ref;
    public ListenerThread(Socket client, Connector connector) {
        _connector_ref = connector;
        _client = client;
        try {
            _instream = _client.getInputStream();
            _outstream = _client.getOutputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                ObjectInputStream obj_in = new ObjectInputStream(_instream);
                Message msg = (Message) obj_in.readObject();
                //System.out.println(msg);
                _connector_ref.deliver_msg(msg);
            } catch(IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }
}
