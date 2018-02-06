public interface Indexable<T> {

    void createIndex() throws Exception;

    T searchIndex(String key, String searchOnField) throws Exception;

    void showMatches(T matches);
}
