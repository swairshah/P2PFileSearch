import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Node extends Thread {
    public final NodeInfo _info;
    public Connector _connector;
    public String _datafile = "metafile";
    private SearchKeeper _search_keeper;
    private FileSearch _file_search;
    public int _node_id;
    private boolean _am_i_leaving = false;
    private List<NodeInfo> _leave_acks;
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

        _leave_acks = new ArrayList<>();
    }

    public void set_id(int id) {
        _node_id = id;
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
                _search_keeper.add(content.get("search_id"),msg.getSender());

                String result = local_search(content.get("search_term"));

                if (result.equals("")) {
                    // Don't send a reply
                }
                else {
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

            /*
            If the search is initiated by us,
            we'll have an associated search agent in _search_agents,
            since we got a result, stop the agent.
             */
            String search_id = content.get("search_id");
            //System.out.println("id:peers : " + _search_keeper._search_peers);
            if (_search_agents.containsKey(search_id)) {
                System.out.println("result received: " + content.get("search_result") + " from " + msg.getSender());
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
                //System.out.println("got a relayed search result from " + msg.getSender());
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

        else if (msg.getType().equals("bye")) {
            /*
            when we get a "bye" message from a node,
            that node is exiting, bye is the last message.
            So remove that node from Connector.
             */
            _connector.remove_neighbour(msg.getSender());
        }

        else if (msg.getType().equals("can_i_leave")) {
            if(_am_i_leaving) {
                Message reply_msg = new Message.MessageBuilder()
                    .type("no_you_cannot")
                    .from(_info)
                    .to(msg.getSender())
                    .build();
                _connector.send_message(reply_msg,msg.getSender());
            }
            else {
                Message reply_msg = new Message.MessageBuilder()
                    .type("yes_you_can")
                    .to(msg.getSender())
                    .from(_info).build();
                _connector.send_message(reply_msg,msg.getSender());
            }
        }

        else if (msg.getType().equals("yes_you_can")) {
            _leave_acks.add(msg.getSender());
        }

        else if (msg.getType().equals("no_you_cannot")) {

        }
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
        else if(cmd.equals("nodes")) {
            System.out.println(_connector._node_lookup.keySet());
        }
        else if(cmd.equals("bye")) {
            cleanup();
        }
        else if(cmd.equals("leave")) {
            _am_i_leaving = true;

            Message leave_msg = new Message.MessageBuilder()
                    .type("yes_you_can")
                    .from(_info).build();
            _connector.send_neighbours(leave_msg);
            /*
            TODO:
            send can_i_leave message, wait for yes/no.
            (can_i_leave message should contain node_id with it)
            if yes from all neighbour,
                choose a neighbour at random, send join_these_nodes message to it
            else
                wait for 1 second and repeat the process
             */
        }
        else {
        }
    }

    public boolean ready_to_leave() {
        return false;
    }

    public void cleanup() {
        _connector.cleanup();
    }

    @Override
    public void run() {
        take_commands();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                cleanup();
            }
        });
    }

    public static void main(String args[]) {
        Node node = new Node(new NodeInfo(args[0]));
        node.start();
        if (args.length == 2) {
            node.set_id(Integer.parseInt(args[1]));
        }

    }
}
