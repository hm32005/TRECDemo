import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.similarities.AfterEffectL;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelP;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.NormalizationH2;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/** Simple command-line based search demo. */
public class BatchSearchBoolean {

	private BatchSearchBoolean() {
	}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		String usage = "Usage:\tjava BatchSearch [-index dir] [-simfn similarity] [-field f] [-queries file]";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)\n");
			System.exit(0);
		}

		String index = "index";
		String field = "contents";
		String queries = null;
		String simstring = "default";

		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i + 1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i + 1];
				i++;
			} else if ("-queries".equals(args[i])) {
				queries = args[i + 1];
				i++;
			} else if ("-simfn".equals(args[i])) {
				simstring = args[i + 1];
				i++;
			}
		}

		Similarity simfn = null;
		if ("default".equals(simstring)) {
			simfn = new DefaultSimilarity();
		} else if ("bm25".equals(simstring)) {
			simfn = new BM25Similarity();
		} else if ("dfr".equals(simstring)) {
			simfn = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
		} else if ("lm".equals(simstring)) {
			simfn = new LMDirichletSimilarity();
		}
		if (simfn == null) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)");
			System.out.println("bm25: BM25Similarity (standard parameters)");
			System.out.println("dfr: Divergence from Randomness model (PL2 variant)");
			System.out.println("lm: Language model, Dirichlet smoothing");
			System.exit(0);
		}

		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(simfn);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_41);
		QueryParser parser = new QueryParser(Version.LUCENE_41, field, analyzer);
		BufferedReader in = null;
		if (queries != null) {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else {
			in = new BufferedReader(new InputStreamReader(new FileInputStream("queries"), "UTF-8"));
		}
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("result/result_eval_" + simstring + ".out")));
		while (true) {
			String line = in.readLine();

			if (line == null || line.length() == -1) {
				break;
			}

			line = line.trim();
			if (line.length() == 0) {
				break;
			}

			String[] pair = line.split(" ", 2);

			writer.write(doBatchSearch(in, searcher, pair[0], pair[1], simstring, parser));
		}
		reader.close();
		writer.close();
	}

	/**
	 * This function performs a top-1000 search for the query as a basic TREC
	 * run.
	 * 
	 * @param parser
	 * @throws ParseException 
	 */
	public static String doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, String query, String runtag, QueryParser parser) throws IOException, ParseException {

		StringTokenizer st = new StringTokenizer(query);
		int count = st.countTokens();
		final HashMap<String, HashMap<Integer, Float>> docMap = new HashMap<String, HashMap<Integer, Float>>();

		while (st.hasMoreTokens()) {
			String queryTerm = st.nextToken();
			final  HashMap<Integer, Float> docIds = new  HashMap<Integer, Float>();
			searcher.search(parser.parse(queryTerm), new Collector() {
				private int docBase;
				private Scorer scorer;
				public void setScorer(Scorer scorer) {
					this.scorer = scorer;
				}

				public boolean acceptsDocsOutOfOrder() {
					return true;
				}

				public void collect(int doc) throws IOException {
					docIds.put(doc + docBase, scorer.score());
				}

				public void setNextReader(AtomicReaderContext context) {
					this.docBase = context.docBase;
				}
			});
			docMap.put(queryTerm, docIds);
		}
		st = new StringTokenizer(query);
		HashMap<Integer, Float> MergedMap = null;
		if (st.countTokens() == 1)
			MergedMap = docMap.get(st.nextToken());
		else {
			HashMap<Integer, Float> TempMergedList = null;
			if (st.hasMoreTokens())
				TempMergedList = docMap.get(st.nextToken());
			while (st.hasMoreTokens()) {
				MergedMap = new HashMap<Integer, Float>();
				HashMap<Integer, Float> postList = docMap.get(st.nextToken());
				Iterator<Entry<Integer, Float>> tempIter =  (new TreeMap<Integer, Float>(TempMergedList).entrySet()).iterator();
				Iterator<Entry<Integer, Float>> postIter =  (new TreeMap<Integer, Float>(postList).entrySet()).iterator();
				int doc1 = 0;
				int doc2 = 0;
				float score = 0;
				if(tempIter.hasNext() && postIter.hasNext()){
					Entry<Integer, Float> mapEntry1 = tempIter.next();
					Entry<Integer, Float> mapEntry2 = postIter.next();
					doc1 = mapEntry1.getKey();
					doc2 = mapEntry2.getKey();
					score = mapEntry1.getValue() + mapEntry2.getValue();
				}
				while (tempIter.hasNext() && postIter.hasNext()) {
					if (doc1 == doc2) {
						MergedMap.put(doc1, score);
						Entry<Integer, Float> mapEntry1 = tempIter.next();
						Entry<Integer, Float> mapEntry2 = postIter.next();
						doc1 = mapEntry1.getKey();
						doc2 = mapEntry2.getKey();
						score = mapEntry1.getValue() + mapEntry2.getValue();
					}
					else if(doc1 < doc2){
						Entry<Integer, Float> mapEntry = tempIter.next();
						doc1 = mapEntry.getKey();
						score =  mapEntry.getValue();
					}
					else
						doc2 = postIter.next().getKey();
				}
				TempMergedList = MergedMap;
			}
		}
		// Collect enough docs to show 5 pages
		HashMap<String, String> seen = new HashMap<String, String>(1000);
		StringBuffer sb = new StringBuffer();
		Iterator<Entry<Integer, Float>> mergedIter = (new TreeMap<Integer, Float>(MergedMap).entrySet()).iterator();
		int start = 0;
		int end = Math.min(MergedMap.size(), 1000);

		for (int i = start; i <= end && mergedIter.hasNext(); i++) {
			Entry<Integer, Float> mapEntry = mergedIter.next();
			Document doc = searcher.doc(mapEntry.getKey());
			String docno = doc.get("docno");
			// There are duplicate document numbers in the FR collection, so
			// only output a given
			// docno once.
			if (seen.containsKey(docno)) {
				continue;
			}
			seen.put(docno, docno);
			System.out.println(qid + " Q0 " + docno + " " + i + " " + mapEntry.getValue() + " " + runtag);
			sb.append(qid + " Q0 " + docno + " " + i + " " + mapEntry.getValue() + " " + runtag + "\n");
			if(!mergedIter.hasNext())
				break;
		}
		return sb.toString();
	}
}
