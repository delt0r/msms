package at.mabs.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class TreeWidth {
	public static void main(String[] args) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			// this is simple... we parse out the [xx] bit first then compare to
			// the last line.
			String line = reader.readLine();
			String lastLine = null;
			int count = 0;
			while (line != null) {
				if (line.length()>0 && line.charAt(0)=='[' && line.endsWith(";")) {
					//System.out.println("TREE:"+line);
					StringTokenizer st = new StringTokenizer(line.trim(), "[]");
					String s = st.nextToken();
					s = st.nextToken();
					System.out.println(s);
				}
				line = reader.readLine();
			}
			// System.out.println(count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
