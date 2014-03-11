import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

import org.apache.lucene.queryparser.classic.ParseException;

public class TempTest {

	public static void main(String[] args) throws IOException, ParseException {
		// TODO Auto-generated method stub
		/*
		 * String line =
		 * "What are the advantages and/or disadvantages of tooth implants?";
		 * Pattern p = Pattern.compile("[/]"); Matcher m = p.matcher(line); line
		 * = m.replaceAll(" "); System.out.println(line);
		 */
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		float zoneBoosts[][] = {
								{
									(float) 0.2,
									(float) 0.2,
									(float) 0.2 },
								{
									(float) 0.4,
									(float) 0.2,
									(float) 0.2 },
								{
									(float) 0.6,
									(float) 0.2,
									(float) 0.2 },
								{
									(float) 0.8,
									(float) 0.2,
									(float) 0.2 },
								{
									(float) 1.0,
									(float) 0.2,
									(float) 0.2 },
								{
									(float) 0.2,
									(float) 0.4,
									(float) 0.2 },
								{
									(float) 0.2,
									(float) 0.6,
									(float) 0.2 },
								{
									(float) 0.2,
									(float) 0.8,
									(float) 0.2 },
								{
									(float) 0.2,
									(float) 1.0,
									(float) 0.2 },
								{
									(float) 0.2,
									(float) 0.2,
									(float) 0.4 },
								{
									(float) 0.2,
									(float) 0.2,
									(float) 0.6 },
								{
									(float) 0.2,
									(float) 0.2,
									(float) 0.8 },
								{
									(float) 0.2,
									(float) 0.2,
									(float) 1.0 }

		};
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("eval2.sh")));
		for (int i = 0; i < zoneBoosts.length; i++) {
			BatchSearch.execBatch("index", null, "contents", zoneBoosts[i][0], zoneBoosts[i][1], zoneBoosts[i][2], "default", loadProperties("taggers/posTagWeights.props"), true, true);
			writer.write("trec_eval.9.0/trec_eval.exe -M1000 test-data/qrels.trec6-8.nocr result/result_QP_" + String.format("%.2f", zoneBoosts[i][0]) + "_" + String.format("%.2f", zoneBoosts[i][1]) + "_" + String.format("%.2f", zoneBoosts[i][2]) + ".out | tee result/result_QP_" + String.format("%.2f", zoneBoosts[i][0]) + "_" + String.format("%.2f", zoneBoosts[i][1]) + "_"
					+ String.format("%.2f", zoneBoosts[i][2]) + ".eval\n");
		}
		writer.close();
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
