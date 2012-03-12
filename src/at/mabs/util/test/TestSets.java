package at.mabs.util.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;


import at.mabs.util.cuckoo.CHashMap;
import at.mabs.util.cuckoo.CHashSet;
import at.mabs.util.cuckoo.CIdentityHashMap;
import at.mabs.util.cuckoo.CIdentityHashSet;
import at.mabs.util.cuckoo.CIntHashMap;
import at.mabs.util.random.Random64;

public class TestSets {
	private Random64 random =new Random64();
	private int itemCount =10000;

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCHashSet() {
		CHashSet<Object> set =new CHashSet<Object>(2, .5);// ensure resizing...
		testSet(set);
		set.clear();
		for(int i=0;i<100;i++){
			set.add(new Integer(1));
			assertEquals(1, set.size());
			assertTrue(set.contains(new Integer(1)));
		}
	}
	
	@Test
	public void testCIdentityHashSet(){
		CIdentityHashSet<Object> set =new CIdentityHashSet<Object>(2,.5);// ensure resizing...
		testSet(set);
		set.clear();
		for(int i=0;i<100;i++){
			set.add(new Integer(1));
			assertEquals(i+1, set.size());
			assertFalse(set.contains(new Integer(1)));
		}
	}
	
	@Test
	public void testCHashMap(){
		CHashMap<Object, Object> map=new CHashMap<Object, Object>(2,0.5);
		testMap(map);
		map.clear();
		for(int i=0;i<100;i++){
			map.put(new Integer(1), new Object());
			assertEquals(1, map.size());
			assertNotNull(map.get(new Integer(1)));
		}
	}
	
	
	
	@Test
	public void testCIdentityHashMap(){
		CIdentityHashMap<Object, Object> map=new CIdentityHashMap<Object, Object>(2,.5);
		testMap(map);
		map.clear();
		for(int i=0;i<100;i++){
			map.put(new Integer(1), new Object());
			assertEquals(i+1, map.size());
			assertNull(map.get(new Integer(1)));
		}
	}
	
	@Test
	public void testCIntHashMap(){
		CIntHashMap<Object> intMap=new CIntHashMap<Object>(2,.75);
		testIntegerMap(intMap);
		intMap.clear();
		int size=1000;
		for(int i=0;i<size;i++){
			intMap.put(i, new Integer(i));
			assertEquals(i+1, intMap.size());
			assertTrue(intMap.containsKey(i));
		}
		for(int i=0;i<size;i++){
			intMap.remove(i);
			assertEquals(size-i-1, intMap.size());
			assertFalse(intMap.containsKey(i));
		}
		for(int i=0;i<size;i++){
			int r=random.nextInt();
			Object o=new Object();
			intMap.put(r, o);
			assertTrue(intMap.get(r)==o);
		}
		
	}

	public void testSet(Set<Object> set) {
		// first test add and contains
		set.clear();
		assertTrue("Clear should make it empty", set.isEmpty());
		Object[] items=new Object[itemCount];
		for (int i =0; i < itemCount; i++) {
			Integer o =Integer.valueOf(i);
			items[i]=o;
			set.add(o);
			assertTrue("add/contains", set.contains(o));
			assertEquals("Size check", i + 1, set.size());
		}

		for (int i =0; i < itemCount; i++) {
			Object o =items[i];
			boolean mod =set.remove(o);
			assertTrue("remove returns true", mod);
			assertFalse("remove/contains", set.contains(o));
			assertEquals("Size check", itemCount - i - 1, set.size());
		}

		set.clear();
		assertTrue("Clear should make it empty", set.isEmpty());

		for (int i =0; i < itemCount; i++) {
			Integer o =Integer.valueOf(random.nextInt());
			items[i]=o;
			set.add(o);
			assertTrue("add/contains Random:" + set.size() + "\t" + o, set.contains(o));
		}

		set.clear();
		assertTrue("Clear should make it empty", set.isEmpty());

		// now for the iterators part...
		int count =0;
		for (Object o : set) {
			assertTrue("iterator contains", set.contains(o));
			count++;
		}
		assertEquals("iterator size check", count, set.size());

		Iterator<Object> iterator =set.iterator();
		while (iterator.hasNext()) {
			Object o =iterator.next();
			assertTrue("iterator contains", set.contains(o));
			iterator.remove();
			assertFalse("iterator removed", set.contains(o));
		}
		assertTrue("everything removed with an iterator", set.isEmpty());

		// now for collections of add and remove...
		HashSet<Object> hashSet =new HashSet<Object>();
		for (int i =0; i < itemCount; i++) {
			hashSet.add(random.nextDouble());
		}

		set.clear();
		assertTrue("Clear should make it empty", set.isEmpty());
		set.addAll(hashSet);
		assertTrue("contains All", set.containsAll(hashSet));
		set.remove(set.iterator().next());
		assertFalse("!contains All", set.containsAll(hashSet));

		HashSet<Object> hashSet2 =new HashSet<Object>();
		count =0;
		for (Object o : hashSet) {
			hashSet2.add(o);
			count++;
			if (count >= set.size() / 2)
				break;
		}

		set.addAll(hashSet);
		set.removeAll(hashSet2);
		assertEquals("remove all", hashSet.size() - hashSet2.size(), set.size());
		set.addAll(hashSet);
		set.retainAll(hashSet2);
		assertEquals("retain all", hashSet2.size(), set.size());
		assertTrue("retain all", set.containsAll(hashSet2));
		assertFalse("retain all", set.containsAll(hashSet));

		Object hash1=new Object(){
			@Override
			public int hashCode() {
			
				return 1;
			}
		};
		Object one=1;
		set.clear();
		set.add(hash1);
		set.add(one);
		assertEquals(2, set.size());
		set.remove(hash1);
		set.add(one);
		assertEquals(1, set.size());
		
	}
	
	public void testMap(Map<Object,Object> map){
		Object[] keys=new Object[itemCount];
		Object[] values=new Object[itemCount];
		
		for(int i=0;i<itemCount;i++){
			keys[i]=Double.valueOf(i);
			values[i]=Double.valueOf(random.nextDouble());
		}
		
		map.clear();
		assertTrue(map.isEmpty());
		for(int i=0;i<itemCount;i++){
			map.put(keys[i], values[i]);
			assertTrue(map.containsKey(keys[i]));
			assertTrue(map.get(keys[i])==values[i]);
			assertEquals(i+1, map.size());
			//assertTrue(map.containsValue(values[i])); //TOO SLOW
		}
		
		
		
		for(int i=0;i<itemCount;i++){
			Object o=map.remove(keys[i]);
			assertFalse(map.containsKey(keys[i]));
			assertTrue(o==values[i]);
		}
		assertTrue(map.isEmpty());
		
		//now for random action.
		for(int i=0;i<itemCount;i++){
			Object k=Integer.valueOf(random.nextInt());
			Object v=Integer.valueOf(random.nextInt());
			map.put(k,values[i]);
			assertTrue(map.containsKey(k));
			Object o=map.put(k,v);
			assertTrue(o==values[i]);
			assertTrue(map.get(k)==v);
			assertEquals(i+1, map.size());
		}
		
		map.clear();
		assertTrue(map.isEmpty());
		
		for(int i=0;i<itemCount;i++){
			map.put(keys[i], values[i]);
		}
		
		int count=0;
		for(Map.Entry<Object,Object> e:map.entrySet()){
			assertTrue(map.get(e.getKey())==e.getValue());
			count++;
		}
		assertEquals(count, map.size());
		
		Collection<Object> v=map.values();
		assertEquals(count,v.size());
		Iterator<Object> iterator=v.iterator();
		for(int i=0;i<Math.min(100, itemCount);i++){
			Object o=iterator.next();
			assertTrue(map.containsValue(o));
		}
		
		assertTrue(map.values().containsAll(Arrays.asList(values)));
		
		for(Object k:map.keySet()){
			assertTrue(map.containsKey(k));
			map.remove(k);
			assertFalse(map.containsKey(k));
		}
		assertTrue(map.isEmpty());
		
		map.clear();
		
		//put all.
		HashMap<Object,Object> map2=new HashMap<Object, Object>();
		for(int i=0;i<itemCount;i++){
			map2.put(keys[i], values[i]);
		}
		
		map.putAll(map2);
		assertEquals(map2.size(),map.size());
		assertTrue(map.keySet().containsAll(map2.keySet()));
		
		//now test the key entry part. 
		Object hash1=new Object(){
			@Override
			public int hashCode() {
			
				return 1;
			}
		};
		Object one=1;
		map.clear();
		map.put(hash1, hash1);
		map.put(one,one);
		assertEquals(2, map.size());
		map.remove(hash1);
		map.put(one,one);
		assertEquals(1, map.size());
	}
	
	public void testIntegerMap(Map<Integer,Object> map){
		Integer[] keys=new Integer[itemCount];
		Object[] values=new Object[itemCount];
		
		for(int i=0;i<itemCount;i++){
			keys[i]=i;
			values[i]=Double.valueOf(random.nextDouble());
		}
		
		map.clear();
		assertTrue(map.isEmpty());
		for(int i=0;i<itemCount;i++){
			map.put(keys[i], values[i]);
			assertTrue(map.containsKey(keys[i]));
			assertTrue(map.get(keys[i])==values[i]);
			assertEquals(i+1, map.size());
			//assertTrue(map.containsValue(values[i])); //TOO SLOW
		}
		
		
		
		for(int i=0;i<itemCount;i++){
			Object o=map.remove(keys[i]);
			assertFalse(map.containsKey(keys[i]));
			assertTrue(o==values[i]);
		}
		assertTrue(map.isEmpty());
		
		//now for random action.
		for(int i=0;i<itemCount;i++){
			Integer k=Integer.valueOf(random.nextInt());
			Object v=Integer.valueOf(random.nextInt());
			map.put(k,values[i]);
			assertTrue(map.containsKey(k));
			Object o=map.put(k,v);
			assertTrue(o==values[i]);
			assertTrue(map.get(k)==v);
			assertEquals(i+1, map.size());
		}
		
		map.clear();
		assertTrue(map.isEmpty());
		
		for(int i=0;i<itemCount;i++){
			map.put(keys[i], values[i]);
		}
		
		int count=0;
		for(Map.Entry<Integer,Object> e:map.entrySet()){
			assertTrue(map.get(e.getKey())==e.getValue());
			count++;
		}
		assertEquals(count, map.size());
		
		Collection<Object> v=map.values();
		assertEquals(count,v.size());
		Iterator<Object> iterator=v.iterator();
		for(int i=0;i<Math.min(1000, itemCount);i++){
			Object o=iterator.next();
			assertTrue(map.containsValue(o));
		}
		
		assertTrue(map.values().containsAll(Arrays.asList(values)));
		
		for(Object k:map.keySet()){
			assertTrue(map.containsKey(k));
			map.remove(k);
			assertFalse(map.containsKey(k));
		}
		assertTrue(map.isEmpty());
		
		map.clear();
		
		//put all.
		HashMap<Integer,Object> map2=new HashMap<Integer, Object>();
		for(int i=0;i<itemCount;i++){
			map2.put(keys[i], values[i]);
		}
		
		map.putAll(map2);
		assertEquals(map2.size(),map.size());
		assertTrue(map.keySet().containsAll(map2.keySet()));
	}

	
}
