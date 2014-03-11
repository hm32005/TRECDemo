import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/** Simple command-line based search demo. */
public class BatchSearch {
	static String usage = "Usage:\tjava BatchSearch [-index dir] [-simfn similarity] [-field f] [-queries file] [-title-boost boost] [-desc-boost boost] [-narr-boost boost]";

	private BatchSearch() {
	}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)\n");
			System.exit(0);
		}

		String index = "index";
		String field = "contents";
		String queries = null;
		String simstring = "default";
		float title_boost = 1;
		float desc_boost = 1;
		float narr_boost = 1;

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
			} else if ("-title-boost".equals(args[i])) {
				title_boost = Float.parseFloat(args[i + 1]);
				i++;
			} else if ("-desc-boost".equals(args[i])) {
				desc_boost = Float.parseFloat(args[i + 1]);
				i++;
			} else if ("-narr-boost".equals(args[i])) {
				narr_boost = Float.parseFloat(args[i + 1]);
				i++;
			}
		}

		execBatch(index, queries, field, title_boost, desc_boost, narr_boost, simstring, loadProperties("taggers/posTagWeights.props"), true, false);
	}

	public static void execBatch(String index, String queries, String field, float title_boost, float desc_boost, float narr_boost, String simstring, Properties properties, boolean pos, boolean zoneBoost) throws IOException, ParseException {
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

		BufferedReader in = null;
		if (queries == null) {
			new TrecQueryGenerator().writeQuery();
			queries = "test-data/complete-queries.301-450";
		}
		in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		QueryParser parser = new QueryParser(Version.LUCENE_41, field, analyzer);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("result/result_QP_" + String.format("%.2f", title_boost) + "_" + String.format("%.2f", desc_boost) + "_" + String.format("%.2f", narr_boost) + ".out")));
		MaxentTagger tagger = new MaxentTagger("taggers/english-left3words-distsim.tagger", loadProperties("taggers/english-left3words-distsim.tagger.props"));
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
			String[] pair2 = pair[1].split("~");
			Query titleQuery;
			StringTokenizer st;
			if (pos) {
				String taggedTitleQuery = tagger.tagString(pair2[0].trim().toLowerCase());
				st = new StringTokenizer(taggedTitleQuery);
				StringBuffer sbTitle = new StringBuffer();
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					if (!token.contains("$") && !token.contains("\'") && !token.contains("(") && !token.contains(")") && !token.contains(",") && !token.contains(".") && !token.contains(":") && !token.contains("`") && !token.contains(";")){
						String tokenType = st.nextToken();
						sbTitle.append(token + "^" + properties.getProperty(tokenType) + " ");
						System.out.println("token \t " + token + "\t\ttokentype\t" + tokenType + "\t\tweight\t" + properties.getProperty(tokenType));
					}
					else
						st.nextToken();
				}
				titleQuery = parser.parse(sbTitle.toString().trim());
				if(zoneBoost)
					titleQuery.setBoost(title_boost);
			} else if (zoneBoost && !pos){
				titleQuery = parser.parse(pair2[0].trim());
				titleQuery.setBoost(title_boost);
			}
			else
				titleQuery = parser.parse(pair2[0].trim());
			System.out.println(titleQuery.toString());

			Query descQuery;
			if (pos) {
				String taggedDescQuery = tagger.tagString(pair2[1].trim().toLowerCase());
				st = new StringTokenizer(taggedDescQuery);
				StringBuffer sbDesc = new StringBuffer();
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					if (!token.contains("$") && !token.contains("\'") && !token.contains("(") && !token.contains(")") && !token.contains(",") && !token.contains(".") && !token.contains(":") && !token.contains("`") && !token.contains(";"))
						sbDesc.append(token + "^" + properties.getProperty(st.nextToken()) + " ");
					else
						st.nextToken();
				}
				descQuery = parser.parse(sbDesc.toString().trim());
				if(zoneBoost)
					descQuery.setBoost(desc_boost);
			} else if (zoneBoost && !pos){
				descQuery = parser.parse(pair2[1].trim());
				descQuery.setBoost(desc_boost);
			}
			else
				descQuery = parser.parse(pair2[1].trim());
			System.out.println(descQuery.toString());

			Query narrQuery;
			if (pos) {
				String taggedNarrQuery = tagger.tagString(pair2[2].trim().toLowerCase());
				st = new StringTokenizer(taggedNarrQuery);
				StringBuffer sbNarr = new StringBuffer();
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					if (!token.contains("$") && !token.contains("\'") && !token.contains("(") && !token.contains(")") && !token.contains(",") && !token.contains(".") && !token.contains(":") && !token.contains("`") && !token.contains(";"))
						sbNarr.append(token + "^" + properties.getProperty(st.nextToken()) + " ");
					else
						st.nextToken();
				}
				narrQuery = parser.parse(sbNarr.toString().trim());
				if(zoneBoost)
					narrQuery.setBoost(narr_boost);
			} else if (zoneBoost && !pos){
				narrQuery = parser.parse(pair2[2].trim());
				narrQuery.setBoost(narr_boost);
			}
			else
				narrQuery = parser.parse(pair2[2].trim());
			System.out.println(narrQuery.toString());

			BooleanQuery bq = new BooleanQuery();
			bq.add(titleQuery, Occur.SHOULD);
			bq.add(descQuery, Occur.SHOULD);
			bq.add(narrQuery, Occur.SHOULD);
			writer.write(doBatchSearch(in, searcher, pair[0], bq, simstring, title_boost, desc_boost, narr_boost));
		}
		reader.close();
		writer.close();
	}

	/**
	 * This function performs a top-1000 search for the query as a basic TREC
	 * run.
	 * 
	 * @param narr_boost
	 * @param desc_boost
	 * @param title_boost
	 */
	public static String doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, Query query, String runtag, float title_boost, float desc_boost, float narr_boost) throws IOException {

		// Collect enough docs to show 5 pages
		TopDocs results = searcher.search(query, 1000);
		ScoreDoc[] hits = results.scoreDocs;
		HashMap<String, String> seen = new HashMap<String, String>(1000);
		int numTotalHits = results.totalHits;
		StringBuffer sb = new StringBuffer();

		int start = 0;
		int end = Math.min(numTotalHits, 1000);

		for (int i = start; i < end; i++) {
			Document doc = searcher.doc(hits[i].doc);
			String docno = doc.get("docno");
			// There are duplicate document numbers in the FR collection, so
			// only output a given
			// docno once.
			if (seen.containsKey(docno)) {
				continue;
			}
			seen.put(docno, docno);
			System.out.println(qid + " Q0 " + docno + " " + i + " " + hits[i].score + " " + runtag);
			sb.append(qid + " Q0 " + docno + " " + i + " " + hits[i].score + " " + runtag + "\n");

		}
		return sb.toString();
	}

	public static Properties loadProperties(String filename) throws IOException {
		Properties props = new Properties();
		FileInputStream inStream = null;
		inStream = new FileInputStream(filename);
		props.load(inStream);
		inStream.close();
		return props;
	}
}
