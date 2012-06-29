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
package at.mabs.model.selection;
/**
 * The frequency in a deme is larger or over all.  
 * @author bob
 *
 */
public class BasicFrequencyCondition implements FrequencyCondition {
	private final int deme;
	private final double frequency;
	private final long time;
	
	public BasicFrequencyCondition(long time,int deme,double f) {
		this.deme=deme;
		this.frequency=f;
		this.time=time;
	}
	
	@Override
	public boolean isMeet(double[] frequencys) {
		if(deme<0){
			double s=0;
			for(int i=0;i<frequencys.length;i++){
				s+=frequencys[i]/frequencys.length;
			}
			if(s>=frequency)
				return true;
		}else{
			return frequencys[deme]>=frequency;
		}
		return false;
	}
	
	@Override
	public long getTime() {
		return time;
	}

}
