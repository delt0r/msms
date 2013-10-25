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

import java.util.Random;

import cern.jet.random.Binomial;

import at.mabs.coalescent.LineageState;
import at.mabs.model.selection.SelectionData;
import at.mabs.util.random.RandomGenerator;

/**
 * an implementation of the -es option in ms
 * 
 * @author bob
 * 
 */
public class DemeSplitEvent extends Event {
	private final int deme;
	private final double q, N;
	private int demeLabel;
	private Binomial binomial = RandomGenerator.getBinomial();

	public DemeSplitEvent(long t, int deme, double p, double N) {
		super(t);
		this.deme = deme;
		this.q = 1 - p;
		this.N = N;
	}

	

}
