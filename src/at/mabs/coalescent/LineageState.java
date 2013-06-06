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

import java.util.*;

import at.mabs.model.ModelHistroy;
import at.mabs.model.selection.SelectionData;

//import at.mabs.segment.SegmentEventRecoder;
//import at.mabs.segment.SegmentSet;
//import at.mabs.segment.SegmentSetFactory;

import at.mabs.util.Bag;
import at.mabs.util.PartialSumsTree;
import at.mabs.util.Util;
import at.mabs.util.random.RandomGenerator;

//import at.mabs.util.cuckoo.CFieldHashSet;
//import at.mabs.util.cuckoo.CHashSet;

/**
 * represents the state of the current lineages. ie number demes and the number of nodes in each
 * allele type in each deme. Was quite immutable .. but now we can mutate the state quite a bit.
 * 
 * 
 * 
 * @author bob
 * 
 */
/*
 * the demeSize can change. To avoid constant reallocation, the lineages object can be oversized. Do
 * not use lineages.length except for maxDemes;
 */
public class LineageState<T extends LineageData<T>> {
	private final Random random =RandomGenerator.getRandom();
	private int currentDemeCount;

	private final double alleleLocation;
	private boolean selection;

	// first level is the deme id. Next is the alleleId. Finally the nodes in
	// that deme/allele
	private Bag<T>[][] lineages;
	// private Set<T> allLineages;// quicker for some iterations
	private PartialSumsTree<T> partialSumsTree;

	private double currentTime;
	private double currentMaxTime;
	
	private double maxRecombinationRate=Double.MAX_VALUE;

	// private boolean isRecombination;

	private int totalSamples;
	private int samplesLeft;

	// private double totalRecombinationWeight;

	// private final SegmentEventRecoder mutationModel;
	private final ModelHistroy modelHistroy;

	private final LineageDataFactory<T> linageDataFactory;
	private int selectionMutationCount =0;
	private SelectionData selectionData;

	// public LineageState(int maxDemeCount, int currentDemeCount,MutationModel
	// model) {
	// this(maxDemeCount, currentDemeCount, Double.NaN,model);
	// }

	public LineageState(ModelHistroy modelHistory, LineageDataFactory<T> factory) {
		this.currentDemeCount =modelHistory.getFirstModelDemeCount();
		lineages =new Bag[modelHistory.getMaxDemeCount()][2];
		// allLineages =new HashSet<T>();
		for (int i =0; i < lineages.length; i++) {
			lineages[i][0] =new Bag<T>();
			lineages[i][1] =new Bag<T>();
		}
		alleleLocation =modelHistory.getAlleleLocation();
		// this.mutationModel =model;
		this.modelHistroy =modelHistory;
		this.linageDataFactory =factory;
		partialSumsTree =new PartialSumsTree<T>();
		maxRecombinationRate=modelHistory.getMaxRecombinationRate();
	}

	/**
	 * 
	 * @param samples
	 * @return Leaf nodes
	 */
	public void initState(SelectionData selectionData) {
		currentTime =0;
		selectionMutationCount =0;
		this.selectionData=selectionData;
		linageDataFactory.reset();
		currentDemeCount =modelHistroy.getFirstModelDemeCount();
		// System.out.println("Init:"+currentDemeCount);
		selection =false;
		// allLineages.clear();
		partialSumsTree.clear();
		samplesLeft=this.modelHistroy.getSampleConfiguration().getMaxSamples();
		for (int i =0; i < lineages.length; i++) {// we must clear everything

			for (int a =0; a < lineages[i].length; a++)
				lineages[i][a].clear();

		}
		// need to randomize
		for (int i =0; i < lineages.length; i++) {
			for (int a =0; a < lineages[i].length; a++) {
				Collections.shuffle(lineages[i][a], random);
			}
		}
		reCalculateRecombinationWeight();
		// assert totalRecombinationWeight==partialSumsTree.getTotalWeight();
	}

	public int getLinagesActive() {
		// System.out.println(calculateRecombinationWeight()+"\t"+totalRecombinationWeight);
		// assert totalSamples == allLineages.size();
		//System.out.println("TS:"+totalSamples+"\tSL:"+samplesLeft+"\t"+currentTime);
		return totalSamples+this.samplesLeft;
	}

	/**
	 * 
	 * 
	 * @param deme
	 * @param allele
	 * @return
	 */
	public int getLineageSize(int deme, int allele) {
		return lineages[deme][allele].size();
	}

	/**
	 * this number can change over time due to events.
	 * 
	 * At any current time however this should match the current model deme count.
	 * 
	 * @return
	 */
	public int getDemeCount() {
		return currentDemeCount;
	}

	public void setCurrentDemeCount(int currentDemeCount) {
		// System.out.println("DemeCounter:"+currentDemeCount);
		this.currentDemeCount =currentDemeCount;
	}

	/**
	 * The current time since sampling that this state currently represents...
	 * 
	 * @return
	 */
	public double getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(double currentTime) {
		this.currentTime =currentTime;
	}

	/**
	 * the max time this state should be set to without some "event" management.
	 * 
	 * @return
	 */
	public double getCurrentMaxTime() {
		return currentMaxTime;
	}

	public void setCurrentMaxTime(double currentMaxTime) {
		this.currentMaxTime =currentMaxTime;
	}

	public void coalescentEvent(int deme, int allele, double dt) {
		assert (currentTime + dt <= currentMaxTime) : "tried to create event past maxTime for interval:" + (currentTime + dt) + "\t" + currentMaxTime;
		if (currentTime + dt > currentMaxTime)
			throw new RuntimeException("Error, tried to create event past maxTime for interval");
		//System.err.println("Cevent:");
		
		currentTime +=dt;
		
		List<T> list =lineages[deme][allele];
		assert list.size()>=2;
		T a =list.remove(list.size() - 1);
		T b =list.remove(list.size() - 1);
		assert a.getAllele() == allele;
		assert a.getDeme() == deme : a.getDeme();
		assert b.getAllele() == allele;
		assert b.getDeme() == deme : b.getDeme();
		//check if they are valid coalescent events. 
		double rcombRate=0;
		if(a.getEnd()<b.getStart()){
			rcombRate=b.getStart()-a.getEnd();
		}else if(b.getEnd()<a.getStart()){
			rcombRate=a.getStart()-b.getEnd();
		}
		//System.out.println(rcombRate+"\t"+a+"\t"+b);
		if(rcombRate*modelHistroy.getRecombinationRate()>=maxRecombinationRate && rcombRate>0){
			//no coalescent!
			list.add(b);
			randomSwapEnd(list);
			list.add(a);
			randomSwapEnd(list);
			return;
		}
		
		//now we do it... 
		
		// allLineages.remove(b);// we only remove a if its empty
		partialSumsTree.remove(b);

		// totalRecombinationWeight -= getRecombinationWeight(a);
		// totalRecombinationWeight -= getRecombinationWeight(b);

		// SkipListSegmentSet n =null;

		// now we are suppose to have a union of all other nodes to get the
		// active regions
		// correct.
		// we have the nice situation that we have removed these two nodes..
		// Sequence union =null;
		// if (isRecombination) {
		// union =unionAllNodes();
		// }
		boolean selectedSiteContatined=a.contains(alleleLocation)|| b.contains(alleleLocation);
		a.join(b, currentTime);// Node.createCoalesentNode(a, b, currentTime);
		// now for the segementSet code.
		
		if (a.isEmpty()) {
			totalSamples -=2;
			//System.err.println("Cevent:Empty");
			
			// allLineages.remove(a);
			partialSumsTree.remove(a);
			//we note that a is removed. if the selection was containted here we need to mark it
			if(allele==1 && selectedSiteContatined){
				//System.err.println("REMOVE tha FRAKER ");
				int otherAllele=allele==1?0:1;
				a.markSelectedAllele(otherAllele, this.alleleLocation);//mutate out of this allele
			}
			// linageCount();
			return;
		}else{
			//System.err.println("Cevent:!Empty");
		}
		assert !a.isEmpty();
		list.add(a);
		randomSwapEnd(list);
		totalSamples--;
		partialSumsTree.updateWeight(a, getRecombinationWeight(a));
		// linageCount();
		// assert totalSamples == allLineages.size();
		// totalRecombinationWeight += getRecombinationWeight(a);
		// assert totalRecombinationWeight==partialSumsTree.getTotalWeight();
	}

	/*
	 * the union sequence of all nodes. Brute force for debug. We can be much smarter than this I
	 * think. Well not so far
	 */
	// private Sequence unionAllNodes() {
	// Sequence result =null;
	// for (int i =0; i < currentDemeCount; i++) {
	// for (int a =0; a < lineages[i].length; a++) {
	// List<Node> list =lineages[i][a];
	// for (int j =0; j < list.size(); j++) {
	// Node n =list.get(j);
	// if (result == null) {
	// result =n.getSequence().deepClone();
	// } else {
	// result.union(n.getSequence());
	// if (result.getMutationProbablityWeight() == 1)
	// return result;
	// }
	// }
	// }
	// }
	//
	// // System.out.println(result);
	// return result;
	// }

	/*
	 * This method mantains the randomness of lists with less overhead than random insertion or
	 * deletetion. It simply swaps the last element with a random draw from the contained elements
	 * including itself.
	 */
	private void randomSwapEnd(List<T> nodes) {
		int end =nodes.size() - 1;
		if (end == 0)
			return;
		// if(true){
		// Collections.shuffle(nodes);
		// return;
		// }

		int r =random.nextInt(end + 1);
		T n =nodes.get(end);
		// assert n.getSegmentSet().isEmpty()==false;
		nodes.set(end, nodes.get(r));
		nodes.set(r, n);

	}

	// public boolean isRecombination() {
	// return isRecombination;
	// }
	//
	// public void setRecombination(boolean isRecombination) {
	// this.isRecombination = isRecombination;
	// }

	public double getTotalRecombinationWeight() {
		return partialSumsTree.getTotalWeight();
	}

	private void reCalculateRecombinationWeight() {
		// double weight =0;
		// double span = 0;

		for (T set : partialSumsTree) {
			double w =getRecombinationWeight(set);
			// span += w;
			partialSumsTree.updateWeight(set, w);
		}
		// assert partialSumsTree.getTotalWeight()==span;

	}

	private double getRecombinationWeight(T data) {
		if (data.isEmpty())
			return 0;
		if (!selection)
			return data.getEnd() - data.getStart();
		return Math.max(data.getEnd(), modelHistroy.getAlleleLocation()) - Math.min(data.getStart(), modelHistroy.getAlleleLocation());
	}

	private double getRandomCutSite(T data) {
		assert !data.isEmpty();
		double start =data.getStart();
		double end =data.getEnd();
		if (selection) {
			start =Math.min(start, modelHistroy.getAlleleLocation());
			end =Math.max(end, modelHistroy.getAlleleLocation());
		}
		double r =(end - start) * random.nextDouble() + start;
		long recombSegments =modelHistroy.getRecombinationCutSites();
		if (recombSegments > 0)
			r =(double) ((int) (r * recombSegments)) / recombSegments;
		return r;
	}

	/**
	 * 
	 * @param totalRecoWeight
	 * @param dt
	 * @param frequency
	 */

	public double recombinationEvent(double totalRecoWeight, double dt) {
		
		// this is when we are selected.
		if (selectionData == null || !selection) {
			// recomTime +=(System.nanoTime() - time) * 1e-6;
			return recombinationEventNS(totalRecoWeight, dt);

			
		}
		double weight =random.nextDouble() * partialSumsTree.getTotalWeight();
		T node =partialSumsTree.select(weight);
		// now just find it.
		List<T> line =lineages[node.getDeme()][node.getAllele()];
		// assert line.contains(node);
		// int i =line.indexOf(node);

		// System.out.println("Recombination:"+dt+" al"+alleleLocation);
		int d =node.getDeme();
		if (node.getAllele() == 0) {
			if (selectionData.getFrequency(d, 0, currentTime + dt) > random.nextDouble()) {
				return recombinationEventInternal(node, 0, 0, dt);
			} else {
				return recombinationEventInternal(node, 0, 1, dt);
			}

			
		}
		if (selectionData.getFrequency(d, 1, currentTime + dt) > random.nextDouble()) {
			return recombinationEventInternal(node, 1, 1, dt);
		} else {
			return recombinationEventInternal(node, 1, 0, dt);
		}

		//return;

	}

	
	private double recombinationEventNS(double totalRecoWeight, double dt) {

		double weight =random.nextDouble() * partialSumsTree.getTotalWeight();
		T node =partialSumsTree.select(weight);
		// now just find it.
		List<T> line =lineages[node.getDeme()][node.getAllele()];

		// int index =line.indexOf(node);

		return recombinationEventInternal(node, 0, 0, dt);
		// for (int d =0; d < this.currentDemeCount; d++) {
		// List<T> line =lineages[d][0];
		// for (int i =0; i < line.size(); i++) {
		// weight -=getRecombinationWeight(line.get(i));// getCutWeight();
		// if (weight < 0) {
		// recombinationEventInternal(i, d, 0, 0, 0, dt);// note that the node
		// is
		// // removed
		// recomTime2 +=(System.nanoTime() - time) * 1e-6;
		// return;
		// }
		// }
		// }
		// assert false;

	}

	private double recombinationEventInternal(T node, int selectedAllele, int notSelectedAllele, double dt) {
		currentTime +=dt;
		double cutSite =getRandomCutSite(node);
		T otherNode =node.split(cutSite, partialSumsTree);

		int nodeAllele =selectedAllele;
		int otherAllele =notSelectedAllele;
		// first which node goes where...
		if (cutSite <= modelHistroy.getAlleleLocation()) {
			nodeAllele =notSelectedAllele;
			otherAllele =selectedAllele;
		}

		if (!otherNode.isEmpty()) {
			lineages[node.getDeme()][otherAllele].add(otherNode);
			randomSwapEnd(lineages[node.getDeme()][otherAllele]);
			otherNode.setDeme(node.getDeme());
			otherNode.setAllele(otherAllele);
			partialSumsTree.add(otherNode, getRecombinationWeight(otherNode));
			totalSamples++;
		}
		if (node.isEmpty()) {

			lineages[node.getDeme()][node.getAllele()].remove(node);
			partialSumsTree.remove(node);
			totalSamples--;
		} else {
			if (nodeAllele != node.getAllele()) {

				lineages[node.getDeme()][node.getAllele()].remove(node);
				lineages[node.getDeme()][nodeAllele].add(node);
				randomSwapEnd(lineages[node.getDeme()][nodeAllele]);
				node.setAllele(nodeAllele);

			}
			partialSumsTree.updateWeight(node, getRecombinationWeight(node));
		}
		// linageCount();
		// totalRecombinationWeight=partialSumsTree.getTotalWeight();
		// isRecombination = true;
		return cutSite;
	}

	/** 
	 * the allele is chosen as a random draw from the current allele frequencies. 
	 * @param deme
	 */
	public void addSample(int deme) {
		//System.err.println("Adding samples:"+samplesLeft);
		int allele=0;
		if(selection && selectionData!=null){
			if(selectionData.getFrequency(deme, 1, currentTime-1e-9)>random.nextDouble()){
				allele=1;//FIXME 2 allele model.
			}
			
		}
		T set =(T) linageDataFactory.createLineageData(this.currentTime);
		set.setAllele(allele);
		set.setDeme(deme);
		lineages[deme][allele].add(set);
		randomSwapEnd(lineages[deme][allele]);
		partialSumsTree.add(set, getRecombinationWeight(set));
		totalSamples++;
		samplesLeft--;
		assert samplesLeft>=0;
	}

	/**
	 * note that to and from is in the coalescent time sence. ie from it what it is in now. to is
	 * where it ends up in further into the past.
	 * 
	 * @param fromDeme
	 * @param toDeme
	 */
	public void migrationEvent(int fromDeme, int toDeme, int allele, double dt) {
		if (!selection && allele != 0) {
			throw new RuntimeException("Allele must be zero when there is no selection");
		}
		currentTime +=dt;

		// System.out.println("FromDeme:"+fromDeme+" to "+toDeme);
		// pretty simple really.
		List<T> list =lineages[fromDeme][allele];
		T n =list.remove(list.size() - 1);
		assert n.getDeme() == fromDeme;
		assert n.getAllele() == allele;
		n.setDeme(toDeme);
		list =lineages[toDeme][allele];
		list.add(n);

		randomSwapEnd(list);
		// linageCount();
	}

	public void alleleMutationEvent(int deme, int fromAllele, int toAllele, double dt) {
		// if (true)
		// throw new
		// RuntimeException("Allele mutation can only happen with selection");
		// }

		currentTime +=dt;
		// pretty simple really.
		List<T> list =lineages[deme][fromAllele];

		assert list.size() > 0;

		T n =list.remove(list.size() - 1);
		n.setAllele(toAllele);
		list =lineages[deme][toAllele];
		list.add(n);
		randomSwapEnd(list);
		
		//System.err.println("Doing some mutation crap:"+n.contains(alleleLocation)+"\t"+n.isEmpty());
		// we only consider directly observed "mutations", not mutations that are observed via
		// recombined hitch hiking section.
		// this means we let segment trackers etc do the dirty work.
		if (n.contains(alleleLocation) ) { 
			n.markSelectedAllele(toAllele, modelHistroy.getAlleleLocation());
		}
		selectionMutationCount++;
		// linageCount();
	}

	/**
	 * moves alleles without counting mutations. This is only called by Selection start!
	 * 
	 * @param deme
	 * @param fromAllele
	 * @param toAllele
	 */
	public void alleleInitalMoveEvent(int deme, int fromAllele, int toAllele) {
		List<T> list =lineages[deme][fromAllele];
		// randomSwapEnd(list);//pick at random.
		// System.out.println(list);
		// Collections.shuffle(list);
		// System.out.println(list);
		
		T n =list.remove(list.size() - 1);
		n.setAllele(toAllele);

		//n.markSelectedAllele(toAllele, modelHistroy.getAlleleLocation());

		list =lineages[deme][toAllele];
		list.add(n);
		randomSwapEnd(list);
		//Collections.shuffle(list);
		// linageCount();
	}

	/*
	 * we hope its the last node.
	 */
	// @Deprecated
	// Node getLastNode() {
	// assert false;
	// // System.out.println("Finding the root:"+totalSamples);
	// for (int i =0; i < currentDemeCount; i++) {
	// for (int a =0; a < lineages[i].length; a++) {
	// if (lineages[i][a].size() > 0) {
	// assert (lineages[i][a].size() == 1) : lineages[i][a].size();
	// return lineages[i][a].get(0);
	// }
	// }
	// }
	// //assert (false);
	// return null;
	// }

	public boolean isSelection() {
		return selection;
	}

	/**
	 * should be some kind of friend function for ModelEvents...
	 * 
	 * @param selection
	 */
	public void setSelection(boolean selection) {
		// System.out.println("SetSelection:"+this.selection+"->"+selection+" :"+totalSamples+" "+currentTime);
		this.selection =selection;
		reCalculateRecombinationWeight();// resets
		// partial
		// Sums as
		// well.
	}

	public String toString() {
		String s ="demes:" + currentDemeCount + " (";
		for (int i =0; i < currentDemeCount; i++) {
			s +=lineages[i][0].size() + "|" + lineages[i][1].size() + ",";
		}
		s +="trackSize=" + totalSamples + ")";
		return s;
	}

	public ModelHistroy getModelHistroy() {
		return modelHistroy;
	}

	public int getSelectionMutationCount() {
		return selectionMutationCount == 0 ? 1 : selectionMutationCount;// always one or more!
	}
	
	public void setSelectionData(SelectionData selectionData) {
		this.selectionData = selectionData;
	}
	
	public SelectionData getSelectionData() {
		return selectionData;
	}

	// private int linageCount(){
	// int c=0;
	// for(Bag<T>[] allele:lineages){
	// for(Bag<T> bag:allele){
	// c+=bag.size();
	// }
	// }
	// assert c==totalSamples;
	// return c;
	// }
}
