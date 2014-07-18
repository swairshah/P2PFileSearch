import java.util.HashMap;
import java.util.UUID;

/**
 * Created by swair on 7/16/14.
 */
public class SearchAgent extends Thread {
    private Node _node_ref;
    public final String _search_term;
    private int _t_out = 1000; //Milliseconds
    private int _hop_count = 1;
    public boolean _running = true;
    public UUID _search_id;

    public SearchAgent(String search_term, Node node_ref) {
        _search_term = search_term;
        _node_ref = node_ref;
        _search_id = UUID.randomUUID();
    }

    public void terminate() {
        _running = false;
    }

    public synchronized void search() {
        /*
        For each neighbors of the node, construct
        a search message and ask _connector to send them.
        */
        for (String n_str : _node_ref._connector._node_lookup.keySet()) {
            NodeInfo n = new NodeInfo(n_str);
            HashMap<String,String> content = new HashMap<>();

            content.put("search_id",_search_id.toString());
            content.put("search_term",_search_term);
            content.put("hop_count",Integer.toString(_hop_count));
            content.put("initiator",_node_ref._info.toString());

            Message search_msg = new Message.MessageBuilder()
                    .type("search")
                    .content(content)
                    .to(n)
                    .from(_node_ref._info).build();

            _node_ref._connector.send_message(search_msg,n);
        }
    }

    @Override
    public void run() {
        /*
        while thread should be _running and _hop_count < 16,
        Initiate search and then wait for t_out*1,
        If result comes. Exit.
        Else continue search with hop_count*2
         */
        while(_running && _hop_count <= 16) {
            search();
            _hop_count = _hop_count*2;
            try {
                Thread.sleep(_t_out*_hop_count);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        _running = false;
    }


}
