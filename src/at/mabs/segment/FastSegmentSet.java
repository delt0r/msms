package at.mabs.segment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;

import at.mabs.coalescent.LineageData;

import at.mabs.util.random.RandomGenerator;

/**
 * finding the other skip list cumbersome and confusion. So in desperation, i will rewite.
 * 
 * Hopefully it will go so well i will wonder why i didn't do this sooner.
 * 
 * So a skip list as before. However we only have 2 basic operations, add and split. We use fingers
 * to have fast but simple merges. Add will of course delete coaleset lines. It will also split
 * incomplete overlapping segments.
 * 
 * by doing this, we achive the same to better complexity with simpler code. We hope.
 * 
 * @author bob
 * 
 */
public class FastSegmentSet extends LineageData<FastSegmentSet> {
	private static final double GEOMETRIC_CONSTANT =1.0 / Math.log(.5);// .33//.25 is faster still

	private final Random random =RandomGenerator.getRandom();
	private final SegmentSetFactory parent;

	private Entry head;
	private Entry tail;
	// private Entry[] pointers;
	private Entry[] finger;// last place where we found something.

	// private int topLevel;

	private FastSegmentSet(Entry head, Entry tail, SegmentSetFactory parent) {
		this.head =head;
		this.tail =tail;
		finger =new Entry[head.skips.length];
		Arrays.fill(finger, head);
		assert head.skips.length > 0;
		assert (head.skips[0] == null) == (tail == null) : head.skips[0] + "\t" + tail;// tail null
																						// iff empty
		// pointers =new Entry[head.skips.length];
		this.parent =parent;
		// topLevel =findTopLevel();

	}

	/*
	 * Add itself to the one that need to be tracked. Perhaps keeping this list in linageState makes
	 * more sense now
	 */

	FastSegmentSet(SegmentSetFactory parent) {
		head =new Entry(new Entry[1], null, null);
		finger =new Entry[] { head };
		// pointers =new Entry[1];
		this.parent =parent;

	}

	private Entry createEntry(Segment seg, SegmentTracker tracker) {
		int nlevel =generateLevel();
		Entry entry =new Entry(new Entry[nlevel], seg, tracker);
		return entry;
	}

	private void add(Segment seg, SegmentTracker tracker) {
		// Arrays.fill(finger,head);
		add(createEntry(seg, tracker), Double.NaN);
	}

	/*
	 * contract is that all entry.skips are rewriten too.
	 * 
	 * @param entry
	 */
	private void add(Entry entry, double currentTime) {
		// entry should not belong to this set
		assert entry.element != null;
		// ensure top level

		// checkHeadLevel(entry.skips.length);
		findWithFinger(entry.element);
		// now finger points to our insertion place.. or a cut place.
		// more to the point, in contatins the pointer instances.

		// first is it a simple insertion?
		if (finger[0].skips[0] == null || finger[0].skips[0].element.compareTo(entry.element) > 0) {
			// simple insertion.
			insertAtFinger(entry);
			return;
		}
		// are we ==?
		if (finger[0].skips[0].element.equals(entry.element)) {
			insertEqual(entry, currentTime);
			return;
		}

		// now we need to cut at least one segment. could be more of course
		// we do this as a cut and add operation. that is we cut insert. Then call add again.
		// ie recurse --this could add too much to the stack however --lets find out ;)

		Entry contained =finger[0].skips[0];// cant be null

		// the case where the inserted segment starts before any other segment
		if (entry.element.start < contained.element.start) {
			assert contained.element.start < entry.element.end;
			Segment[] split =entry.element.cut(contained.element.start);
			Entry newe =createEntry(split[1], contained.data.split());
			insertAtFinger(entry);
			add(newe, currentTime);
			return;

		}
		// so case one. the segment in this set has a start before the new segment. we need to cut
		// it then add it as a new one.
		if (contained.element.start < entry.element.start) {
			assert contained.element.end > entry.element.start; // we really cut this segment
			Segment[] split =contained.element.cut(entry.element.start);
			contained.element =split[1];
			Entry newe =createEntry(split[0], contained.data.split());
			insertAtFinger(newe);// so now we just continue adding things.
			add(entry, currentTime);
			return;
		}

		// next case is that the current ends after then end of the new item.
		if (contained.element.end > entry.element.end) {
			assert contained.element.start == entry.element.start;
			Segment[] split =contained.element.cut(entry.element.end);
			contained.element =split[0];// just for readablity --noop
			Entry newe =createEntry(split[1], contained.data.split());
			insertEqual(entry, currentTime);// since now it is equal. note could result in a delete.
			// finally we add the new piece.
			add(newe, currentTime);
			return;
		}
		// final case. our entry needs to be cut
		if (contained.element.end < entry.element.end) {
			assert contained.element.start == entry.element.start;
			Segment[] split =entry.element.cut(contained.element.end);
			Entry newe =createEntry(split[1], contained.data.split());
			insertEqual(entry, currentTime);
			// finally we add the new piece.
			add(newe, currentTime);
			return;
		}
		assert false;
	}

	private void insertEqual(Entry entry, double currentTime) {
		// decrement the count.
		assert entry.element.equals(finger[0].skips[0].element);
		Entry e =finger[0].skips[0];
		e.data.join(entry.data, entry.element, currentTime);
		if (e.element.decrementLineageCount(entry.element) <= 1) {
			e.data.finishedWithSegment(e.element);
			deleteAtFinger();
		}
	}

	private void insertAtFinger(Entry entry) {
		assert entry != null;
		checkHeadLevel(entry.skips.length);
		int startLevel =entry.skips.length - 1;
		assert entry.skips.length <= finger.length : finger.length + "\t" + entry.skips.length;
		for (int level =startLevel; level >= 0; level--) {
			// System.out.println(entry.skips[level]+"\t"+finger[level]);
			entry.skips[level] =finger[level].skips[level];
			finger[level].skips[level] =entry;
		}
		if (entry.skips[0] == null)
			tail =entry;
	}

	private void deleteAtFinger() {
		Entry e =finger[0].skips[0];
		for (int level =0; level < e.skips.length; level++) {
			finger[level].skips[level] =e.skips[level];
		}
		// we could reduce the height of the skip list.
		//int high =e.skips.length - 1;
		if (e.skips.length == head.skips.length) {
			makeHeadLevelLow();
		}
		if (e == tail) {
			tail =finger[0];
		}
	}

	/*
	 * moves the finger to the location of segemnt, or where segment would be added if no in this
	 * list, or the item that spans this segement.
	 */
	private void findWithFinger(Segment segment) {
		// first do we need to "reset"
		if (finger[0].skips[0] == null || finger[0].skips[0].element.compareTo(segment) >= 0) {
			finger[finger.length - 1] =head;
		}
		// now we just move along till we are where we need to be
		for (int level =finger.length - 1; level >= 0; level--) {
			while (finger[level].skips[level] != null && finger[level].skips[level].element.compareTo(segment) < 0) {
				finger[level] =finger[level].skips[level];// skip forward
			}
			if (level != 0)
				finger[level - 1] =finger[level];
		}
		// System.out.println("FindFinger:"+Arrays.toString(finger));
	}

	/*
	 * uses end postions
	 */
	private void findWithFinger(double p) {
		// first do we need to "reset"
		if (finger[0].skips[0] == null || finger[0].skips[0].element.end >= p) {
			finger[finger.length - 1] =head;
		}
		// now we just move along till we are where we need to be
		for (int level =finger.length - 1; level >= 0; level--) {
			while (finger[level].skips[level] != null && finger[level].skips[level].element.end <= p) {
				finger[level] =finger[level].skips[level];// skip forward
			}
			if (level != 0)
				finger[level - 1] =finger[level];
		}
		// System.out.println("FindFinger:"+Arrays.toString(finger));
	}

	/**
	 * move one more place.
	 */
	private void incrementFinger() {
		Entry next =finger[0].skips[0];
		if (next == null)
			return;
		for (int i =0; i < next.skips.length; i++) {
			// if (next.skips[i] != null)
			finger[i] =next;// .skips[i];
		}
	}

	private int findTopLevel() {
		int c =-1;
		for (int i =0; i < head.skips.length; i++) {
			if (head.skips[i] != null)
				c++;
		}
		return c;
	}

	private void checkHeadLevel(int level) {
		if (head.skips.length >= level) {
			return;
		}

		Entry[] entry =new Entry[level];
		System.arraycopy(head.skips, 0, entry, 0, head.skips.length);
		//Entry oldHead =head;
		head.skips =entry;
		Entry[] nfig =new Entry[level];
		System.arraycopy(finger, 0, nfig, 0, finger.length);
		Arrays.fill(nfig, finger.length, nfig.length, head);
		finger =nfig;
		assert finger.length >= level;
		// System.out.println("Leveling:"+head);
		// System.out.println("Finger:"+Arrays.toString(finger));
		// Arrays.fill(nfig,head);

	}

	/**
	 * reduece the head level to the lowest possible
	 */
	private void makeHeadLevelLow() {
		//if(true)return;
		int high =head.skips.length - 1;
		int reduction =0;
		for (int i =high; i > 0; i--) {
			if (head.skips[i] == null) {
				reduction++;
			} else {
				break;
			}
		}
		int nLevel =high + 1 - reduction;
		Entry[] nSkips =new Entry[nLevel];
		System.arraycopy(head.skips, 0, nSkips, 0, nSkips.length);
		head.skips =nSkips;
		// now same thing for finger.
		Entry[] nFig =new Entry[nLevel];
		System.arraycopy(finger, 0, nFig, 0, nFig.length);
		finger =nFig;

	}

	@Override
	public void join(FastSegmentSet set, double currentTime) {
		// joins are now simple. We iterate and add.
		// TODO add specal cases. aka non overlapping intervals, empty sets
		assert set != null;
		Entry pointer =set.head.skips[0];

		while (pointer != null) {
			// System.out.println("Joiner:"+pointer);
			Entry next =pointer.skips[0];
			add(pointer, currentTime);
			pointer =next;
		}

	}

	@Override
	public FastSegmentSet split(double splitPoint, Iterable<FastSegmentSet> setsToUpdate) {
		findWithFinger(splitPoint);
		// start with the empty set case
		if (finger[0].skips[0] == null) {
			return new FastSegmentSet(this.parent);
		}
		if (finger[0] == head && finger[0].skips[0].element.start >= splitPoint) {
			FastSegmentSet tailSet =new FastSegmentSet(head, tail, parent);
			clear();
			return tailSet;
		}

		// so now finger could be pointing at something we need to split.
		Entry next =finger[0].skips[0];
		Entry extra =null;
		// System.out.println("Cut:" + next.element);
		if (next.element.start < splitPoint && next.element.end > splitPoint) {
			// System.out.println("We Cut");
			Segment[] splits =next.element.cut(splitPoint);
			// the low part is still the element instance
			extra =createEntry(splits[1], next.data.split());
			// need to increment the finger by one
			incrementFinger();
		}
		// now we cut at the finger.
		FastSegmentSet tailSet =cutTailSetWithFinger();
		if (extra != null)
			tailSet.add(extra, Double.NaN);
		assert head.skips.length > 0;
		assert !tailSet.isEmpty() && !isEmpty() : this + "\t" + tailSet + "\t" + splitPoint;
		return tailSet;
	}

	/**
	 * we assume that finger is in the right place and chop.
	 * 
	 * @return
	 */
	private FastSegmentSet cutTailSetWithFinger() {
		// lets assert we have a tail.
		assert isEmpty() == (tail == null);

		Entry[] headSkips =new Entry[finger.length];
		Entry newTail =finger[0];
		for (int i =0; i < finger.length; i++) {
			headSkips[i] =finger[i].skips[i];
			finger[i].skips[i] =null;
		}
		if (headSkips[0] == null)
			tail =null;
		FastSegmentSet tailSet =new FastSegmentSet(new Entry(headSkips, null, null), tail, parent);
		if (newTail == head) {
			tail =null;
		} else {
			tail =newTail;
		}
		assert isEmpty() == (tail == null) : isEmpty() + "\t" + tail;
		Arrays.fill(finger, head);// reset finger.
		return tailSet;
	}

	public void clear() {
		head =new Entry(new Entry[1], null, null);
		finger =new Entry[] { head };
		tail =null;
	}

	/**
	 * calls makeSele... on the correct SegmentData object
	 */
	@Override
	public void markSelectedAllele(int allele, double p) {
		findWithFinger(p);
		Entry next =finger[0].skips[0];
		if (next == null)
			return;
		SegmentTracker data =next.data;
		Segment seg =next.element;
		if (seg.start <= p && seg.end > p) {
			data.markSelectedAllele(allele, p);
		}
	}

	@Override
	public double getEnd() {
		if (tail == null) {
			assert false : isEmpty();
			return Double.NaN;
		}
		return tail.element.end;
	}

	public double getStart() {
		if (head.skips[0] == null) {
			assert false : isEmpty();
			return Double.NaN;
		}
		return head.skips[0].element.start;
	};

	@Override
	public boolean isEmpty() {

		return head.skips[0] == null;
	}

	private final int generateLevel() {
		// return (int) (GEOMETRIC_CONSTANT * Math.log(random.nextDouble()));
		return Integer.numberOfTrailingZeros(random.nextInt()) / 2 + 1;
	}

	// struc
	private static class Entry {
		Entry[] skips;
		Segment element;
		SegmentTracker data;

		public Entry(Entry[] skips, Segment e, SegmentTracker data) {
			this.skips =skips;
			element =e;
			this.data =data;
		}

		@Override
		public String toString() {

			return "E(" + element + ")";
		}
	}

	public double density() {
		double l =getEnd() - getStart();
		double cover =0;
		Entry e =head.skips[0];
		while (e != null) {
			cover +=e.element.end - e.element.start;
			e =e.skips[0];
		}
		return cover / l;
	}
	
	public int count() {
		
		int c=0;
		Entry e =head.skips[0];
		while (e != null) {
			c++;
			e =e.skips[0];
		}
		return c;
	}

	@Override
	public String toString() {
		String s ="skip[";
		assert head.skips.length > 0;
		Entry e =head.skips[0];
		while (e != null) {
			s +=e.element + "|" + e.skips.length + ",";
			e =e.skips[0];
		}
		return s + "]" + head.skips.length;
	}

	void addAll(Collection<Segment> all, SegmentTracker data) {
		for (Segment seg : all)
			add(seg, data.split());
		// System.out.println("Added:"+this);
	}

	public static void main(String[] args) {
		// FastSegmentSet fss =new FastSegmentSet(null);
		// Segment s1 =new Segment(0, 1);
		// Segment s2 =new Segment(1, 2);
		// Segment s3 =new Segment(2, 3);
		// Segment s4 =new Segment(3, 4);
		//
		// Segment s5 =new Segment(-1.5, 7.5);
		//
		// fss.add(s4, new SegmentTracker.Mock());
		// System.out.println(fss+"\t"+fss.getStart()+"=="+fss.getEnd());
		// fss.add(s1, new SegmentTracker.Mock());
		// System.out.println(fss+"\t"+fss.getStart()+"=="+fss.getEnd());
		// fss.add(s2, new SegmentTracker.Mock());
		// System.out.println(fss+"\t"+fss.getStart()+"=="+fss.getEnd());
		// fss.add(s3, new SegmentTracker.Mock());
		// System.out.println(fss+"\t"+fss.getStart()+"=="+fss.getEnd());
		// //fss.add(s3, new SegmentTracker.Mock());
		// //System.out.println(fss+"\t"+fss.getStart()+"=="+fss.getEnd()+"\n");
		// FastSegmentSet fss2 =new FastSegmentSet(null);
		// fss2.add(s5, new SegmentTracker.Mock());
		// fss2.join(fss, 1);
		// System.out.println(fss2+"\t"+fss.getStart()+"=="+fss.getEnd());
		//
		// FastSegmentSet cut =fss.split(1.1, null);
		//
		// System.out.println(fss + "\t" + cut + "\t" + cut.isEmpty() + "\n");
		//
		// fss.join(cut, 1);
		// System.out.println(fss+"\t"+fss.getStart()+"=="+fss.getEnd());

		Random rand =new Random();

		FastSegmentSet fssr =new FastSegmentSet(null);
		fssr.add(new Segment(0, 1), new SegmentTracker.Mock());
		FastSegmentSet[] cuts =new FastSegmentSet[128];
		cuts[0] =fssr;
		for (int rep =0; rep < 4; rep++) {
			for (int i =1; i < cuts.length; i++) {
				FastSegmentSet set =cuts[rand.nextInt(i)];
				double s =set.getStart();
				double e =set.getEnd();
				double p =rand.nextDouble() * (e - s) + s;
				FastSegmentSet high =set.split(p, null);
				cuts[i] =high;
				System.out.println(set + "\t" + high);
			}
			System.out.println("\nMERGE\n");
			for (int i =0; i < cuts.length; i +=2) {
				cuts[i].join(cuts[i + 1], i);
				System.out.println(cuts[i] + "::" + cuts[i].density());
			}
			System.out.println("\nMERGE2\n");
			for (int i =0; i < cuts.length; i +=4) {
				cuts[i].join(cuts[i + 2], i);
				System.out.println(cuts[i] + "::" + cuts[i].density());
			}
			System.out.println("\nMERGE4\n");
			for (int i =0; i < cuts.length; i +=8) {
				cuts[i].join(cuts[i + 4], i);
				System.out.println(cuts[i] + "::" + cuts[i].density());
			}

			System.out.println("\nMERGE8\n");
			for (int i =0; i < cuts.length; i +=16) {
				cuts[i].join(cuts[i + 8], i);
				System.out.println(cuts[i] + "::" + cuts[i].density());
			}

			System.out.println("\nMERGE16\n");
			for (int i =0; i < cuts.length; i +=32) {
				cuts[i].join(cuts[i + 16], i);
				System.out.println(cuts[i] + "::" + cuts[i].density());
			}

			System.out.println("\nMERGE32\n");
			for (int i =0; i < cuts.length; i +=64) {
				cuts[i].join(cuts[i + 32], i);
				System.out.println(cuts[i] + "::" + cuts[i].density());
			}

			System.out.println("\nMERGE64\n");
			for (int i =0; i < cuts.length; i +=128) {
				cuts[i].join(cuts[i + 64], i);
				System.out.println(cuts[i] + "::" + cuts[i].density());
			}
			FastSegmentSet hold=cuts[0];
			Arrays.fill(cuts, null);
			cuts[0]=hold;
		}
		System.out.println(cuts[0].density()+"\t"+cuts[0].count());

	}
	@Override
	public boolean contains(double p) {
		throw new RuntimeException("This thing is broken anyways");
	}
}
