import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FileSearch {
    private String _search_term;
    private String _filename;
    public FileSearch(String filename, String search_term) {
        _filename = filename;
        _search_term = search_term.toLowerCase();
    }

    public boolean search_line(String line) {
        if(line.contains(_search_term)) {
            return true;
        }
        else {
            return false;
        }
    }

    public ArrayList search() {
        ArrayList<String> results = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(_filename))) {
            String line;
            String matched_file;
            while((line = reader.readLine()) != null) {
                if(search_line(line)) {
                    matched_file = line.split(" ")[0];
                    results.add(matched_file);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return results;
    }

    public static void main(String args[]) {
        FileSearch f = new FileSearch(args[0],args[1]);
        System.out.println(f.search());
    }
}
