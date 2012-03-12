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

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import at.mabs.util.cuckoo.CHashMap.BackedEntrySet;
import at.mabs.util.cuckoo.CHashMap.BackedKeySet;
import at.mabs.util.cuckoo.CHashMap.BackedValues;
import at.mabs.util.random.Random64;

/**
 * CHashMap with int keys. Using int methods will be much faster than using Integer keys. 
 * 
 * NOTE: No nulls!
 * 
 * @author greg ewing
 * 
 * 
 * 
 */
public class CIntHashMap<V> implements Map<Integer, V>{
	private static final int PRIME =0xbe1f14b1;
	private static final int PRIME2 =0xb4b82e39;
	private static final int PRIME3 =0xced1c241;

	private int[] keys;
	private V[] entrys;
	private int size;
	private double loadFactor;
	private int threshold;
	private int mask;// must have the lowest bit cleared
	private int bitSize;
	private Random random =new Random64();

	public CIntHashMap() {
		this(1024, .75);
	}

	public CIntHashMap(CIntHashMap<? extends V> map) {
		this(map.size(), .75);
		this.putAll(map);
	}

	public CIntHashMap(int s, double lf) {
		loadFactor =Math.min(lf, .9);
		s =Math.max(s, 16);
		int pow2 =Integer.highestOneBit(s);
		if (pow2 < s)
			pow2 <<=1;
		bitSize =Integer.numberOfTrailingZeros(pow2) + 1;
		entrys =(V[]) new Object[pow2];
		keys =new int[pow2];
		threshold =(int) (pow2 * loadFactor);
		mask =(keys.length - 1);
	}
	
	@Override
	public void clear() {
		Arrays.fill(entrys, null);
		Arrays.fill(keys, 0);
		size =0;
	}

	@Override
	public boolean containsKey(Object key) {
		if(key instanceof Integer){
			return containsKey(((Integer)key).intValue());
		}
		return false;
	}
	
	public boolean containsKey(int key) {
		int hash =hash(key);
		int hash2 =hash2(key);
		int hash3 =hash3(key);
		hash &=mask;
		hash2 &=mask;
		hash3 &=mask;
		if ((key == keys[hash] && entrys[hash]!=null) || (key == keys[hash2] && entrys[hash2]!=null) || (key == keys[hash3] && entrys[hash3]!=null))
			return true;
		return false;
	}

	/**
	 * expensive operation.
	 */
	@Override
	public boolean containsValue(Object value) {
		for (int i =0; i < entrys.length; i++) {
			if (value.equals(entrys[i]))
				return true;
		}
		return false;
	}

	@Override
	public V get(Object key) {
		if(key instanceof Integer){
			return get(((Integer)key).intValue());
		}
		return null;
	}
	
	public V get(int key) {
		int hash =hash(key);
		hash &=mask;
		// System.out.println("hashCode"+hash+"\t"+table.length);
		if (key == keys[hash])
			return entrys[hash];
		int hash2 =hash2(key);
		hash2 &=mask;
		if (key == keys[hash2])
			return entrys[hash2];
		int hash3 =hash3(key);
		hash3 &=mask;
		if (key == keys[hash3])
			return entrys[hash3];
		return null;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	

	public V put(Integer key, V value) {
		return put(key.intValue(),value);
	}
	
	public V put(int key, V value) {
		int hash =hash(key);
		hash &=mask;
		assert value != null;
		// System.out.println(Integer.toHexString(hash)+"\t"+Integer.toHexString(hash2)+"\t"+Integer.toHexString(hash3));

		Object tk =entrys[hash];
		if (key == keys[hash] && entrys[hash]!=null) {
			V old =(V) entrys[hash];
			entrys[hash] =value;
			return old;
		}
		int hash2 =hash2(key);
		hash2 &=mask;
		tk =entrys[hash2];
		if (key == keys[hash2] && entrys[hash2]!=null) {
			V old =(V) entrys[hash2];
			entrys[hash2] =value;
			return old;
		}
		int hash3 =hash3(key);
		hash3 &=mask;

		tk =entrys[hash3];
		if (key == keys[hash3] && entrys[hash3]!=null) {
			V old =(V) entrys[hash3];
			entrys[hash3] =value;
			return old;
		}
		
		tk =entrys[hash];
		if (tk == null) {
			keys[hash] =key;
			entrys[hash] =value;
			size++;
			return null;
		}
		tk =entrys[hash2];
		if (tk == null) {
			keys[hash2] =key;
			entrys[hash2] =value;
			size++;
			return null;
		}
		tk =entrys[hash3];
		if (tk == null) {
			keys[hash3] =key;
			entrys[hash3] =value;
			size++;
			return null;
		}
		// System.out.println(Integer.toHexString(hash)+"\t"+Integer.toHexString(hash2)+"\t"+Integer.toHexString(hash3));

		// fail. This is generally very rare.
		// evict key value pair...
		int pushKey =keys[hash];
		V pushValue =entrys[hash];
		keys[hash] =key;
		entrys[hash] =value;
		// System.out.println("Key:"+key+"\t"+hash);
		assert key != pushKey;
		pushInsert(pushKey, pushValue);
		size++;
		if (size > threshold)
			rehash();
		return null;
	}

	private int hash(int h) {
		// bias towards counted ints if ints count.
		return (PRIME*(h >>> (32 - bitSize))) + (h & mask);

	}

	private int hash2(int h) {
		h *=PRIME2;
		return h ^ (h >>> (32 - bitSize));

	}

	private int hash3(int h) {
		h *=PRIME3;
		return h ^ (h >>> (32 - bitSize));
	}

	public double getTrueLoadFactor() {
		return (double) size / (keys.length);
	}

	private void pushInsert(final int key, V value) {
		// System.out.println("PushKeyHash:" + key + "\t" + hash(key.hashCode()) + "\t" +
		// hash(hash(key.hashCode())));

		int hash =hash(key);
		int hash2 =hash2(key);
		int hash3 =hash3(key);
		// assert hash != hash2 && hash2 != hash3;
		hash &=mask;
		hash2 &=mask;
		hash3 &=mask;
		int workingKey =key;
		// System.out.println("FirstHash:"+hash_+"\t"+hash2_+"\t"+hash3_+"\t"+workingKey);
		while (true) {

			for (int i =0; i < size; i++) {
				if (entrys[hash] == null) {
					keys[hash] =workingKey;
					entrys[hash] =value;
					// System.out.println("Slot1 "+count);
					return;
				}
				if (entrys[hash2] == null) {
					keys[hash2] =workingKey;
					entrys[hash2] =value;
					// System.out.println("Slot1 "+count);
					return;
				}
				if (entrys[hash3] == null) {
					keys[hash3] =workingKey;
					entrys[hash3] =value;
					// System.out.println("Slot1 "+count);
					return;
				}
				// fail. So push into hash
				switch (random.nextInt(3)) {
				case 1:
					hash2 =hash3;
					break;
				case 2:
					hash2 =hash;
				}
				int nKey =keys[hash2];
				V nValue =entrys[hash2];
				keys[hash2] =workingKey;
				entrys[hash2] =value;
				workingKey =nKey;
				value =nValue;

				// int oldHash =hash;

				hash =hash(workingKey);
				hash2 =hash2(workingKey);
				hash3 =hash3(workingKey);
				hash &=mask;
				hash2 &=mask;
				hash3 &=mask;
				// System.out.println("NextHash:"+hash_+"\t"+hash2_+"\t"+hash3_+"\t"+workingKey);
				// assert (key != workingKey && !key.equals(workingKey)) || (key == workingKey) :
				// key + "\t" + workingKey + "\t" + key == workingKey;
			}
			;
			// System.out.println(hash_ + "\t" + hash2_ + "\t" + hash3_ + "\t:" + table.length +
			// "\tcount:" + count);
			rehash();
			hash =hash(workingKey);
			hash2 =hash2(workingKey);
			hash3 =hash3(workingKey);
			hash &=mask;
			hash2 &=mask;
			hash3 &=mask;
		}
	}

	private void rehash() {
		// System.out.println("Rehash:" + ((double)size)/(table.length/2));
		int[] oldkeys =keys;
		V[] oldValues =entrys;

		keys =new int[keys.length * 2];
		entrys =(V[]) new Object[keys.length * 2];
		mask =(keys.length - 1);
		bitSize++;

		threshold =(int) (keys.length * loadFactor);

		int oldSize =size;
		size =0;
		for (int i =0; i < oldkeys.length; i++) {
			if (oldValues[i] != null) {
				put(oldkeys[i], oldValues[i]);
			}
		}
		assert (size == oldSize);
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends V> m) {
		for(Entry<? extends Integer, ? extends V> e:m.entrySet()){
			put(e.getKey(),e.getValue());
		}
	}

	public void putAll(CIntHashMap<? extends V> cmap) {
		for (int i =0; i < cmap.keys.length; i +=2) {
			if (cmap.entrys[i] != null) {
				put(cmap.keys[i], cmap.entrys[i]);
			}
		}

	}

	
	
	@Override
	public V remove(Object key) {
		if(key instanceof Integer){
			return remove(((Integer)key).intValue());
		}
		return null;
	}
	
	public V remove(int key) {

		int hash =hash(key);
		int hash2 =hash2(key);
		int hash3 =hash3(key);
		hash &=mask;
		hash2 &=mask;
		hash3 &=mask;
		if (key == keys[hash]) {
			V o =(V) entrys[hash];
			entrys[hash] =null;
			keys[hash]=0;//
			size--;
			return o;
		}
		if (key == keys[hash2]) {
			V o =(V) entrys[hash2];
			entrys[hash2] =null;
			keys[hash2]=0;
			size--;
			return o;
		}
		if (key == keys[hash3]) {
			V o =(V) entrys[hash3];
			entrys[hash3] =null;
			keys[hash3]=0;
			size--;
			return o;
		}
		return null;
	}

	public int size() {
		return size;
	}

	

	public String toString() {
		String s ="CIntHashSet:[";
		for (int i =0; i < keys.length; i++) {
			if (entrys[i] != null) {
				s +="(" + keys[i] + "," + entrys[i + 1] + ")";
			}
		}
		return s + "]";
	}


	@Override
	public Set<java.util.Map.Entry<Integer, V>> entrySet() {
		
		return new BackedEntrySet();
	}
	
	class BackedEntrySet extends AbstractCollection<Entry<Integer, V>> implements Set<Entry<Integer, V>>{

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			if(o instanceof Entry){
				Entry e=(Entry)o;
				return e.getValue().equals(get(e.getKey()));
			}
			return false;
		}

		/**
		 * Will not fail fast. sorry. 
		 */
		@Override
		public Iterator<java.util.Map.Entry<Integer, V>> iterator() {
			
			return new Iterator<Map.Entry<Integer,V>>() {
				private int index=0;
				private int lastReturnedIndex=-1;
				@Override
				public boolean hasNext() {
					if(lastReturnedIndex==index)
						index++;
					while(index<entrys.length){
						if(entrys[index]!=null){
							return true;
						}
						index++;
					}
					return false;
				}
				@Override
				public Entry<Integer, V> next() {
					//check we have a next
					if(lastReturnedIndex==index)
						index++;
					if(entrys[index]==null && !hasNext()){
						throw new NoSuchElementException();
					}
					Entry<Integer, V> e=new AbstractMap.SimpleEntry<Integer,V>(keys[index],(V)entrys[index]);
					lastReturnedIndex=index;
					return e; 
				}
				@Override
				public void remove() {
					 CIntHashMap.this.remove(keys[lastReturnedIndex]);
				}
			};
		}

		@Override
		public boolean add(Entry<Integer, V> e) {
			 V result=put(e.getKey(),e.getValue());
			 return result!=null;
		}

		@Override
		public boolean remove(Object o) {
			V result=CIntHashMap.this.remove(o);
			return result!=null;
		}
		
	}
	
	@Override
	public Set<Integer> keySet() {
		return new BackedKeySet();
	}
	
	class BackedKeySet extends AbstractCollection<Integer> implements Set<Integer>{

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			return CIntHashMap.this.containsKey(o);
		}

		/**
		 * Will not fail fast. sorry. 
		 */
		@Override
		public Iterator<Integer> iterator() {
			
			return new Iterator<Integer>() {
				private int index=0;
				private int lastReturnedIndex=-1;
				@Override
				public boolean hasNext() {
					if(lastReturnedIndex==index)
						index++;
					while(index<entrys.length){
						if(entrys[index]!=null){
							return true;
						}
						index++;
					}
					return false;
				}
				@Override
				public Integer next() {
					//check we have a next
					if(lastReturnedIndex==index)
						index++;
					if(entrys[index]==null && !hasNext()){
						throw new NoSuchElementException();
					}
					
					lastReturnedIndex=index;
					return keys[index]; 
				}
				@Override
				public void remove() {
					 CIntHashMap.this.remove(keys[lastReturnedIndex]);
				}
			};
		}
		@Override
		public boolean remove(Object o) {
			V result=CIntHashMap.this.remove(o);
			return result!=null;
		}
		
	}
	
	@Override
	public Collection<V> values() {
		
		return new BackedValues();
	}
	
	class BackedValues extends AbstractCollection<V>{
		@Override
		public int size() {
			return size;
		}
		
		@Override
		public Iterator<V> iterator() {
			return new Iterator<V>() {
				private int index=0;
				private int lastReturnedIndex=-1;
				@Override
				public boolean hasNext() {
					if(lastReturnedIndex==index)
						index++;
					while(index<entrys.length){
						if(entrys[index]!=null){
							return true;
						}
						index++;
					}
					return false;
				}
				@Override
				public V next() {
					//check we have a next
					if(lastReturnedIndex==index)
						index++;
					if(entrys[index]==null && !hasNext()){
						throw new NoSuchElementException();
					}
					V value=(V)entrys[index];
					lastReturnedIndex=index;
					return value; 
				}
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

}
