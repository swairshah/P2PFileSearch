import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Node extends Thread {
    public final NodeInfo _info;
    public Connector _connector;
    public String _datafile = "metafile";
    private SearchKeeper _search_keeper;
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
            HashMap<String,String> content = msg.getContent();
            System.out.println(content);

            if(_search_keeper.has(content.get("id"))) {
                /*
                then I've already searched for this message
                in 'recent' past, won't do a local search
                 */
            }
            else {
                local_search(content.get("search_string"));
                _search_keeper.add(content.get("id"));
            }
        }
    }

    public void local_search(String query) {

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
