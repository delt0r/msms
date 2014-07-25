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

import at.mabs.cern.jet.random.Poisson;
import at.mabs.model.ModelHistroy;
import at.mabs.util.Bag;
import at.mabs.util.PartialSumTreeElement;
import at.mabs.util.PartialSumsTree;
import at.mabs.util.Util;
import at.mabs.util.random.RandomGenerator;


/**
 * This is really the "bridge" between the coalesent events (joins/splits etc)
 * and what to use that for. In this case mutation data. This can mean tracking
 * trees etc. For now the interface is "folded" into the mutation object. But
 * this should perhaps change. Esp since adding finite sites means we now have 3
 * main ways of tracking and calculating mutations.
 * 
 * Again a class that perhaps should be an interface. We can do that with
 * refactoring tools when it matters.
 * <p>
 * This is the "gloabal" mutation object. It keeps track of all mutations and
 * where they belong. We will provide some output stuff here too.
 * <p>
 * This is more of a "state" tracker. keeping track of all results that we want
 * to keep. Such as mutations and trees.
 * <p>
 * Its a recorder. As in it records interesting things that happen. So that
 * correct output data can be produced, or has been stored
 * 
 * @author bob
 * 
 */
public class SegmentEventRecoder {

	private Bag<InfinteMutation> mutations = new Bag<InfinteMutation>();
	private TreeMap<Segment, AssociatedSegmentData> segmentData = new TreeMap<Segment, AssociatedSegmentData>();

	private Poisson poission = RandomGenerator.getPoisson();
	private Random random = RandomGenerator.getRandom();

	// private final double neutralMutationRate;
	private double generationScale;

	private boolean trackTrees;
	private long recombinationCuts;
	// private final boolean zeroRecombination;

	private ModelHistroy modelHistory;// used for mutation data.

	private boolean foldMutations = false;
	private boolean unPhase = false;
	private boolean weightedMutations = false;

	private boolean isConditionalMutation;
	private PartialSumsTree<TreePiece> pieces = new PartialSumsTree<TreePiece>();

	//private ArrayList<TreePiece> orderedPieces = new ArrayList<TreePiece>();// for
																			// finite
																			// sites
	// private int treePieceCount;

	private boolean finished = false;
	private boolean sorted = false;

	private List<FixedBitSet> selectedLeafSets = new ArrayList<FixedBitSet>();
	private List<Integer> selectedLeafAlleles = new ArrayList<Integer>();

	private boolean addSelectedAlleleMutations;

	private int totalLeafCount;

	public SegmentEventRecoder(ModelHistroy modelHistory) {
		this.modelHistory = modelHistory;
		this.generationScale = 1.0 / (4 * modelHistory.getN());
		recombinationCuts = modelHistory.getRecombinationCutSites();
		// zeroRecombination=modelHistory.getRecombinationRate()==0;
		totalLeafCount = modelHistory.getSampleConfiguration().getMaxSamples();
		foldMutations = modelHistory.isFoldMutations();
		unPhase = modelHistory.isUnphase();
		weightedMutations=modelHistory.isWeightedMutations();
	}

	/**
	 * just to send into stats collectors. Should point out that this will
	 * create a broken segEvenRecorder.
	 * 
	 * @param mutations
	 * @param fold
	 * @param unPhase
	 */
	public SegmentEventRecoder(Bag<InfinteMutation> mutations, boolean fold, boolean unPhase) {
		this.mutations = mutations;
		this.foldMutations = fold;
		this.unPhase = unPhase;
		if (foldMutations) {
			foldFilter();
		}
		if (unPhase) {
			unPhaseFilter();
		}
	}

	// should push this stuff into a mutation model.
	public void setConditionalMutation(boolean isConditionalMutation) {
		this.isConditionalMutation = isConditionalMutation;
	}

	public void setTrackTrees(boolean trackTrees) {
		this.trackTrees = trackTrees;
	}

	public void addMutations(Segment segment, FixedBitSet s, double l) {
		assert !finished;
		if (!isConditionalMutation) {
			addMutationsNow(segment, s, l);
			return;
		}
		double weight = (segment.getEnd() - segment.getStart()) * l;
		TreePiece tp = new TreePiece(new FixedBitSet(s), segment.getStart(), segment.getEnd(), weight);
		pieces.add(tp, weight);

	}

	private void addMutationsNow(Segment segment, FixedBitSet s, double l) {
		double mutSpan = segment.getEnd() - segment.getStart();
		double mean = l * mutSpan * modelHistory.getNeutralMutationRate();
		int number = poission.nextInt(mean);
		if (number == 0)
			return;
		s = new FixedBitSet(s);// copy instance
		double start = segment.getStart();
		for (int i = 0; i < number; i++) {
			double p = random.nextDouble() * mutSpan + start;
			mutations.add(new InfinteMutation(p, s));
		}
	}

	/**
	 * if we are tracking selected alleles in the sample... we add them here.
	 * assuming a single allele.
	 * 
	 * @param allele
	 * @param p
	 */

	public void markSelectedAllele(int allele, double p, FixedBitSet leafSet) {
		// System.out.println("EventRecorderMark:"+p+"\t"+leafSet);
		assert p == modelHistory.getAlleleLocation();

		selectedLeafSets.add(new FixedBitSet(leafSet));
		selectedLeafAlleles.add(allele);
		// System.out.println(leafSet);
		// if(allele!=0)
		// throw new RuntimeException("Smeg");
	}

	public void finishedWithSegment(AssociatedSegmentData data, Segment seg) {
		assert !finished;
		assert seg.getLinageCount() <= 1;
		segmentData.put(seg, data);
	}

	/**
	 * used by AssosicatedSegmentData to scale trees.
	 * 
	 * @return
	 */
	public double getGenerationScale() {
		return generationScale;
	}

	/**
	 * tells this class that all events have been recorded. It can now do
	 * whatever internal book keeping it wants. clear must be called to use
	 * again.
	 */
	public void finishRecording() {
		finished = true;
		if (isConditionalMutation) {
			if (!weightedMutations) {
				for (int i = 0; i < modelHistory.getSegSiteCount(); i++) {
					TreePiece tp = pieces.select(random.nextDouble() * pieces.getTotalWeight());
					double p = random.nextDouble() * (tp.stop - tp.start) + tp.start;
					mutations.add(new InfinteMutation(p, tp.leafSet));
				}
			} else {
				//single weighted mutation per edge. 
				double scale=modelHistory.getSegSiteCount()/pieces.getTotalWeight();
				for(TreePiece tp:pieces){
					double p = random.nextDouble() * (tp.stop - tp.start) + tp.start;
					mutations.add(new InfinteMutation(p, tp.leafSet,tp.weight*scale));
				}
			}
		}
		// fold the spectrum!
		if (foldMutations) {
			// System.out.println("FOLDING!!");
			foldFilter();
		}
		if (unPhase) {
			unPhaseFilter();
		}

		// System.out.println(modelHistory.getAlleleLocation()+"\t"+selectedLeafSet);
		if (addSelectedAlleleMutations) {
			FixedBitSet union = new FixedBitSet(this.totalLeafCount);
			// go from last added to first. clearing or adding as required.

			for (int i = selectedLeafAlleles.size() - 1; i >= 0; i--) {
				int allele = selectedLeafAlleles.get(i);// what we change to
														// pastward!
				FixedBitSet leafSet = selectedLeafSets.get(i);
				// System.out.println("A:"+allele+"\t"+leafSet);
				if (allele == 0) {
					union.or(leafSet);
				} else {
					leafSet.invert();
					union.and(leafSet);
					leafSet.invert();// just in case
				}
			}
			mutations.add(new MultiOriginMutation(modelHistory.getAlleleLocation(), union, selectedLeafSets));// new
																												// InfinteMutation(modelHistory.getAlleleLocation(),
																												// selectedLeafSet));
		}

		// Collections.sort(mutations);//FIXME slower than we would like... its
		// the array copy that is probably half the issue
	}

	private void foldFilter() {
		for (InfinteMutation m : mutations) {
			if (m.leafSet.countSetBits() > m.leafSet.getTotalLeafCount() / 2) {
				m.leafSet.invert();
			}
		}
	}

	private void unPhaseFilter() {
		for (InfinteMutation m : mutations) {
			FixedBitSet leafSet = m.leafSet;
			for (int i = 0; i < leafSet.getTotalLeafCount(); i += 2) {
				if (leafSet.contains(i) == leafSet.contains(i + 1)) {
					continue;
				}
				// else we swap.. with p=.5
				if (.5 < random.nextFloat()) {
					boolean a = leafSet.contains(i);
					leafSet.set(i, !a);
				}
			}
		}
	}

	public void clear() {
		mutations.clear();
		segmentData.clear();
		pieces.clear();
		selectedLeafSets.clear();
		selectedLeafAlleles.clear();
		finished = false;
		sorted = false;
	}

	public boolean isAddSelectedAlleleMutations() {
		return addSelectedAlleleMutations;
	}

	public void setAddSelectedAlleleMutations(boolean addSelectedAlleleMutations) {
		this.addSelectedAlleleMutations = addSelectedAlleleMutations;
	}

	public int getTotalMutationCount() {
		assert finished;
		return mutations.size();
	}

	/**
	 * note that this is only at the selected site. this is *not* the same as
	 * origin count.
	 * 
	 * @return
	 */
	public int getSelectedAlleleMutationCount() {
		assert finished;
		return selectedLeafSets.size();
	}

	public boolean isTrackTrees() {
		return trackTrees;
	}

	public List<InfinteMutation> getMutationsUnsorted() {
		assert finished;
		return Collections.unmodifiableList(mutations);
	}

	public List<InfinteMutation> getMutationsSorted() {
		assert finished:"Note that this assertion failure is normal when the -NC option is used. Since its sort of broken, but results should be correct";
		if (!sorted)
			mutations.sort();
		return Collections.unmodifiableList(mutations);
	}

	/**
	 * ms compatable output of the trees.
	 * 
	 * Leaving here, despite really being in the wrong place. I don't want to
	 * couple stats with treeStrings/AssciatedSegmentData at this stage.
	 * 
	 * @param builder
	 */
	public void treeStrings(StringBuilder builder) {
		assert finished;
		if (!trackTrees)
			return;

		for (Map.Entry<Segment, AssociatedSegmentData> entry : segmentData.entrySet()) {
			Segment segment = entry.getKey();
			SegmentTracker data = entry.getValue();
			// first get the partion.
			double part = ((segment.getEnd() - segment.getStart()));
			if (recombinationCuts > 0) {
				long ipart = (long) Math.rint(part * recombinationCuts);
				builder.append('[').append(ipart).append(']');
			} else if (modelHistory.getRecombinationRate() == 0) {
				builder.append('[').append(Util.defaultFormat.format(part)).append(']');
			}
			builder.append(data.getTreeString()).append(';').append('\n');

		}
	}

	public int getTotalLeafCount() {
		return totalLeafCount;
	}

	// public double getGenerationScale(){
	// return generationScale;
	// }

	public double[] getTreeLengthDataAt(double p) {
		double height = 0;
		double length = 0;

		for (Segment s : segmentData.keySet()) {
			if (s.start <= p && s.end > p) {
				SegmentTracker asd = segmentData.get(s);
				height = asd.getSegmentHeight() * generationScale;
			}
			SegmentTracker asd = segmentData.get(s);
			length += asd.getSegmentTreeLength() * (s.end - s.start);
		}

		return new double[] { height, length * generationScale };
	}

	private class TreePiece implements PartialSumTreeElement {
		int index = -1;
		final FixedBitSet leafSet;
		final double start, stop;
		final double weight;

		// final int count;

		public TreePiece(FixedBitSet set, double start, double stop, double w) {
			this.leafSet = set;
			this.start = start;
			this.stop = stop;
			this.weight = w;
			// this.count=SegmentEventRecoder.this.treePieceCount++;//evil
			// syntax. assignment then increment.
		}

		@Override
		public int getPartialSumTreeIndex() {

			return index;
		}

		@Override
		public void setPartialSumTreeIndex(int partialSumTreeIndex) {
			index = partialSumTreeIndex;
		}

		// @Override
		// public int compareTo(TreePiece o) {
		//
		// return count-o.count;
		// }
	}

}
