package at.mabs.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * simple script to just output the count of sucsessive identical trees.
 * 
 * @author bob
 * 
 */
public class SameTrees {
	public static void main(String[] args) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			// this is simple... we parse out the [xx] bit first then compare to
			// the last line.
			String line = reader.readLine();
			String lastLine = null;
			int count = 0;
			while (line != null) {
				StringTokenizer st = new StringTokenizer(line.trim(), "[]");
				if (st.countTokens() > 1) {
					st.nextToken();
					String thisLine = st.nextToken();
					if (thisLine.equals(lastLine)) {
						count++;
					}
					lastLine = thisLine;
				} else {
					if (count > 0) {
						System.out.println(count);
					}
					count = 0;
					lastLine = null;
				}

				line = reader.readLine();
			}
			//System.out.println(count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
