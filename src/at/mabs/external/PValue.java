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
 * p value based on given cutoffs. The cut offs can just be from quantile function in R. 
 * @author bob
 *
 */
public class PValue {

	public static void main(String[] args) {
		try{
		double low=Double.parseDouble(args[0]);
		double high=Double.parseDouble(args[1]);
		
		LineNumberReader lnr=new LineNumberReader(new InputStreamReader(System.in));
		String line=lnr.readLine();
		
		long totalCount=0;
		long rejectCount=0;
		
		while(line!=null){
			totalCount++;
			double v=Double.parseDouble(line);
			if(v<low || v>high)
				rejectCount++;
			
			line=lnr.readLine();
		}
		lnr.close();
		System.out.println((double)rejectCount/totalCount);
		
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
