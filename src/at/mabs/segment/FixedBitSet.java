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

import java.util.Collection;

import at.mabs.util.Util;

/**
 * very simple bit set with less over head and a custom operation or two.
 * 
 * 
 * @author bob
 * 
 */
public class FixedBitSet {
	private long[] bits;
	private int totalLeafCount;
	private long time = 0;// sampling time. Default 0

	private FixedBitSet() {
		// serilization constructor.
	}

	public FixedBitSet(int size) {
		bits = new long[1 + size / 64];
		this.totalLeafCount = size;
	}

	public FixedBitSet(FixedBitSet leafSet) {
		bits = leafSet.bits.clone();
		totalLeafCount = leafSet.totalLeafCount;
	}

	public void and(FixedBitSet leaves) {
		for (int i = 0; i < bits.length; i++) {
			bits[i] &= leaves.bits[i];
		}
	}

	public void nand(FixedBitSet leaves) {
		for (int i = 0; i < bits.length; i++) {
			bits[i] &= ~leaves.bits[i];
		}
	}

	public void xor(FixedBitSet leaves) {
		for (int i = 0; i < bits.length; i++) {
			bits[i] ^= leaves.bits[i];
		}
	}

	public void or(FixedBitSet leaves) {
		for (int i = 0; i < bits.length; i++) {
			bits[i] |= leaves.bits[i];
		}
	}

	public int countSetBits() {
		int c = 0;
		for (int i = 0; i < bits.length; i++) {
			c += Long.bitCount(bits[i]);
		}
		return c;
	}

	public boolean bitsOverlap(FixedBitSet set) {

		for (int i = 0; i < bits.length; i++) {
			if ((bits[i] & set.bits[i]) != 0) {
				return true;
			}
		}
		// System.out.println("OverLap:\n\t"+toBinaryString()+"\n\t"+set.toBinaryString());
		return false;
	}

	/**
	 * counts the number of set bits after an and operation with mask. however
	 * this bit set remains unmodified.
	 * 
	 * @param mask
	 * @return
	 */
	public int countSetBitsMask(FixedBitSet mask) {
		int c = 0;
		for (int i = 0; i < bits.length; i++) {
			c += Long.bitCount(bits[i] & mask.bits[i]);
		}
		return c;
	}

	public int countSetBitsComplementMask(FixedBitSet mask) {
		int c = 0;
		for (int i = 0; i < bits.length; i++) {
			c += Long.bitCount(bits[i] & (~mask.bits[i]));
		}
		return c;
	}

	public boolean contains(int leafNumber) {
		return ((bits[leafNumber >> 6] >>> (leafNumber & 63)) & 1l) == 1;
	}

	public void set(int leafNumber) {
		bits[leafNumber >> 6] |= 1l << (leafNumber & 63);
		// System.out.println(Arrays.toString(bits));
	}

	public void clear(int leafNumber){
		bits[leafNumber >> 6] &= ~(1l << (leafNumber & 63));
	}
	
	public void set(int i, boolean b) {
		if (b)
			set(i);
		else
			clear(i);
	}

	public void set(int start, int end) {
		// System.out.println("SetBits:"+start+"->"+end);
		for (int i = start; i <= end; i++) {
			set(i);
		}
		// System.out.println("Set:"+start+"\t"+end+"\n\t"+toBinaryString());
	}

	public String toBinaryString() {
		StringBuilder sb = new StringBuilder(this.totalLeafCount);
		for (int i = totalLeafCount; i >= 0; i--) {
			sb.append(contains(i) ? '1' : '0');
		}
		return sb.toString();
	}

	/**
	 * use the fact that if we have n ones in the bit set. Then there are
	 * n*(total-n) pair wise differences. We then nomalized by nCr?
	 * 
	 * @return
	 */
	public double getPairwiseDifference() {
		int i = countSetBits();
		return (double) i * (totalLeafCount - i);
	}

	public double getPairwiseDifferenceMask(FixedBitSet mask) {
		int j = mask.countSetBits();
		int i = countSetBitsMask(mask);
		return (double) i * (j - i);
	}

	public int size() {

		return totalLeafCount;
	}

	public void invert() {
		for (int i = 0; i < this.bits.length; i++) {
			bits[i] = ~bits[i];
		}
		int b = totalLeafCount % 64;
		if (b > 0) {
			long mask = -1L >>> (64 - b);
			// System.out.println("MASK:"+Long.toBinaryString(mask));
			bits[bits.length - 1] &= mask;
		}

	}

	public int getTotalLeafCount() {
		return totalLeafCount;
	}

	public String toString() {
		String s = "LeafSet:" + toBinaryString();
		// for(int i=bits.length-1;i>=0;i--){
		// String bs=Long.toBinaryString(bits[i]);
		// s+="0000000000000000000000000000000000000000000000000000000000000000".substring(bs.length())+bs;
		// }
		return s;
	}

	public FixedBitSet union(Collection<FixedBitSet> leafs) {
		if (leafs.isEmpty())
			return new FixedBitSet(this.totalLeafCount);// not null!
		FixedBitSet leafSet = null;
		for (FixedBitSet l : leafs) {
			if (leafSet == null) {
				leafSet = new FixedBitSet(l);
			} else {
				leafSet.or(l);
			}
		}
		return leafSet;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void shiftRight() {
		long carry = 0;
		for (int i = bits.length - 1; i >= 0; i--) {
			long nv = carry | (bits[i] >>> 1);
			carry = (bits[i] & 1) == 0 ? 0 : 0x8000000000000000l;
			bits[i] = nv;
		}
	}

	public long lastBitsAsLong() {
		return bits[0];

	}

	public static void main(String[] args) {
		FixedBitSet fbs = new FixedBitSet(128);
		fbs.set(70);
		fbs.set(90);
		for (int i = 0; i < 128; i++) {
			fbs.shiftRight();
			System.out.println(fbs);
		}
	}

}
