import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Node extends Thread {
    public final NodeInfo _info;
    public Connector _connector;
    public String _datafile = "metafile";
    private SearchKeeper _search_keeper;
    private FileSearch _file_search;
    /*
    _search_agents stores UUID(strings) : SearchAgent object
    for that search
     */
    public ConcurrentHashMap<String, SearchAgent> _search_agents;

    public Node(NodeInfo info) {
        _info = info;
        _connector = new Connector(this);
        _connector.start();
        _search_agents = new ConcurrentHashMap<>();
        _search_keeper = new SearchKeeper(this);
        _search_keeper.start();

        _file_search = new FileSearch(_datafile);
    }

    public synchronized void take_commands() {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.print("> ");
            String command = scanner.nextLine();
            this.execute_command(command);
        }
    }

    public void process_msg(Message msg) {
        System.out.println("got msg "+ msg.getType());

        if (msg.getType().equals("search")) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> content = msg.getContent();
            System.out.println(content);
            if (_search_keeper.has(content.get("search_id"))) {
                /*
                then I've already searched for this message
                in 'recent' past, won't do a local search
                 */
            }
            else {
                System.out.println(_search_keeper._search_ids);
                String result = local_search(content.get("search_term"));

                if (result.equals("")) {
                    // Don't send a reply
                }
                else {
                 _search_keeper.add(content.get("search_id"),msg.getSender());

                /*
                now build a search_result
                message and send to msg.getSender()
                 */

                HashMap<String, String> res_content = new HashMap<>();

                res_content.put("search_id", content.get("search_id"));
                res_content.put("search_term", content.get("search_term"));
                res_content.put("initiator", content.get("initiator"));
                res_content.put("search_result", result);

                Message result_msg = new Message.MessageBuilder()
                        .type("search_result")
                        .content(res_content)
                        .to(msg.getSender())
                        .from(_info).build();

                System.out.println("sending search_results "+ result);
                _connector.send_message(result_msg, msg.getSender());
                }
            }

             /*
             forward msg to neighbours is hop_count != 0
             */
            if (Integer.parseInt(content.get("hop_count")) == 0) {
                /* Don't forward the message */
            }
            else {
                // Decrease the hop_count of the new message
                int fwd_hop_count = Integer.parseInt(content.get("hop_count")) - 1;
                HashMap<String, String> fwd_content = new HashMap<>();

                fwd_content.put("search_id", content.get("search_id"));
                fwd_content.put("search_term", content.get("search_term"));
                fwd_content.put("initiator", content.get("initiator"));
                fwd_content.put("hop_count", Integer.toString(fwd_hop_count));

                Message fwd_msg = new Message.MessageBuilder()
                        .type("search")
                        .content(fwd_content)
                        .from(_info).build();

                _connector.send_neighbours_except(fwd_msg, msg.getSender());
            }

        } // end *search* handling

        /*
         *search_result* handling
        */
        else if(msg.getType().equals("search_result")) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> content = msg.getContent();
            System.out.println("result received: " + content.get("search_result") + " from " + msg.getSender());

            /*
            If the search is initiated by us,
            we'll have an associated search agent in _search_agents,
            since we got a result, stop the agent.
             */
            String search_id = content.get("search_id");
            if (_search_agents.containsKey(search_id)) {
                SearchAgent sa = _search_agents.get(search_id);
                sa.terminate();
                //TODO: display results properly
            }
            /*
            this search could be a relay-search
            check _search_keeper for its id
            if its there, forward to the peer,
             */
            else if (_search_keeper.has(search_id)) {
                if (_search_keeper.has_peer_for(search_id)) {
                    _connector.send_message(msg, _search_keeper.get_peer(search_id));
                }
                else {
                    System.err.println("id exists but no peer for "+ search_id + " " + content.get("search_term") );
                }
            }
            /*
            else we may have timed out on it, so drop it
             */
            else {
               // do nothing
            }
        } //end *search_result* handling
    }

    public String local_search(String query) {
        @SuppressWarnings("unchecked")
        ArrayList<String> results =_file_search.search(query);
        if (results.isEmpty()) {
            return "";
        }
        else {
            String result_str = "";
            for (String s : results) {
                result_str += s + ",";
            }
            return result_str;
        }

    }

    public synchronized void execute_command(String command) {
        String cmd = command.toLowerCase();
        String help_msg = String.format("\n%s\n" +
                        "%-20s\t:\t%s\n" +
                        "%-20s\t:\t%s\n" +
                        "%-20s\t:\t%s\n" +
                        "%-20s\t:\t%s\n",
                "Following Commands are supported:",
                "help","Prints this help",
                "join <ip>:<port>","Join the cluser of node with <ip> listening on <port>",
                "leave","leave from cluster(s)",
                "search <keywords>","search for a file");

        if      (cmd.startsWith("help")) {
            System.out.println(help_msg);
        }
        else if (cmd.startsWith("join")) {
            if (!cmd.contains(" ") | !cmd.contains(":")) {
                System.out.println("Incorrect Usage of Join command!");
                System.out.println(help_msg);
                return;
            }
            String[] parts = cmd.split(" ");
            if(parts.length != 2) {
                System.out.println("Incorrect Usage of Join command!");
                System.out.println(help_msg);
            }
            else {
                System.out.println("joining "+parts[1]);
                _connector.join_neighbour(new NodeInfo(parts[1]));
            }
        }
        else if (cmd.startsWith("search")) {
            String[] parts = cmd.split(" ");
            String search_term = parts[1];
            SearchAgent search_agent = new SearchAgent(search_term,this);
            _search_agents.put(search_agent._search_id.toString(), search_agent);
            search_agent.start();
        }
        else if (cmd.startsWith("leave")) {

        }
        else if(cmd.equals("nodes")) {
            System.out.println(_connector._node_lookup.keySet());
        }
        else {
        }
    }

    @Override
    public void run() {
        take_commands();
    }

    public static void main(String args[]) {
        Node node = new Node(new NodeInfo(args[0]));
        node.start();
    }
}
