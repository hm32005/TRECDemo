import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrecQueryGenerator{

	protected BufferedReader rdr;
	protected boolean at_eof = false;

	public void writeQuery() {
		try {
			rdr = new BufferedReader(new InputStreamReader(new FileInputStream("test-data/topics.301-450"), "UTF-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		HashMap<Integer, String> hm = new HashMap<Integer, String>();
		StringBuffer sb = new StringBuffer();
		StringBuffer sb_title = new StringBuffer();
		StringBuffer sb_desc = new StringBuffer();
		StringBuffer sb_narr = new StringBuffer();
		try {
			String line;
			Pattern docno_tag = Pattern.compile("<num>\\s*(\\S+)\\s*(\\d+)\\s*");
			boolean in_desc = false;
			boolean in_narr = false;
			String docno = null;
			Pattern p = Pattern.compile("[/\\(\\)\\?-]");
			while (true) {
				line = rdr.readLine();
				if (line == null) {
					at_eof = true;
					break;
				}
				if (line.startsWith("<title>")) {
				    Matcher m = p.matcher(line);
			    	line = m.replaceAll(" ");
					sb_title.append(new String(line.substring(line.indexOf("<title>") + "<title>".length())));
				}
				if (line.startsWith("<desc>")) {
					in_desc = true;
					continue;
				}
				if (line.startsWith("<narr>")) {
					in_desc = false;
					in_narr = true;
					continue;
				}
				if (line.startsWith("</top>")) {
					in_narr = false;
					sb.append(docno + " " + sb_title + "~" + sb_desc + "~" + sb_narr);
					hm.put(Integer.parseInt(docno), sb.toString());
					sb = new StringBuffer();
					sb_title = new StringBuffer();
					sb_desc = new StringBuffer();
					sb_narr = new StringBuffer();
				}
				if (in_desc) {
				    Matcher m = p.matcher(line);
			    	line = m.replaceAll(" ");
					sb_desc.append(line + " ");
				}
				if (in_narr) {
				    Matcher m = p.matcher(line);
			    	line = m.replaceAll(" ");
					sb_narr.append(line + " ");
				}
				if (line.startsWith("<num>")) {
					Matcher m = docno_tag.matcher(line);
					if (m.find()) {
						docno = m.group(2);
					}
				}
				
			}
			TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>(hm);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("test-data/complete-queries.301-450")));
			for(Entry<Integer, String> e: treeMap.entrySet()){
				writer.write(e.getValue() + "\n");
			}
			writer.close();
		} catch (IOException e) {
			
		}
	}

}
