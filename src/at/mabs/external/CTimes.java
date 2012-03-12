package at.mabs.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.StringTokenizer;

import at.mabs.util.Util;

/**
 * simple script to just output the count of sucsessive identical trees.
 * 
 * @author bob
 * 
 */
public class CTimes {
	public static void main(String[] args) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			// this is simple... we parse out the [xx] bit first then compare to
			// the last line.
			String line = reader.readLine();

			List<Double> list = new ArrayList<Double>();
			while (line != null) {

				if (line.length() > 0 && line.charAt(0) == '[' && line.endsWith(";")) {
					// System.out.println("TREE:"+line);
						list.clear();
						StringTokenizer st = new StringTokenizer(line.trim(), "[]");
						st.nextToken();
						String thisLine = st.nextToken();
						// now we parse trees... only we don't really. We just
						// pull numbers after :
						pullNumbers(thisLine, list);
						
					

				} else if (!list.isEmpty()) {
					Collections.sort(list);
					for (double d : list) {
						System.out.print(d + "\t");
					}
					System.out.println();
					list.clear();
				}

				line = reader.readLine();
			}
			// System.out.println(count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void pullNumbers(String s, Collection<Double> list) {
		int i = 0;

		StringTokenizer st = new StringTokenizer(s, "(),:");
		while (st.hasMoreElements()) {
			String n = st.nextToken();
			if (n.contains(".")) {
				list.add(Double.parseDouble(n));
			}
		}

	}
}
