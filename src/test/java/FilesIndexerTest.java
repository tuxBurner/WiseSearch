import org.apache.lucene.document.Document;

import java.util.List;

public class FilesIndexerTest {
    public static void main(String[] args) throws Exception {
        FilesIndexer filesIndexer = new FilesIndexer("src\\test\\resources", "target\\indexDirectory");
        filesIndexer.createIndex();
        List<Document> documents = filesIndexer.searchIndex("text*", Constants.FIELD_NAME);
        filesIndexer.showMatches(documents);
    }
}
