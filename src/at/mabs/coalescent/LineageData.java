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

import java.util.Collection;

import at.mabs.model.ModelHistroy;
import at.mabs.util.PartialSumTreeElement;
/**
 * represents the lineage and operations that are performed on it. Such as split (recombination)
 * join (coalescent) and isEmpty. And fianlly recombination weights.
 * @author bob
 *
 */
public abstract class LineageData<T extends LineageData<T>> implements PartialSumTreeElement {
	private int partialSumTreeIndex=-1;
	private int allele=-1;
	private int deme=-1;
	
	public abstract boolean isEmpty();

	/**
	 * splits the list at the point. This instance is the low part. The new returned instance is the
	 * new list that is created from the old. note that segments are cut if needed. If a segment is
	 * cut. All the "sets to update" may be updated to reflect this split as needed. Note that it is an
	 * error to refer to LineageData that is not in the collection after this call. 
	 * 
	 * @param splitPoint
	 * @return
	 */
	public abstract T split(double splitPoint, Iterable<T> setsToUpdate);

	/**
	 * merges the set. Faster than iterating and adding by some constant. This mutates the pased in
	 * set and clears it. --note we also decrement duplicate segments and remove as needed. Perhaps
	 * need to do something with removal like set
	 * 
	 * @param set
	 */
	public abstract void join(T set, double currentTime);

	//public double getRecombinationWeight();

	//public double getRecombinationWeightSelection(ModelHistroy model);
	
	/**
	 * the start/end of the first "observed" sequence material. note this does not include selected loci
	 * @return
	 */
	public abstract double getStart();
	public abstract double getEnd();

	
	/**
	 * We manage which lineages are "selected" or beneifical in particualr with leaves carry the selected allele.
	 * @param allele
	 */
	public abstract void markSelectedAllele(int allele,double p);
	public abstract boolean contains(double p);
	/* (non-Javadoc)
	 * @see at.mabs.coalescent.PartialSumTreeElement#getPartialSumTreeIndex()
	 */
	public final int getPartialSumTreeIndex() {
		return partialSumTreeIndex;
	}

	/* (non-Javadoc)
	 * @see at.mabs.coalescent.PartialSumTreeElement#setPartialSumTreeIndex(int)
	 */
	public final void setPartialSumTreeIndex(int partialSumTreeIndex) {
		this.partialSumTreeIndex =partialSumTreeIndex;
	}

	final int getAllele() {
		return allele;
	}

	final void setAllele(int allele) {
		this.allele =allele;
	}

	final int getDeme() {
		return deme;
	}

	final void setDeme(int deme) {
		this.deme =deme;
	}
	
	
	static class Mock extends LineageData<Mock>{
		
		@Override
		public double getEnd() {
			
			return 0;
		}

		@Override
		public double getStart() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void join(Mock set, double currentTime) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Mock split(double splitPoint, Iterable<Mock> setsToUpdate) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void markSelectedAllele(int allele,double p) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public boolean contains(double p) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}