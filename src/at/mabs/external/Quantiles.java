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

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;

/**
 * calulate the given quantiles from a stream. Needs to fit into memeory.
 * 
 * @author bob
 * 
 */
public class Quantiles {
	private static final double THRID=1.0/3;
	public static void main(String[] args) {
		try {
			double[] quantiles =new double[args.length];
			for (int i =0; i < quantiles.length; i++)
				quantiles[i] =Double.parseDouble(args[i]);

			// now just read everything!
			LineNumberReader lnr =new LineNumberReader(new InputStreamReader(System.in));
			String line =lnr.readLine();
			
			List<Double> list=new ArrayList<Double>();
			
			while(line!=null){
				list.add(Double.parseDouble(line));
				line=lnr.readLine();
			}
			
			//now sort.
			Collections.sort(list);
			
			for(int i=0;i<quantiles.length;i++){
				double h=(list.size()+THRID)*quantiles[i]+THRID;
				double ih=Math.floor(h);
				
				double x_ih=list.get((int)ih);
				double x_ihp=list.get(1+(int)ih);
				
				double estimate=x_ih+(h-ih)*(x_ihp-x_ih);
				System.out.print(estimate+"\t");
			}
			System.out.println();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
