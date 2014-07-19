import java.io.*;
import java.net.Socket;

public class ListenerThread extends Thread {
    private final Socket _client;
    private InputStream _instream;
    private OutputStream _outstream;
    private Connector _connector_ref;
    private boolean _running = true;
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

    public void cleanup() {
        terminate();
        try {
            _instream.close();
            _outstream.close();
            _client.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void terminate() {
        _running = false;
    }

    @Override
    public void run() {
        while(_running) {
            try {
                ObjectInputStream obj_in = new ObjectInputStream(_instream);
                Message msg = (Message) obj_in.readObject();
                /*
                 when a bye message is received,
                 terminate the thread, as there will be no further
                 message from node at the other end of the connection
                  */
                if(msg.getType().equals("bye")) {
                    obj_in.close();
                    terminate();
                }
                _connector_ref.deliver_msg(msg);
            } catch(IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }
}
