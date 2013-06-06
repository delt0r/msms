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

import java.util.*;




/**
 * a segment for recombination tracking. That is this represents a short segment of neutral sequence
 * --regardless of the mutation model. There must be a segment store and all access to all
 * components that come from a segment store should always be from a single thread.
 * 
 * The idea here is that for a given interval there is just one instance of a segment. Hence we 
 * can keep track of how many linages own a segment via its count. Otherwise at each merge we must 
 * test all other lineages if they still track this segment or any part of the segment.
 * 
 * WARNING compareTo is NOT consitant with equals. 
 * 
 * @author bob
 * 
 */
public class Segment implements Comparable<Segment> {
	

	// after we are finished we have a subTree length. This can help with
	// mutation models
	// private double subTreeLength;

	

	// the segment --there is no restriction on the values of start and end
	// other than
	// start<end. this implies that start and end are never NaN. The also should
	// order is based on the start postion. A single segement store should never
	// have
	// 2 segments with the same start postion.
	double start, end;//getters? setters?

	private int linageCount;

	Segment(double s, double e) {
		start =s;
		end =e;
		
	}

//	Segment(double s, double e, Segment superSet) {
//		start =s;
//		end =e;
//		
//
//	}

	/**
	 * cut this segment at p. If p<start or p>end then this is an error.
	 * 
	 * This instance keeps the current start value. The returned segment has p as a start value;
	 * 
	 * @param p
	 * @return
	 */
	// Segment cut(double p){
	// assert(start>p);
	// assert(end<p);
	// Segment newSeg=parent.createSegment(p,end);
	// end=p;
	// return newSeg;
	// }

	public double getStart() {
		return start;
	}

	public double getEnd() {
		return end;
	}

	
	/**
	 * equal if overlap. Test on open intervals. 
	 * 
	 * Warring NOT CONSISTANT with equals. 
	 */
	@Override
	public int compareTo(Segment o) {
		if (start >= o.end)
			return 1;
		if (end <= o.start)
			return -1;
		// assert (start==o.start)== (end==o.end);
		return 0;
	}

	/**
	 * WARNING not consitant with compareTo.
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Segment) {
			Segment s =(Segment) obj;
			return s.start == start && s.end == end;
		}
		return false;
	}

	@Override
	public int hashCode() {
		long a =Double.doubleToRawLongBits(start);
		long b =Double.doubleToRawLongBits(end);
		return (int) ((a * 13) ^ (b * 17) + (a >>> 32));
	}

	public int getLinageCount() {
		return linageCount;
	}

	public void setLinageCount(int linageCount) {
		this.linageCount =linageCount;
	}

	

	public String toString() {
		return this.start + "->" + this.end;
	}

	public int decrementLineageCount() {
		linageCount--;
		return linageCount;
	}
	
	/**
	 * min -1 operation
	 * @param s
	 * @return
	 */
	public int decrementLineageCount(Segment s) {
		linageCount=Math.min(linageCount, s.linageCount);
		linageCount--;
		return linageCount;
	}

	/**
	 * 
	 * @param splitPoint
	 * @return {this,new}
	 */
	public Segment[] cut(double splitPoint) {
		assert start < splitPoint : "seg:" + start + "\t" + splitPoint;
		assert end > splitPoint;
		Segment newSegmentHigh =new Segment(splitPoint, end);
		Segment newSegmentLow =this;//new Segment(start, splitPoint);FIXME
		newSegmentLow.end=splitPoint;
		newSegmentHigh.setLinageCount(getLinageCount());
		newSegmentLow.setLinageCount(getLinageCount());

		return new Segment[] { newSegmentLow, newSegmentHigh };
	}

}
