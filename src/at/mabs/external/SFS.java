package at.mabs.external;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;

/**
 * lines of SFS --easy right.
 * 
 * @author bob
 * 
 */
public class SFS {
	public static void main(String[] args) {
		try {
			// first parse the "command line" searching for the -I option.
			BufferedReader reader =new BufferedReader(new InputStreamReader(System.in));
			String line =reader.readLine();

			Map<Integer, Integer> map =new HashMap<Integer, Integer>();
			while (true) {
				while (line != null && !line.startsWith("pos")) {
					line =reader.readLine();
				}
				if (line == null)
					return;

				// we need to read the lines *after* the pos
				map.clear();
				int samples =0;
				line =reader.readLine();
				while (line != null && line.trim().length() > 0) {
					for (int i =0; i < line.length(); i++) {
						char c =line.charAt(i);
						if (c == '0') {
							continue;
						}
						if (!map.containsKey(i)) {
							map.put(i, 1);
							continue;
						}
						int count =map.get(i);
						map.put(i, count + 1);
					}
					samples++;
					line =reader.readLine();
				}
				// now for the output...
				List<Integer> counts =new LinkedList(map.values());

				for (int i =1; i < samples; i++) {
					int total =0;
					for (int e : counts)
						if (e == i)
							total++;
					System.out.print(total+"\t");
				}
				System.out.println();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
