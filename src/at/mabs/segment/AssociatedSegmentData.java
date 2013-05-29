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

import at.mabs.util.Util;

/**
 * It is a class for now. However this is probably best as a interface with an appropriate factory.
 * <p>
 * This is so that segments can be a single instance. While some other data need to be per lineage or
 * more accurately per SegmentSet. This is that data without complicating the segmentSet. 
 * <p>
 * The operations on this are as per a segment set. Split and join. Split clones this data in the proper way. 
 * That is it is niether a shallow copy nor a deep copy. Since parts need to be deep clones other do not. 
 * <p>
 * At a basic level this class tracks the leaf set via a leaf bit set. A tree string, the last "coalescent"
 * event and a link to the mutation model. The mutation model is *not* cloned. 
 * <p>
 * A join carries out mutation calculations in a abstract way. This also means we do not need to track trees 
 * in any way or form. This when recombination rates are high we save a huge amount of allocation. 
 * <p>
 * NOTE that this and SegmentEventRecorder are a little tightly coupled. Factorys may need to generate compatable types.
 * 
 * @author bob
 * 
 */
public class AssociatedSegmentData implements SegmentTracker<AssociatedSegmentData> {
	private StringBuilder treeString;
	private double lastJoinTime;
	private FixedBitSet leafSet;
	private final SegmentEventRecoder mutationModel;
	private final boolean storeTrees;
	private double treeLength=0;
	
	private AssociatedSegmentData(AssociatedSegmentData data) {
		
		this.lastJoinTime=data.lastJoinTime;
		setLeafSet(new FixedBitSet(data.getLeafSet()));
		mutationModel=data.mutationModel;
		storeTrees=data.storeTrees;
		if(storeTrees){
			this.treeString=new StringBuilder(data.treeString);
		}
		this.treeLength=data.treeLength;
	}
	
	public AssociatedSegmentData(int leafNumber,int maxLeafNumber,double time, SegmentEventRecoder model) {
		
		setLeafSet(new FixedBitSet(maxLeafNumber));
		getLeafSet().set(leafNumber);
		this.mutationModel=model;
		this.storeTrees=model.isTrackTrees();
		if(storeTrees){
			treeString=new StringBuilder(""+(leafNumber+1));//ms counts from 1.
		}
		lastJoinTime=time;//even a leaf can have a non zero last join time!
	}
	
	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#split()
	 */
	@Override
	public AssociatedSegmentData split(){
		return new AssociatedSegmentData(this);
	}
	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#join(at.mabs.segment.AssociatedSegmentData, at.mabs.segment.Segment, double)
	 */
	@Override
	public void join(AssociatedSegmentData data,Segment seg,double time){
		//treeString=new StringBuilder("("+treeString+":"+(time-lastJoinTime)+","+data.treeString+":"+(time-data.lastJoinTime)+")");
		if(storeTrees){
			treeString.insert(0, '(');//could be very slow.
			treeString.append(':');
			treeString.append(Util.defaultFormat.format((time-lastJoinTime)*mutationModel.getGenerationScale()));
			treeString.append(',');
			treeString.append(data.treeString);
			treeString.append(':');
			treeString.append(Util.defaultFormat.format((time-data.lastJoinTime)*mutationModel.getGenerationScale()));
			treeString.append(')');
		}
		
		mutationModel.addMutations(seg, getLeafSet(), time-lastJoinTime);
		mutationModel.addMutations(seg, data.getLeafSet(), time-data.lastJoinTime);
		
		this.treeLength+=(time-lastJoinTime)+(time-data.lastJoinTime)+data.treeLength;
		
		this.lastJoinTime=time;
		getLeafSet().or(data.getLeafSet());
	}	
	
	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#markSelectedAllele(int, double)
	 */
	@Override
	public void markSelectedAllele(int allele,double p){
		mutationModel.markSelectedAllele(allele,p,leafSet);
	}
	
	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#finishedWithSegment(at.mabs.segment.Segment)
	 */
	@Override
	public void finishedWithSegment(Segment seg){
		assert seg.getLinageCount()<=1;
		mutationModel.finishedWithSegment(this, seg);
	}
	
	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#getTreeString()
	 */
	@Override
	public StringBuilder getTreeString() {
		return treeString;
	}

	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#setLeafSet(at.mabs.segment.LeafSet)
	 */
	@Override
	public void setLeafSet(FixedBitSet leafSet) {
		this.leafSet = leafSet;
	}

	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#getLeafSet()
	 */
	@Override
	public FixedBitSet getLeafSet() {
		return leafSet;
	}
	
	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#getSegmentHeight()
	 */
	@Override
	public double getSegmentHeight(){
		return lastJoinTime;
	}
	
	/* (non-Javadoc)
	 * @see at.mabs.segment.SegmentTracker#getSegmentTreeLength()
	 */
	@Override
	public double getSegmentTreeLength(){
		return treeLength;
	}
}
