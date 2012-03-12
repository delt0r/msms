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
package at.mabs.segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import at.mabs.coalescent.LineageDataFactory;
import at.mabs.coalescent.LineageState;
import at.mabs.model.ModelHistroy;
import at.mabs.util.Bag;
import at.mabs.util.Util;

/**
 * This is now a simple factory to make SegmentSets that track segments. Note that you cannot create
 * more leaves than the total number of linages from a single instance of this class. You need to
 * reinstanate the class each time. 
 * 
 * @author bob
 * 
 */
public class SegmentSetFactory implements LineageDataFactory<SegmentSet>{

	private Collection<Segment> segSet;//inital loci pattern. 

	private int atomicLeafCount =0;
	private int lineageCount;
	private double[] sortedIntervals;

	private final SegmentEventRecoder recorder;
	private double allelePosition;

	/**
	 * need the inital loci pattern... a list of doubles. a pair for each loci
	 */
	public SegmentSetFactory(ModelHistroy model, SegmentEventRecoder recorder) {
		double[] loci =model.getLociConfiguration();
		assert loci.length > 1 && (loci.length & 1) == 0:Arrays.toString(loci);
		sortedIntervals =loci.clone();
		Arrays.sort(sortedIntervals);
		this.lineageCount =model.getSampleConfiguration().getMaxSamples();
		this.recorder =recorder;
		// now create the basics.
		this.allelePosition=model.getAlleleLocation();
		initLoci();
	}
	
	
	private void initLoci(){
		ArrayList<Segment> all =new ArrayList<Segment>();
		for (int i =0; i < sortedIntervals.length; i +=2) {
			Segment seg =new Segment(sortedIntervals[i], sortedIntervals[i + 1]);
			seg.setLinageCount(lineageCount);
			all.add(seg);

		}
		segSet =all;
	}
	
	/**
	 * only lineage number of SegmentSets can be create from this. 
	 * @return
	 */
	public SegmentSet createLineageData(double time) {
		assert atomicLeafCount < lineageCount : atomicLeafCount;
		SegmentSet set =new SegmentSet(this);//the inital set of netrual loci. Default a single segment.
		SegmentTracker data =new AssociatedSegmentData(atomicLeafCount, lineageCount,time, recorder);
		atomicLeafCount++;
		set.addAll(segSet, data);
		// addSegmentSet(set);
		return set;// sets.get(0);
	}
	
	@Override
	public void reset() {
		atomicLeafCount=0;
		initLoci();
	}


	public double getAllelePosition() {
		return allelePosition;
	}
}
