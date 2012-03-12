/*
This code is licensed under the LGPL v3 or greater with the classpath exception, 
with the following additions and exceptions.  

packages cern.* have retained the original cern copyright notices.

packages at.mabs.cmdline and at.mabs.util.* 
have the option to be licensed under a BSD(simplified) or Apache 2.0 or greater  license 
in addition to LGPL. 

Note that you have permission to replace this license text to any of the permitted licenses. 

Main text for LGPL can be found here:
http://www.opensource.org/licenses/lgpl-license.php

For BSD:
http://www.opensource.org/licenses/bsd-license.php

for Apache:
http://www.opensource.org/licenses/apache2.0.php

classpath exception:
http://www.gnu.org/software/classpath/license.html
*/
package at.mabs.external;

import java.io.*;

/**
 * simple program to turn ms input into frequences.
 * 
 * @author bob
 * 
 */
public class MSFreq {
	/**
	 * reads untill it finds a segsites: then dumps the positions line then parses the binary strings..
	 * 
	 * @param lnr
	 * @return
	 * @throws IOException
	 */
	public static final int[] nextSimulationFrequencys(LineNumberReader lnr) throws IOException {
		String s =lnr.readLine();
		while (s != null && !s.contains("segsites")) {
			s =lnr.readLine();
		}
		if (s == null) {
			return null;
		}
		// we are the segsite line
		int size =Integer.parseInt(s.split(":")[1].trim());
		int[] freq =new int[size];
		// dump the postions line
		
		s =lnr.readLine();
		while (s != null && !(s.startsWith("1") || s.startsWith("0"))) {
			
			//this is needed for the size ==0 case
			if(s.contains("//"))
				return freq;
			s =lnr.readLine();
		}
		// regular expression match might be a bit slow.
		while (s != null && (s.startsWith("1") || s.startsWith("0"))) {
			//System.out.println(s+"\t\t::::"+size+"\t"+lnr.getLineNumber());
			for (int i =0; i < s.length(); i++) {
				if (s.charAt(i) == '1')
					freq[i]++;
			}
			s =lnr.readLine();
		}
		return freq;
	}

	public static void main(String[] args) {
		// assume file filter
		try {
			LineNumberReader lnr =new LineNumberReader(new InputStreamReader(System.in));
			int[] f =MSFreq.nextSimulationFrequencys(lnr);
			while (f!=null) {
				for(int i=0;i<f.length;i++){
					System.out.println(f[i]);
				}
				f =MSFreq.nextSimulationFrequencys(lnr);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
