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
package at.mabs.model;

import java.util.Arrays;

import at.mabs.coalescent.LineageState;
import at.mabs.model.selection.SelectionData;
import at.mabs.util.random.RandomGenerator;

/**
 * Implements the -ej switch in ms. ie we join i to j.
 * 
 * I want to delete i but thats not how ms works... its still a real
 * population... This does not affect coalescent simulations since it is
 * unobservable... But this is not the case with selection. If they set finite
 * migration or something after the join... Its pretty weird really (the way ms
 * does i mean).
 * 
 * for now we assume its deleting.. hence its population size it set to zero,
 * and it is ignored by the forward simulator...
 * 
 * @author bob
 * 
 */

public class DemeJoinEvent extends Event {
	private final int i, j;

	public DemeJoinEvent(long t, int i, int j) {
		super(t);
		this.i = i;
		this.j = j;
	}

	

}
