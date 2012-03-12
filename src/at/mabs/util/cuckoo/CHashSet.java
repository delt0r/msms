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
package at.mabs.util.cuckoo;

import java.lang.reflect.Array;
import java.util.*;

import at.mabs.util.random.Random64;


/**
 * 
 * @author greg
 * 
 * 
 * 
 * @param <T>
 */
public class CHashSet<T> implements Set<T> {
	private static final int PRIME =0xbe1f14b1;
	private static final int PRIME2 =0xb4b82e39;
	private static final int PRIME3 =0xced1c241;
	
	private T[] table;
	private int size;
	private double loadFactor;
	private int mask =0;
	private int bitSize;
	private int threshold;
	private Random random =new Random64();
	

	public CHashSet(){
		this(1024,.75);
	}
	
	public CHashSet(int initSize, double loadFactor) {
		this.loadFactor =loadFactor;
		int pow2Size =Integer.highestOneBit(initSize);
		if (pow2Size < initSize)
			pow2Size <<=1;
		bitSize=Integer.numberOfTrailingZeros(pow2Size);
		table =(T[]) new Object[pow2Size];
		mask =pow2Size - 1;
		threshold =(int) (pow2Size * loadFactor);
	}

	public CHashSet(Collection<? extends T> links) {
		this(links.size(),.75);
		addAll(links);
	}

	@Override
	public boolean add(T e) {
		if (e == null)
			throw new RuntimeException("Does not permit nulls");
		int code=e.hashCode();
		int hash =hash(code);
		int hash2 = hash2(code);
		int hash3 =hash3(code);
		hash =hash & mask;
		hash2 =hash2 & mask;
		hash3 =hash3 & mask;
		//inlining for performance. makes add about 20% faster
		if (e.equals(table[hash]) || e.equals(table[hash2]) || e.equals(table[hash3]))
			return false;
		if(table[hash]==null){
			table[hash]=e;
		}else if(table[hash2]==null){
			table[hash2]=e;
		}else if(table[hash3]==null){
			table[hash3]=e;
		}else{
			T pushed=table[hash];
			table[hash]=e;
			//System.out.println("Pushing "+e);
			pushInsert(pushed);
		}
		size++;
		// check size... to avoid cascading rehashes.
		if (size > threshold)
			rehash();
		return true;
	}


	private int hash(int h) {
		h*=PRIME;
		return h^(h>>>(32-bitSize));

	}

	private int hash2(int h) {
		h*=PRIME2;
		return h^(h>>>(32-bitSize));

	}

	private int hash3(int h) {
		h*=PRIME3;
		return h^(h>>>(32-bitSize));
	}
	
	private boolean pushInsert(T e) {
		int code=e.hashCode();
		int hash =hash(code);
		int hash2 = hash2(code);
		int hash3 =hash3(code);
		hash =hash & mask;
		hash2 =hash2 & mask;
		hash3 =hash3 & mask;
		while (true) {
			for (int i =0; i < size; i++) {
				if (table[hash] == null) {
					table[hash] =e;
					// System.out.println("Iter1:"+i);
					return true;
				}
				if (table[hash2] == null) {
					table[hash2] =e;
					// System.out.println("Iter2:"+i);
					return true;
				}
				if (table[hash3] == null) {
					table[hash3] =e;
					// System.out.println("Iter3:"+i+"\t"+hash+"\t"+hash2+"\t"+hash3);
					return true;
				}
				// didn't add --evict hash element
				switch(random.nextInt(3)){
				case 1:
					hash=hash2;
					break;
				case 2:
					hash=hash3;
				}
				T pushed =table[hash];
				table[hash] =e;
				e =pushed;
				int oldHash =hash;
				// now get all the hashes for the new element
				code=e.hashCode();
				hash =hash(code);
				hash2 = hash2(code);
				hash3 =hash3(code);
				hash =hash & mask;
				hash2 =hash2 & mask;
				hash3 =hash3 & mask;
				// check the patalogical case of old=new.
				if (hash == oldHash) {
					hash =hash3;
					hash3 =oldHash;// swap
				}

			}
			//System.out.println("PushRehash");
			rehash();
			// the last element we still have now has invalid hash codes
			// this should be very rare and so expensive that this hardly matters
			code=e.hashCode();
			hash =hash(code);
			hash2 = hash2(code);
			hash3 =hash3(code);
			hash =hash & mask;
			hash2 =hash2 & mask;
			hash3 =hash3 & mask;
		}
		// return false;

	}

	// must readd all old elements once done...
	private void rehash() {
		T[] oldTable =table;
		int oldSize =size;

		table =(T[]) new Object[table.length * 2];
		size =0;
		mask =table.length - 1;// power of 2 assumed...
		threshold =(int) (table.length * loadFactor);
		bitSize++;
		for (int i =0; i < oldTable.length; i++) {
			if (oldTable[i] != null)
				add(oldTable[i]);
		}
		assert (size == oldSize);
		//System.out.println("Rehash:" + table.length);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		if(c instanceof CHashSet){
			return addAll((CHashSet<? extends T>)c);
		}
		boolean flag =false;
		for (T e : c) {
			flag |=add(e);
		}
		return flag;
	}

	public boolean addAll(CHashSet<? extends T> set){
		boolean b=false;
		for(int i=0;i<set.table.length;i++){
			if(set.table[i]!=null){
				b|=add(set.table[i]);
			}
		}
		return b;
	}
	
	@Override
	public void clear() {
		size =0;
		Arrays.fill(table, null);
	}

	@Override
	public boolean contains(Object e) {
		if (e == null)
			return false;
		int code=e.hashCode();
		int hash =hash(code);
		int hash2 = hash2(code);
		int hash3 =hash3(code);
		hash =hash & mask;
		hash2 =hash2 & mask;
		hash3 =hash3 & mask;

		if (e.equals(table[hash]) || e.equals(table[hash2]) || e.equals(table[hash3]))
			return true;
		return false;

	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (!contains(e))
				return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public Iterator<T> iterator() {

		return new Iterator<T>() {
			private int count =0;
			private int size =CHashSet.this.size;
			private int currentIndex =-1;
			private boolean removed =false;

			@Override
			public boolean hasNext() {

				return count < size;
			}

			@Override
			public T next() {
				if(count==size)
					throw new NoSuchElementException();
				count++;
				currentIndex++;
				while (table[currentIndex] == null) {
					currentIndex++;
				}
				removed =false;
				return table[currentIndex];
			}

			@Override
			public void remove() {
				// pretty messy really. Perhaps there is a more elegant way
				if (removed)
					throw new NoSuchElementException("Element Already removed");
				// not the most effecent...
				CHashSet.this.remove(table[currentIndex]);
				size--;
				currentIndex--;
				count--;
				removed =true;
			}

		};
	}

	@Override
	public boolean remove(Object o) {
		if (o == null)
			return false;
		int code=o.hashCode();
		int hash =hash(code);		
		hash =hash & mask;

		if (o.equals(table[hash]) ) {
			table[hash] =null;
			size--;
			return true;
		}
		int hash2 = hash2(code);
		hash2 =hash2 & mask;
		if (o.equals(table[hash2])) {
			table[hash2] =null;
			size--;
			return true;
		}
		int hash3 =hash3(code);
		hash3 =hash3 & mask;
		if (o.equals(table[hash3])) {
			table[hash3] =null;
			size--;
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean flag =false;
		for (Object o : c) {
			flag |=remove(o);
		}
		return flag;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean flag =false;
		for(int i=0;i<table.length;i++){
			T e=table[i];
			if(e!=null && !c.contains(e)){
				table[i]=null;
				size--;
				flag=true;
			}
		}
		return flag;
	}

	@Override
	public int size() {

		return size;
	}

	@Override
	public <E> E[] toArray(E[] a) {
		if(a.length<size){
			a=(E[])Array.newInstance(a.getClass().getComponentType(),size);
		}
		int index=0;
		for(int i=0;i<table.length;i++){
			if(table[i]!=null)
				a[index++]=(E)table[i];
		}
		return a;
	}
	
	@Override
	public Object[] toArray() {
		Object[] array=new Object[size];
		int index=0;
		for(int i=0;i<table.length;i++){
			if(table[i]!=null)
				array[index++]=table[i];
		}
		return array;
	}

	public String toString() {
		StringBuilder builder =new StringBuilder();
		builder.append("IdentSet:" + size + "(");
		for (int i =0; i < table.length; i++) {
			if (table[i] != null) {
				builder.append(table[i].toString() + ",");
			}
		}
		builder.append(")");
		return builder.toString();
	}

	public T pickRandom(Random random){
		int start=random.nextInt(table.length);
		
		while(table[start]==null){
			start++;
			if(start==table.length)
				start=0;
		}
		return table[start];
	}
	
}
