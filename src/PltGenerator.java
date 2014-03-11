import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

public class PltGenerator {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
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
		BufferedWriter writer = null;
		String type[][] = new String[1][32];
		String value[][] = new String[zoneBoosts.length][32];
		String zones[] = new String[] {
										"title_boost",
										"desc_boost",
										"narr_boost" };
		String zoneType = "Zone with POS";
		for (int i = 0; i < zoneBoosts.length; i++) {
			if (i % 5 == 0) {
				if (writer != null)
					writer.close();
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("result/" + zoneType + "/result_QP_" + zoneType + "_" + i / 5 + ".plt")));
				writer.write("TITLE = \"" + zoneType.toUpperCase() + " " + i / 5 + "\"\nVARIABLES =\n");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("result/" + zoneType + "/result_QP_" + String.format("%.2f", zoneBoosts[i][0]) + "_" + String.format("%.2f", zoneBoosts[i][1]) + "_" + String.format("%.2f", zoneBoosts[i][2]) + ".eval")));
			String line;
			for (int j = 0; (line = reader.readLine()) != null; j++) {
				StringTokenizer st = new StringTokenizer(line);
				while (st.hasMoreTokens()) {
					if (i == 0 && j != 0)
						type[i][j - 1] = st.nextToken();
					else
						st.nextToken();
					st.nextToken();
					if (j != 0)
						value[i][j - 1] = st.nextToken();
					else
						st.nextToken();

				}
			}
			reader.close();
			writer.close();
		}

		for (int i = 0; i < zoneBoosts.length / 5; i++) {
			if (writer != null)
				writer.close();
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("result/" + zoneType + "/result_QP_" + zoneType + "_" + i + ".plt", true)));
			for (int j = 0; j < 32; j++) {
				if (j >= 29)
					writer.write(String.format("%30s", zones[j - 29]));
				else
					writer.write(String.format("%30s", type[0][j]));
			}
			writer.write("\n");
		}
		writer.close();

		for (int i = 0; i < zoneBoosts.length; i++) {
			if (i % 5 == 0) {
				if (writer != null)
					writer.close();
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("result/" + zoneType + "/result_QP_" + zoneType + "_" + i / 5 + ".plt", true)));
			}
			for (int j = 0; j < 32; j++) {
				if (j >= 29)
					writer.write(String.format("%30s", zoneBoosts[i][j - 29]));
				else
					writer.write(String.format("%30s", value[i][j]));
			}
			writer.write("\n");
		}
		writer.close();
	}

}
