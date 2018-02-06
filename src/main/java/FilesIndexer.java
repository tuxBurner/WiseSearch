import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FilesIndexer implements Indexable<List<Document>> {

    private final String FILES_TO_INDEX_DIRECTORY;
    private final String INDEX_SAVE_DIRECTORY;

    private final StandardAnalyzer filesAnalyzer = new StandardAnalyzer();
    private Directory filesIndex;

    FilesIndexer(String filesToIndexDir, String indexSaveDir) {
        this.FILES_TO_INDEX_DIRECTORY = filesToIndexDir;
        this.INDEX_SAVE_DIRECTORY = indexSaveDir;
    }

    public void createIndex() throws Exception {
        final File filesDir = new File(FILES_TO_INDEX_DIRECTORY);
        if (!filesDir.exists() || !filesDir.canRead())
            System.out.println(filesDir.getAbsolutePath() + " does not exist or not readable");

        Date start = new Date();
        filesIndex = FSDirectory.open(Paths.get(INDEX_SAVE_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(filesAnalyzer);
        if (Constants.CONFIG_CREATE_NEW_INDEX)
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        else
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        IndexWriter indexWriter = new IndexWriter(filesIndex, config);
        indexFiles(indexWriter, filesDir);
        indexWriter.close();
        System.out.println(String.format("Index creation took %s ms", new Date().getTime() - start.getTime()));
    }

    private void indexFiles(IndexWriter indexWriter, File file) throws Exception {
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                if (file != null) {
                    for (int i = 0; i < files.length; i++) {
                        indexFiles(indexWriter, new File(file, files[i]));
                    }
                }
            } else {
                Document doc = new Document();
                doc.add(new StringField(Constants.FIELD_NAME, file.getName(), Field.Store.YES));
                doc.add(new StringField(Constants.FIELD_PATH, file.getPath(), Field.Store.YES));
                doc.add(new StringField(Constants.FIELD_SIZE, String.valueOf(file.length()), Field.Store.YES));
                doc.add(new StringField(Constants.FIELD_MODIFIED_TIME, String.valueOf(file.lastModified()), Field.Store.YES));
//                System.out.println("adding doc " + file.getName());
                if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE)
                    indexWriter.addDocument(doc);
                else
                    indexWriter.updateDocument(new Term(Constants.FIELD_NAME, file.getName()), doc);
            }
        }
    }

    public List<Document> searchIndex(String key, String searchOnField) throws Exception {
        Query query;
        query = new QueryParser(searchOnField, filesAnalyzer).parse(key);
        ArrayList<Document> matches = new ArrayList<>();
        IndexReader indexReader = DirectoryReader.open(filesIndex);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        TopDocs topDocs = indexSearcher.search(query, Constants.CONFIG_MAX_HITS);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        System.out.println("hits: " + scoreDocs.length);
        for (int i = 0; i < scoreDocs.length; i++) {
            matches.add(indexSearcher.doc(scoreDocs[i].doc));
        }
        indexReader.close();
        return matches;
    }

    public void showMatches(List<Document> matches) {
        System.out.println(Constants.FIELD_NAME + "\t" + Constants.FIELD_PATH + "\t" + Constants.FIELD_SIZE + "\t" + Constants.FIELD_MODIFIED_TIME);
        for (Document doc : matches) {
            System.out.println(doc.get(Constants.FIELD_NAME) + "\t" + doc.get(Constants.FIELD_PATH) + "\t" + doc.get(Constants.FIELD_SIZE) + "\t" + doc.get(Constants.FIELD_MODIFIED_TIME));
        }
    }


}
