package at.mabs.external;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * a cvs double file without header and turn into binary data. First row defines column count, incorrect column rows are ignored
 * 
 * @author bob
 * 
 */
public class TxtDouble2bin {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(System.out));

			String line = br.readLine();
			StringTokenizer st = new StringTokenizer(line, "\t\n, ");
			int ccount = st.countTokens();
			dos.writeUTF("BinaryDoubleData columnCount: " + ccount + "\n");
			while (line != null) {
				try {
					st = new StringTokenizer(line, "\t\n, ");
					if (st.countTokens() == ccount) {
						while (st.hasMoreElements()) {
							dos.writeDouble(Double.parseDouble(st.nextToken()));
						}
					} else {
						System.err.println("WARNING, Line ignored incorrect col count:\n" + line);
					}
				} catch (Exception e) {
					System.err.println("WARNING, Line missed due to exception:\n" + e);
				}
				line = br.readLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
