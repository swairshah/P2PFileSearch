import java.util.concurrent.ConcurrentHashMap;

public class SearchKeeper extends Thread {
    private Node _node_ref;
    public final int _default_ttl = 8000; // Milliseconds
    /*
    _search_ids contains k:v pairs like:
    id : TTL
    when TTL hits 0 remove it.
     */
    public ConcurrentHashMap<String,Integer> _search_ids;
    public SearchKeeper(Node node) {
        _node_ref = node;
        _search_ids = new ConcurrentHashMap<>();
    }

    public synchronized void add(String id) {
        _search_ids.put(id,_default_ttl);
    }

    public synchronized void remove(String id) {
        _search_ids.remove(id);
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            for(String id : _search_ids.keySet()) {
                if (_search_ids.get(id) == 0) {
                    _search_ids.remove(id);
                }
                else {
                    int new_ttl = _search_ids.get(id) - 1;
                    _search_ids.put(id,new_ttl);
                }
            }
        }
    }
}
