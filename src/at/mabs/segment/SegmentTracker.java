package at.mabs.segment;
/**
 * We have this what looks like a class that should be in segment because there is a difference between
 * a segment and a segment in a lineage. This represents things that happen to a segment within a lineage.
 * 
 * Ie perhaps segment instances should be added to a tracker. However i don't do this because a segment set
 * manages the spliting of segment instances. Note that for any one simulation there is exactly *one* segment 
 * instance for a given interval representation. This we don't need to check which linages have a given 
 * segment at a merge. We just remove anything with a count of one. 
 * @author bob
 *
 * @param <T>
 */
public interface SegmentTracker<T extends SegmentTracker<?>> {

	public T split();

	/**
	 * 
	 * @param data
	 * @param seg
	 * @param time
	 */
	public void join(T data, Segment seg, double time);

	public void markSelectedAllele(int allele, double p);

	/**
	 * genrally this method should not have join or split called after this has been invoked. 
	 * @param seg
	 */
	public void finishedWithSegment(Segment seg);

	public StringBuilder getTreeString();

	public void setLeafSet(FixedBitSet leafSet);

	public FixedBitSet getLeafSet();

	public double getSegmentHeight();

	public double getSegmentTreeLength();
	
	public static class Mock implements SegmentTracker<Mock>{

		@Override
		public Mock split() {
			
			return new Mock();
		}

		@Override
		public void join(Mock data, Segment seg, double time) {
		}

		@Override
		public void markSelectedAllele(int allele, double p) {
			
		}

		@Override
		public void finishedWithSegment(Segment seg) {
			
		}

		@Override
		public StringBuilder getTreeString() {
			return null;
		}

		@Override
		public void setLeafSet(FixedBitSet leafSet) {
		}

		@Override
		public FixedBitSet getLeafSet() {
			return null;
		}

		@Override
		public double getSegmentHeight() {
			return 0;
		}

		@Override
		public double getSegmentTreeLength() {
			return 0;
		}
		
	}
}