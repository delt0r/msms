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
package at.mabs.stats;

import java.io.PrintStream;

import at.mabs.segment.SegmentEventRecoder;

/**
 * legacy implenation of stats collecting. Will stay for ms compatable output for now. 
 * 
 * A group of these object are responable for interating with
 * SegmentEventRecorders to provid usefull output.
 * 
 * This can include summary statistics and other things like LD tables.
 * 
 * The life cycle is simple. collect stats is called for each result. Then
 * finally the summary is called. Generally it can be assumed that collectStats
 * will not be called again after summary
 * 
 * WARNING. The methods on this class can be called by different threads. However only one
 * thread at any one time will call collectStats/summary. Hence no explicit sycnronization should
 * be required. 
 * 
 * This should perhaps get a little deprecated? or at least become the "special case" and not the norm.
 * 
 * @author greg
 * 
 */
public interface StringStatsCollector {
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder);

	public void summary(StringBuilder builder);
}
