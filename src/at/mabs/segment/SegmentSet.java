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

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import at.mabs.coalescent.LineageData;

import at.mabs.util.random.RandomGenerator;


/**
 * lightweight, fast, and very mutable. 
 * 
 * A bit kludgy now. And very not lightweight
 * 
 * this tracks segments and cuts them up as needed. It also "fires" events on segment trackers. 
 * 
 * everything i try to replace this with either does not work, or is slower. 
 * 
 * Go figure. 
 * 
 * @author bob
 * 
 */
public class SegmentSet extends LineageData<SegmentSet>  {
	private final SegmentSetFactory parent;
	private final Random random =RandomGenerator.getRandom();
	
	private final int hashCode1=random.nextInt();
	//public final int hashCode2=random.nextInt();
	//public final int hashCode3=random.nextInt();
	
	private static final double GEOMETRIC_CONSTANT =1.0 / Math.log(.5);// .33//.25 is faster still
	private Entry head;
	private Entry tail;
	private Entry[] pointers;
	private int topLevel;

	private final double allelePosition;
	private SegmentTracker dataAtSelectedLocus;//bloody hacky btw.
	
	// private int size=0;

	private SegmentSet(Entry head, Entry tail, SegmentSetFactory parent) {
		this.head =head;
		this.tail =tail;
		assert (head.skips[0] == null) == (tail == null);// tail null iff empty
		// this.size=size;
		pointers =new Entry[head.skips.length];
		this.parent =parent;
		//parent.addSegmentSet(this);
		topLevel =findTopLevel();
		allelePosition=parent.getAllelePosition();
		
	}

	/*
	 * Add itself to the one that need to be tracked. Perhaps keeping this list in linageState makes
	 * more sense now
	 */
	SegmentSet(SegmentSetFactory parent) {
		head =new Entry(new Entry[1], null, null);
		pointers =new Entry[1];
		this.parent =parent;
		//parent.addSegmentSet(this);
		allelePosition=parent.getAllelePosition();
	}

	private void add(Segment e, SegmentTracker data) {
		assert e != null && data != null;
		add(e, data, false);
	}
	
	private final int generateLevel(){
		//return (int) (GEOMETRIC_CONSTANT * Math.log(random.nextDouble()));
		return Integer.numberOfTrailingZeros(random.nextInt())/2;
	}

	
	
	private void add(Segment e, SegmentTracker data, boolean decrement) {
		assert e != null;
		// first a new Entry...
		// double d=GEOMETRIC_CONSTANT*Math.log(random.nextDouble());
		int level =generateLevel();
		topLevel =Math.max(topLevel, level);
		// System.out.println("L:"+level);
		Entry[] skipPointers =new Entry[level + 1];
		Entry newEntry =new Entry(skipPointers, e, data);
		// check that head is at least that big.
		checkHeadLevel(level);
		// now find where e belongs.
		int searchLevel =topLevel;
		Entry searchNode =head;
		while (searchLevel >= 0) {
			if (searchNode.skips[searchLevel] != null && searchNode.skips[searchLevel].element.compareTo(e) < 0) {
				searchNode =searchNode.skips[searchLevel];
				continue;
			}
			pointers[searchLevel] =searchNode;
			if (level >= searchLevel) {
				newEntry.skips[searchLevel] =searchNode.skips[searchLevel];
				searchNode.skips[searchLevel] =newEntry;
			}
			searchLevel--;
		}
		// duplicate... remove the old.
		if (newEntry.skips[0] != null && newEntry.skips[0].element.equals(e)) {
			if (decrement)
				newEntry.skips[0].element.decrementLineageCount();
			// now we reset all the pointers...
			for (int l =0; l <= level; l++) {
				assert pointers[l].skips[l] == newEntry;
				pointers[l].skips[l] =newEntry.skips[l];
			}
			return;
		}
		// update tail
		if (newEntry.skips[0] == null)
			tail =newEntry;
		// size++;
	}

	private boolean contains(Segment e) {
		int searchLevel =head.skips.length - 1;
		Entry searchNode =head;
		while (searchLevel >= 0) {
			if (searchNode.skips[searchLevel] != null && searchNode.skips[searchLevel].element.compareTo(e) < 0) {
				searchNode =searchNode.skips[searchLevel];
				continue;
			}
			searchLevel--;
		}
		return searchNode.skips[0] != null && searchNode.skips[0].element.equals(e);
		// return false;
	}
	
	/**
	 * if the position is valid loci for the seg set then mark the allele
	 */
	//FIXME this is O(n) when it could be O(ln n)
	@Override
	public void markSelectedAllele(int allele,double p) {
		//System.out.println("MarkSelected:"+this+"\t"+p+"\t"+dataAtSelectedLocus);
		if(this.dataAtSelectedLocus!=null)
			dataAtSelectedLocus.markSelectedAllele(allele, p);
		if(p<getStart() || p>getEnd()){
			return;
		}
		Entry pointer=head.skips[0];
		while(pointer!=null){
			//System.out.println("PointerNOW:"+pointer.element);
			if(pointer.element.start<=p && pointer.element.end>p){
				//System.out.println("Call");
				pointer.data.markSelectedAllele(allele, p);
				return;
			}
			pointer=pointer.skips[0];
		}
		
	}
	
	@Override
	public boolean contains(double p) {
		if(p<getStart() || p>getEnd()){
			return false;
		}
		Entry pointer=head.skips[0];
		while(pointer!=null){
			//System.out.println("PointerNOW:"+pointer.element);
			if(pointer.element.start<=p && pointer.element.end>p){
				//System.out.println("Call");
				
				return true;
			}
			pointer=pointer.skips[0];
		}
		return false;
	}

	private boolean replaceIfFound(Segment current, Segment newSeg1, Segment newSeg2) {
		assert current != null;
		assert newSeg1.start < newSeg2.start;
		assert current.start == newSeg1.start;
		if (head.skips[0] == null)
			return false;
		if (head.skips[0].element.start > current.start || tail.element.start < current.start)
			return false;
	
		
		// now find where e belongs.
		int searchLevel =topLevel;
		Entry searchNode =head;
		int steps =0;
		int skips =0;
		while (searchLevel >= 0) {
			steps++;
			if (searchNode.skips[searchLevel] != null && searchNode.skips[searchLevel].element.start < current.start) {
				searchNode =searchNode.skips[searchLevel];
				skips++;
				continue;
			}
			pointers[searchLevel] =searchNode;
			searchLevel--;
		}
		if(pointers[0].skips[0]==null || !pointers[0].skips[0].element.equals(current)){
			//System.out.println("NotFound "+size()+" "+contains(current));
			return false;
		}
		//System.out.println("Found "+size());
		//now insert the new element...
		assert pointers[0].skips[0].element.start==current.start;
		int level =Math.min(generateLevel(),topLevel);
		
		Entry[] skipPointers =new Entry[level + 1];
		Entry newEntry =new Entry(skipPointers, newSeg1, null);
		// check that head is at least that big.
		
		
		for(int l=0;l<=level;l++){
			newEntry.skips[l]=pointers[l].skips[l];
			pointers[l].skips[l]=newEntry;
		}
		newEntry.skips[0].element =newSeg2;
		newEntry.data =newEntry.skips[0].data.split();
		topLevel =Math.max(topLevel, level);
		 
		return true;

	}

	private int size() {
		int i =-1;
		Entry e =head;
		while (e != null) {
			e =e.skips[0];
			i++;
		}
		return i;
	}

	@Override
	public double getStart() {
		if(head.skips[0]==null)
			return Double.NaN;
		return head.skips[0].element.start;
	}
	
	@Override
	public double getEnd() {
		if(head.skips[0]==null)
			return Double.NaN;
		return tail.element.end;
	}
	
	public void clear() {
		Arrays.fill(head.skips, null);
		Arrays.fill(pointers, null);
		topLevel =0;
		tail =null;
	}

	private void newEntryClear() {
		head =new Entry(new Entry[3], null, null);
		tail =null;
		topLevel =0;
		pointers =new Entry[3];
	}

	/* (non-Javadoc)
	 * @see at.mabs.segment.LineageData#isEmpty()
	 */
	public boolean isEmpty() {
		return head.skips[0] == null;
	}

	/* (non-Javadoc)
	 * @see at.mabs.segment.LineageData#split(double, java.util.Collection)
	 */
	@Override
	public SegmentSet split(double splitPoint, Iterable<SegmentSet> setsToUpdate) {
		
		//long time=System.nanoTime();
		if (head.skips[0] == null) {
			// return new SkipListSegmentSet(parent);
			assert false;
			throw new IndexOutOfBoundsException("SkipList Empty");
		}
		//System.out.println("c@s:"+head.skips[0].data.getLeafSet());
		// check the past ends case.
		if (head.skips[0].element.start >= splitPoint) {
			SegmentSet newSet =new SegmentSet(head, tail, parent);
			newEntryClear();
		//	splitTime+=(System.nanoTime()-time)*1e-6;
			return newSet;
		}
		// assert tail!=null;
		// System.out.println("Tail:"+tail.element);
		if (tail.element.end <= splitPoint) {
		//	splitTime+=(System.nanoTime()-time)*1e-6;
			return new SegmentSet(parent);
		}
		
		// start with a new head
		Entry newHead =new Entry(new Entry[head.skips.length], null, null);

		Entry newTail =null;
		int searchLevel =topLevel;// head.skips.length - 1;
		Entry searchNode =head;

		while (searchLevel >= 0) {

			if (searchNode.skips[searchLevel] != null && searchNode.skips[searchLevel].element.start < splitPoint) {
				searchNode =searchNode.skips[searchLevel];

				continue;
			}
			newHead.skips[searchLevel] =searchNode.skips[searchLevel];
			searchNode.skips[searchLevel] =null;
			newTail =searchNode;
			searchLevel--;
		}

		// System.out.println("NewTail:" + newTail + "\tCurrentTail:" + tail);
		// System.out.println("NewTail:" + newTail.element + "\tCurrentTail:" +
		// tail.element+"\t"+newHead.skips[0]);
		// now newHead points at the right place. But don't forget the case with the new set being
		// empty
		SegmentSet newSet =null;//
		if (newHead.skips[0] == null) {
			newSet =new SegmentSet(newHead, null, parent);
		} else {
			newSet =new SegmentSet(newHead, tail, parent);
		}
		tail =newTail;
		
		//System.out.println("Split?");
		// now check for a cut segment..
		if (tail != null && tail.element.end > splitPoint) {
			//System.out.println("Yep");
			Segment old=tail.element;
			Segment[] newSegs =old.cut(splitPoint);
			tail.element =newSegs[0];
			newSet.add(newSegs[1], tail.data.split());
			int i=0;
			int j=0;
			for (SegmentSet update : setsToUpdate) {
				if(update!=this){
					boolean rep=update.replaceIfFound(old, newSegs[0], newSegs[1]);
					//j+=rep?1:0;
				}
				//i++;
			}
			//System.out.println("CountsFound:"+i+"\t"+j);
			
		}
		newSet.topLevel =newSet.findTopLevel();
		topLevel =findTopLevel();
		//splitTime+=(System.nanoTime()-time)*1e-6;
		//System.out.println("\nTime:"+t);
		return newSet;

	}
	//public static double splitTime=0;
	//public static double mergeTime=0;

	/* (non-Javadoc)
	 * @see at.mabs.segment.LineageData#mergeWithRemoval(at.mabs.segment.SegmentSet, double)
	 */
	public void join(SegmentSet set, double currentTime) {
		// so we do nothing if set is empty
		//long time=System.nanoTime();
		if (set.head.skips[0] == null)
			return;
		if (head.skips[0] == null) {
			head =set.head;
			tail =set.tail;
			topLevel =set.topLevel;
			pointers =set.pointers;
			set.newEntryClear();
		//	mergeTime+=(System.nanoTime()-time)*1e-6;
			return;
		}

		// now the standard max level thing
		topLevel =Math.max(topLevel, set.topLevel);// findTopLevel(), set.findTopLevel());
		// System.out.println("Top:"+topLevel+"\t"+Math.max(findTopLevel(), set.findTopLevel()));
		checkHeadLevel(topLevel);

		// now we ensure the pointers are empty..for now, should be able to leave out.
		Arrays.fill(pointers, null);

		int searchLevel =topLevel;
		Entry currentEntry =head;
		Entry newEntry =set.head.skips[0];
		// double newEntryStart =newEntry.element.start;
		Entry nextEntry =newEntry.skips[0];
		// System.out.println("Sizes:"+this.size()+" "+set.size());

		while (newEntry != null) {
			assert newEntry.skips.length - 1 <= topLevel;
			// two phases --find where we can insert. Then keep inserting till we are done.
			while (searchLevel >= 0) {

				while (currentEntry.skips[searchLevel] != null && currentEntry.skips[searchLevel].element.start < newEntry.element.start) {
					currentEntry =currentEntry.skips[searchLevel];

				}
				pointers[searchLevel] =currentEntry;
				searchLevel--;
			}

			// now we go through the next ones till we run out or they are bigger than the next
			// local entry
			Entry nextLocal =pointers[0].skips[0];

			// System.out.println(nextLocal+"\t"+head);
			while (newEntry != null && (nextLocal == null || nextLocal.element.start >= newEntry.element.start)) {
				// so now we add or decrement our current entry. then delete etc.
				// deal with == first.

				//FIXME add a split overlapping segment thing here. 
				
				if (nextLocal != null && nextLocal.element.equals(newEntry.element)) {
					assert nextLocal.skips.length - 1 <= topLevel;
					int dec =nextLocal.element.decrementLineageCount();
					nextLocal.data.join(newEntry.data, newEntry.element, currentTime);
					if (dec <= 1) {
						// System.out.println(nextLocal.data.getTreeString());
						//need to consider the case where the selected allele is contained here...
						if(nextLocal.element.start<=allelePosition && nextLocal.element.end>allelePosition){
							this.dataAtSelectedLocus=nextLocal.data;
						}
						nextLocal.data.finishedWithSegment(nextLocal.element);
						deleteWithPointerCopy(pointers, nextLocal);
						nextLocal =nextLocal.skips[0];
						assert pointers[0].skips[0] == nextLocal;
						
						
					}

				} else {
					// now we add the dam thing
					insertWithPointerCopy(pointers, newEntry);
					assert newEntry.skips[0] == nextLocal;
				}

				newEntry =nextEntry;
				if (newEntry != null)
					nextEntry =newEntry.skips[0];
			}
			// back to a proper search
			searchLevel =topLevel;
			currentEntry =pointers[searchLevel];
		}
		set.clear();
		//mergeTime+=(System.nanoTime()-time)*1e-6;
		//parent.removeSegmentSet(set);
		// System.out.println("Steps:"+steps+"\tskips"+skips+"\ttop:"+findTopLevel()+"\tHeadLenght"+head.skips.length);
	}

	private void deleteWithPointerCopy(Entry[] pointers, Entry e) {
		for (int i =0; i < e.skips.length; i++) {
			assert pointers[i].skips[i] == e;
			pointers[i].skips[i] =e.skips[i];
		}
		if (e.skips[0] == null)
			tail =pointers[0];
	}

	/**
	 * insert *and* move the pointer forward.
	 * 
	 * @param pointers
	 * @param e
	 */
	private void insertWithPointerCopy(Entry[] pointers, Entry e) {
		for (int i =0; i < e.skips.length; i++) {
			e.skips[i] =pointers[i].skips[i];
			pointers[i].skips[i] =e;
			pointers[i] =e;// move pointers forward
		}
		if (e.skips[0] == null)
			tail =e;
	}

	private int findTopLevel() {
		int c =-1;
		for (int i =0; i < head.skips.length; i++) {
			if (head.skips[i] != null)
				c++;
		}
		// if(c==1)
		// System.out.println(this);
		return c;
	}

	/* (non-Javadoc)
	 * @see at.mabs.segment.LineageData#getSpan()
	 */
//	public double getRecombinationWeight() {
//		// assert (head.skips[0] == null) == (tail == null) : "Head:" + head.skips[0] + "->" + tail;
//		if (head.skips[0] == null)
//			return 0;
//		return tail.element.end - head.skips[0].element.start;
//	}

	/**
	 * recombsegments is how much a unit interval is cut.
	 * 
	 * @param recombSegments
	 * @return
	 */
//	public double generateRandomInSpan(ModelHistroy model) {
//		if (head.skips[0] == null) {
//			assert false;
//			return Double.NaN;
//		}
//		double start =head.skips[0].element.start;
//		double end =tail.element.end;
//		double r =(end - start) * random.nextDouble() + start;
//		long recombSegments=model.getRecombinationCutSites();
//		if (recombSegments > 0)
//			r =(double) ((int) (r * recombSegments)) / recombSegments;
//		return r;
//	}

//	public double generateRandomInSelectionSpan(ModelHistroy model) {
//		if (head.skips[0] == null) {
//			assert false;
//			return Double.NaN;
//		}
//		double start =Math.min(head.skips[0].element.start, model.getAlleleLocation());
//		double end =Math.max(tail.element.end, model.getAlleleLocation());
//		double r =(end - start) * random.nextDouble() + start;
//		long recombSegments=model.getRecombinationCutSites();
//		if (recombSegments > 0)
//			r =(double) ((int) (r * recombSegments)) / recombSegments;
//		return r;
//	}

	/* (non-Javadoc)
	 * @see at.mabs.segment.LineageData#getSelectionSpan(at.mabs.model.ModelHistroy)
	 */
//	public double getRecombinationWeightSelection(ModelHistroy model) {
//		if (head.skips[0] == null) {
//			 assert false;
//			
//			return 0;//assuming a singe allele
//		}
//		return Math.max(model.getAlleleLocation(), tail.element.end) - Math.min(model.getAlleleLocation(), head.skips[0].element.start);
//	}

	private void checkHeadLevel(int level) {
		if (head.skips.length > level) {
			return;
		}
		Entry[] entry =new Entry[level + 1];
		System.arraycopy(head.skips, 0, entry, 0, head.skips.length);
		head =new Entry(entry, null, null);
		pointers =new Entry[level + 2];
	}

	// struc
	private static class Entry {
		final Entry[] skips;
		Segment element;
		SegmentTracker data;

		public Entry(Entry[] skips, Segment e, SegmentTracker data) {
			this.skips =skips;
			element =e;
			this.data =data;
		}
	}

	public String toString() {
		String s ="SkipList[";
		Entry e =head.skips[0];
		while (e != null) {
			s +=e.element + ",";
			e =e.skips[0];
		}
		return s.substring(0, s.length() - 1) + "]";
	}

	void addAll(Collection<Segment> all, SegmentTracker data) {
		for (Segment seg : all)
			add(seg, data.split());
		// System.out.println("Added:"+this);
	}
	
	@Override
	public final int hashCode() {
		return hashCode1;
	}
	
	

}
