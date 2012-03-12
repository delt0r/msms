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
package at.mabs.coalescent;

/**
 * simple event object that can be used as a "closure" kinda.
 * 
 * We use a more struct type format.
 * 
 * @author bob
 * 
 */
public class CoalescentEvent {
	public static final CoalescentEvent NO_OP=new CoalescentEvent(Double.MAX_VALUE){
		public String toString() {return "NO_OP";};
	};
	public final double dt;
	

	public CoalescentEvent(double dt) {
		this.dt =dt;
	}

	public void completeEvent() {
		
	}
	
	
}
