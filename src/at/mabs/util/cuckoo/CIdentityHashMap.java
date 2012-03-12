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
 * Just a CHashMap with identity rather than .equals
 * @author greg ewing
 * 
 * I put this into the public domain. 
 * 
 */
public class CIdentityHashMap<K, V> implements Map<K, V> {
	private static final int PRIME =0xbe1f14b1;
	private static final int PRIME2 =0xb4b82e39;
	private static final int PRIME3 =0xced1c241;

	private Object[] table;
	private int size;
	private double loadFactor;
	private int threshold;
	private int mask;// must have the lowest bit cleared
	private int bitSize;
	private Random random =new Random64();

	public CIdentityHashMap() {
		this(1024, .70);
	}

	public CIdentityHashMap(Map<? extends K, ? extends V> map) {
		this(map.size(), .70);
		this.putAll(map);
	}

	public CIdentityHashMap(int s, double lf) {
		loadFactor =Math.min(lf, .9);
		s =Math.max(s, 16);
		int pow2 =Integer.highestOneBit(s);
		if (pow2 < s)
			pow2 <<=1;
		bitSize=Integer.numberOfTrailingZeros(pow2)+1;
		table =new Object[pow2 * 2];
		threshold =(int) (pow2 * loadFactor);
		mask =(table.length - 1) ^ 1;
	}

	@Override
	public void clear() {
		Arrays.fill(table, null);
		size =0;
	}

	@Override
	public boolean containsKey(Object key) {
		int code=System.identityHashCode(key);
		int hash =hash(code);
		int hash2 =hash2(code);
		int hash3 =hash3(code);
		hash &=mask;
		hash2 &=mask;
		hash3 &=mask;
		if (key==table[hash] || key==table[hash2] || key==table[hash3])
			return true;
		return false;
	}

	/**
	 * expensive operation.
	 */
	@Override
	public boolean containsValue(Object value) {
		for (int i =1; i < table.length; i +=2) {
			if (value.equals(table[i]))
				return true;
		}
		return false;
	}


	@Override
	public V get(Object key) {
		int code=System.identityHashCode(key);
		int hash =hash(code);
		
		
		hash &=mask;
		
		
		// System.out.println("hashCode"+hash+"\t"+table.length);
		if (key==table[hash])
			return (V) table[hash + 1];
		int hash2 =hash2(code);
		hash2 &=mask;
		if (key==table[hash2])
			return (V) table[hash2 + 1];
		int hash3 =hash3(code);
		hash3 &=mask;
		if (key==table[hash3])
			return (V) table[hash3 + 1];
		return null;
	}

	@Override
	public boolean isEmpty() {

		return size == 0;
	}


	@Override
	public V put(K key, V value) {
		if(key==null)
			throw new NullPointerException("Keys cannot be null");
		int code=System.identityHashCode(key);
		int hash =hash(code);
		
		
		hash &=mask;
		
	
		// System.out.println(Integer.toHexString(hash)+"\t"+Integer.toHexString(hash2)+"\t"+Integer.toHexString(hash3));

		Object tk =table[hash];
		
		if (key==tk) {
			V old =(V) table[hash + 1];
			table[hash + 1] =value;
			return old;
		}
		int hash2 =hash2(code);
		hash2 &=mask;
		
		tk =table[hash2];
		
		if (key==tk) {
			V old =(V) table[hash2 + 1];
			table[hash2 + 1] =value;
			return old;
		}

		int hash3 =hash3(code);
		hash3 &=mask;
		
		tk =table[hash3];
		
		if (key==tk) {
			V old =(V) table[hash3 + 1];
			table[hash3 + 1] =value;
			return old;
		}

		tk =table[hash];
		if (tk == null) {
			table[hash] =key;
			table[hash + 1] =value;
			size++;
			return null;
		}
		tk =table[hash2];
		if (tk == null) {
			table[hash2] =key;
			table[hash2 + 1] =value;
			size++;
			return null;
		}
		tk =table[hash3];
		if (tk == null) {
			table[hash3] =key;
			table[hash3 + 1] =value;
			size++;
			return null;
		}
		// System.out.println(Integer.toHexString(hash)+"\t"+Integer.toHexString(hash2)+"\t"+Integer.toHexString(hash3));

		// fail. This is generally very rare.
		// evict key value pair...
		Object pushKey =table[hash];
		Object pushValue =table[hash + 1];
		table[hash] =key;
		table[hash + 1] =value;
		// System.out.println("Key:"+key+"\t"+hash);
		assert key!=pushKey;
		pushInsert(pushKey, pushValue);
		size++;
		if (size > threshold)
			rehash();
		return null;
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

	public double getTrueLoadFactor() {
		return (double) size / (table.length / 2);
	}

	private void pushInsert(final Object key, Object value) {
		//System.out.println("PushKeyHash:" + key + "\t" + hash(key.hashCode()) + "\t" + hash(hash(key.hashCode())));
		int code=System.identityHashCode(key);
		int hash =hash(code);
		int hash2 =hash2(code);
		int hash3 =hash3(code);
		//assert hash != hash2 && hash2 != hash3;
		hash &=mask;
		hash2 &=mask;
		hash3 &=mask;
		Object workingKey =key;
		//System.out.println("FirstHash:"+hash_+"\t"+hash2_+"\t"+hash3_+"\t"+workingKey);
		while (true) {
			
			for(int i=0;i<size;i++) {
				if (table[hash] == null) {
					table[hash] =workingKey;
					table[hash + 1] =value;
					//System.out.println("Slot1 "+count);
					return;
				}
				if (table[hash2] == null) {
					table[hash2] =workingKey;
					table[hash2 + 1] =value;
					//System.out.println("Slot2 "+count);
					return;
				}
				if (table[hash3] == null) {
					table[hash3] =workingKey;
					table[hash3 + 1] =value;
					//System.out.println("Slot3 "+count);
					return;
				}
				// fail. So push into hash
				switch(random.nextInt(3)){
				case 1:
					hash2=hash3;
					break;
				case 2:
					hash2=hash;
				}
				Object nKey =table[hash2];
				Object nValue =table[hash2 + 1];
				table[hash2] =workingKey;
				table[hash2 + 1] =value;
				workingKey =nKey;
				value =nValue;
				
				// int oldHash =hash;
				code=System.identityHashCode(workingKey);
				hash =hash(code);
				hash2 =hash2(code);
				hash3 =hash3(code);
				hash &=mask;
				hash2 &=mask;
				hash3 &=mask;
				//System.out.println("NextHash:"+hash_+"\t"+hash2_+"\t"+hash3_+"\t"+workingKey);
				//assert (key != workingKey && !key.equals(workingKey)) || (key == workingKey) : key + "\t" + workingKey + "\t" + key == workingKey;
			};
			//System.out.println(hash_ + "\t" + hash2_ + "\t" + hash3_ + "\t:" + table.length + "\tcount:" + count);
			rehash();
			code=System.identityHashCode(workingKey);
			hash =hash(code);
			hash2 =hash2(code);
			hash3 =hash3(code);
			hash &=mask;
			hash2 &=mask;
			hash3 &=mask;
		}
	}

	private void rehash() {
		// System.out.println("Rehash:" + ((double)size)/(table.length/2));
		Object[] oldtable =table;

		table =new Object[table.length * 2];
		mask =(table.length - 1) ^ 1;// clear the last bit. Assumes table.lenght is a power of 2
		bitSize++;
		assert table.length==(1<<bitSize);
		threshold =(int) (table.length * loadFactor / 2);

		int oldSize =size;
		size =0;
		for (int i =0; i < oldtable.length; i +=2) {
			if (oldtable[i] != null) {
				put((K) oldtable[i], (V) oldtable[i + 1]);
			}
		}
		assert (size == oldSize);
	}

	/**
	 * fast with a CIdentityHashMap. Slow otherwise
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m instanceof CIdentityHashMap) {
			CIdentityHashMap cmap =(CIdentityHashMap) m;
			for (int i =0; i < cmap.table.length; i +=2) {
				if (cmap.table[i] != null) {
					put((K) cmap.table[i], (V) cmap.table[i + 1]);
				}
			}
			return;
		}
		// now for the slow version...
		Set<? extends Map.Entry<? extends K, ? extends V>> entrys =m.entrySet();
		Iterator<? extends Map.Entry<? extends K, ? extends V>> iter =entrys.iterator();
		while (iter.hasNext()) {
			Map.Entry<? extends K, ? extends V> entry =iter.next();
			put(entry.getKey(), entry.getValue());
		}

	}

	@Override
	public V remove(Object key) {
		int code=System.identityHashCode(key);
		int hash =hash(code);
		int hash2 =hash2(code);
		int hash3 =hash3(code);
		hash &=mask;
		hash2 &=mask;
		hash3 &=mask;
		if (key==table[hash]) {
			table[hash] =null;
			V o =(V) table[hash + 1];
			table[hash + 1] =null;
			size--;
			return o;
		}
		if (key==table[hash2]) {
			table[hash2] =null;
			V o =(V) table[hash2 + 1];
			table[hash2 + 1] =null;
			size--;
			return o;
		}
		if (key==table[hash3]) {
			table[hash3] =null;
			V o =(V) table[hash3 + 1];
			table[hash3 + 1] =null;
			size--;
			return o;
		}
		return null;
	}

	@Override
	public int size() {
		return size;
	}

	

	public String toString() {
		String s ="CHashSet:[";
		for (int i =0; i < table.length; i +=2) {
			if (table[i] != null) {
				s +="(" + table[i] + "," + table[i + 1] + ")";
			}
		}
		return s + "]";
	}

	

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		
		return new BackedEntrySet();
	}
	
	class BackedEntrySet extends AbstractCollection<Entry<K, V>> implements Set<Entry<K, V>>{

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
		public Iterator<java.util.Map.Entry<K, V>> iterator() {
			
			return new Iterator<Map.Entry<K,V>>() {
				private int index=0;
				private int lastReturnedIndex=-1;
				@Override
				public boolean hasNext() {
					if(lastReturnedIndex==index)
						index+=2;
					while(index<table.length){
						if(table[index]!=null){
							return true;
						}
						index+=2;
					}
					return false;
				}
				@Override
				public Entry<K, V> next() {
					//check we have a next
					if(lastReturnedIndex==index)
						index+=2;
					if(table[index]==null && !hasNext()){
						throw new NoSuchElementException();
					}
					Entry<K, V> e=new AbstractMap.SimpleEntry<K,V>((K)table[index],(V)table[index+1]);
					lastReturnedIndex=index;
					return e; 
				}
				@Override
				public void remove() {
					 CIdentityHashMap.this.remove(table[lastReturnedIndex]);
				}
			};
		}

		@Override
		public boolean add(java.util.Map.Entry<K, V> e) {
			 V result=put(e.getKey(),e.getValue());
			 return result!=null;
		}

		@Override
		public boolean remove(Object o) {
			V result=CIdentityHashMap.this.remove(o);
			return result!=null;
		}
		
	}
	
	
	@Override
	public Set<K> keySet() {
		return new BackedKeySet();
	}
	
	class BackedKeySet extends AbstractCollection<K> implements Set<K>{

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(Object o) {
			return CIdentityHashMap.this.containsKey(o);
		}

		/**
		 * Will not fail fast. sorry. 
		 */
		@Override
		public Iterator<K> iterator() {
			
			return new Iterator<K>() {
				private int index=0;
				private int lastReturnedIndex=-1;
				@Override
				public boolean hasNext() {
					if(lastReturnedIndex==index)
						index+=2;
					while(index<table.length){
						if(table[index]!=null){
							return true;
						}
						index+=2;
					}
					return false;
				}
				@Override
				public K next() {
					//check we have a next
					if(lastReturnedIndex==index)
						index+=2;
					if(table[index]==null && !hasNext()){
						throw new NoSuchElementException();
					}
					K key=(K)table[index];
					lastReturnedIndex=index;
					return key; 
				}
				@Override
				public void remove() {
					 CIdentityHashMap.this.remove(table[lastReturnedIndex]);
				}
			};
		}
		@Override
		public boolean remove(Object o) {
			V result=CIdentityHashMap.this.remove(o);
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
						index+=2;
					while(index<table.length){
						if(table[index]!=null){
							return true;
						}
						index+=2;
					}
					return false;
				}
				@Override
				public V next() {
					//check we have a next
					if(lastReturnedIndex==index)
						index+=2;
					if(table[index]==null && !hasNext()){
						throw new NoSuchElementException();
					}
					V value=(V)table[index+1];
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
