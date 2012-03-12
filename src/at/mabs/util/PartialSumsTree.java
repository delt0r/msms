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
package at.mabs.util;

import java.util.Arrays;
import java.util.Iterator;



/**
 * a data structure for lineageData. Permits random weighted selection in ln n and updates in ln n.
 * 
 * We use the implicit binary heap tree structure and mirror instances in the same way. We could
 * have a ring buffer for the instances, but this is simpler and it won't make much difference.
 * 
 * @author bob
 * 
 */
public class PartialSumsTree<T extends PartialSumTreeElement> implements Iterable<T>{
	private double[] sums =new double[16];
	private T[] items =(T[])new PartialSumTreeElement[16];

	private int size =0;

	// private int startIndex=0;

	public void add(T data, double weight) {
		assert data.getPartialSumTreeIndex()<0;
		checkSize();
		// we add to the start index. first move the start index.
		if (size == 0) {
			size++;
			sums[0] =weight;
			items[0] =data;
			data.setPartialSumTreeIndex(0);
			return;
		}
		int startIndex =size - 1;
		int childIndex =startIndex * 2 + 1;
		sums[childIndex] =sums[startIndex];
		int index =childIndex + 1;
		while (index >= 0) {
			sums[index] +=weight;
			index =(index - 1) >> 1;// parent index
		}

		items[childIndex] =items[startIndex];
		items[childIndex].setPartialSumTreeIndex(childIndex);
		items[startIndex] =null;
		items[childIndex + 1] =data;
		data.setPartialSumTreeIndex(childIndex + 1);
		size++;

	}

	public void remove(T data) {
		assert data == items[data.getPartialSumTreeIndex()];
		if (size == 1) {
			sums[0] =0;
			items[0] =null;
			size =0;
			return;
		}
		//System.out.println("S:"+Arrays.toString(sums));
		// now we swap with end element.
		int endIndex =2 * size - 2;
		int index =data.getPartialSumTreeIndex();
		//if (index != endIndex) {
			items[index] =items[endIndex];
			items[index].setPartialSumTreeIndex(index);
			//now update weights of the moved element.
			updateWeight(items[index],sums[endIndex]);
			
		//}
		data.setPartialSumTreeIndex(-1);
		items[endIndex] =null;
		sums[endIndex]=0;
		// now move the rest of the cherry
		endIndex--;
		int parent=(endIndex)>>1;
		items[parent]=items[endIndex];
		items[parent].setPartialSumTreeIndex(parent);
		updateWeight(items[parent],sums[endIndex]);
		sums[endIndex]=0;
		items[endIndex]=null;
		size--;
		//System.out.println("E:"+Arrays.toString(sums));
	}

	public void updateWeight(T data, double newWeight) {
		assert data == items[data.getPartialSumTreeIndex()];
		int index =data.getPartialSumTreeIndex();
		double delta =newWeight - sums[index];
		//index =(index - 1) >> 1;
		while (index >= 0) {
			sums[index] +=delta;
			index =(index - 1) >> 1;
		}
	}

	/** 
	 * use the "random" number r \in 0,totalWeight to selected a item prop to weight; 
	 * 
	 * @param r
	 */
	public T select(double r){
		assert size>0;
		if(size==1)
			return items[0];
		int startIndex=size-1;
		int index=0;
		while(index<startIndex){
			int childA=index*2+1;
			int childB=index*2+2;
			if(sums[childA]>r){
				index=childA;
			}else{
				r-=sums[childA];
				index=childB;
			}
		}
		return items[index];
	}
	
	public double getTotalWeight() {
		return sums[0];
	}
	
	public void clear(){
		Arrays.fill(sums, 0);
		Arrays.fill(items, null);
		size=0;
	}

	private void checkSize() {
		if (1 + size * 2 < sums.length)
			return;
		double[] newSums =new double[sums.length * 2];
		T[] newItems =(T[])new PartialSumTreeElement[items.length * 2];

		System.arraycopy(sums, 0, newSums, 0, sums.length);
		System.arraycopy(items, 0, newItems, 0, items.length);
		sums =newSums;
		items =newItems;
	}

	@Override
	public Iterator<T> iterator() {
	
		return new Iterator<T>() {
			private int index=size-1;//startIndex
			private int startSize=size;//cheaper "concurrent" modification errror
			@Override
			public boolean hasNext() {
				assert startSize==size;
				return index<2*size-1;
			}
			
			@Override
			public T next() {
				assert startSize==size;
				T item=items[index];
				index++;
				return item;
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public static void main(String[] args) {
		
//		PartialSumsTree<Mock> pst =new PartialSumsTree<Mock>();
//		LineageData.Mock[] mocks =new LineageData.Mock[300];
//		double[] weights =new double[mocks.length];
//		double sum =0;
//		for (int i =0; i < mocks.length; i++) {
//			double w =Math.random();
//			sum +=w;
//			mocks[i] =new LineageData.Mock();
//			
//			pst.add(mocks[i], w);
//		}
//		System.out.println("Sums:" + sum + "\t" + pst.getTotalWeight());
//		sum =0;
//		for (int i =0; i < mocks.length; i++) {
//			double w =Math.random();
//			sum +=w;
//			weights[i] =w;
//			pst.updateWeight(mocks[i], w);
//		}
//		System.out.println("Sums:" + sum + "\t" + pst.getTotalWeight());
//		for (int i =0; i < mocks.length/2; i++) {
//			int r=mocks.length-i-1;
//			sum-=weights[r];
//			pst.remove(mocks[r]);
//		}
//		System.out.println("Sums:" + sum + "\t" + pst.getTotalWeight());
//		pst.add(mocks[0], pst.getTotalWeight()*2);
//		weights[0]=-1;
//		for(int i=0;i<100;i++){
//			double r=pst.getTotalWeight()*Math.random();
//			Mock mock=pst.select(r);
//			int find=0;
//			while(mocks[find]!=mock)
//				find++;
//			System.out.println(weights[find]);
//		}
	}
	
	//private class Mock implements PartialSumTreeElement<PartialSumTreeElement<T>>
}
