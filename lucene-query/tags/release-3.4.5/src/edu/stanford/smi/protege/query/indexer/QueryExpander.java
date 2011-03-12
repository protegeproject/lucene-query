package edu.stanford.smi.protege.query.indexer;

import java.io.IOException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import edu.stanford.smi.protege.util.Log;

public class QueryExpander {

    private static final char WILDCARD_CHAR = '*';
    private static final String WILDCARD_LEADING_TRAILING_PATTERN = "^\\" + WILDCARD_CHAR + "|\\" + WILDCARD_CHAR + "$";
    private static final int EXACT_MATCH_BOOST = 10;

    private final ExecutorService indexRunner;
    private final Analyzer analyzer;
    private final String field;
    private final String fullIndexPath;


    public QueryExpander(ExecutorService indexRunner, Analyzer analyzer, String field, String fullIndexPath) {
        this.indexRunner = indexRunner;
        this.analyzer = analyzer;
        this.field = field;
        this.fullIndexPath = fullIndexPath;
    }

    /**
     * Constructs a Lucene query that finds all possible matches for words or
     * phrases in their order with wildcard character at the end of each(i.e.
     * "bloo*" or "cutaneo* mela*" or "epidermol* bullos* acquisi*")
     *
     * @param field
     *            - field to search on
     * @param expr
     * @throws Exception
     */
    public void parsePrefixQuery(BooleanQuery boolQuery,  String expr) throws Exception {
        expr = prepareExpression(expr);

        if (expr.length() > 0) {
            TermQuery tq = new TermQuery(new Term(field, expr));
            tq.setBoost(EXACT_MATCH_BOOST);
            boolQuery.add(tq, BooleanClause.Occur.SHOULD);

            MultiPhraseQuery mpq = new MultiPhraseQuery();
            StringTokenizer st = new StringTokenizer(expr);

            while (st.hasMoreTokens()) {
                Term[] terms = expand(st.nextToken());
                mpq.add(terms);
            }

            boolQuery.add(mpq, BooleanClause.Occur.SHOULD);
        }
    }

    private String prepareExpression(String expr) throws ParseException {
        expr = expr.replaceAll(WILDCARD_LEADING_TRAILING_PATTERN, "");

        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(expr);

        expr = query.toString().replace(field + ":", "");
        expr = expr.replace("\"", "");
        expr = expr.toLowerCase();
        expr = expr.replaceAll(":", " ");

        return expr;
    }

    private Term[] expand(String prefix) throws ExecutionException, InterruptedException {
        QueryExpanderRunner expander = new QueryExpanderRunner(prefix);
        Future<Term[]> future = indexRunner.submit(expander);
        return future.get();
    }

    private class QueryExpanderRunner implements Callable<Term[]> {
        private String prefix;

        public QueryExpanderRunner(String prefix) {
            this.prefix = prefix;
        }

        public Term[] call() throws Exception {
            QueryParser parser = new QueryParser(field, analyzer);
            Query queryExact = parser.parse(prefix + WILDCARD_CHAR);
            IndexReader reader = null;
            Query queryRewritten = null;
            try {
                reader = IndexReader.open(fullIndexPath);
                queryRewritten = queryExact.rewrite(reader);
            } finally {
                forceClose(reader);
            }

            Set<Term> terms = new TreeSet<Term>();
            terms.add(new Term(field, prefix));

            queryRewritten.extractTerms(terms);

            return terms.toArray(new Term[terms.size()]);
        }
    }

    private void forceClose(IndexReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ioe) {
            Log.getLogger().log(Level.WARNING, "Exception caught reading/deleting documents from index", ioe);
        }
    }

}