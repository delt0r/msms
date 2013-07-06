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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.RandomAccess;

/**
 * Order is not preserved over time. But remove/add is fast
 * @author bob
 *
 * @param <E>
 */

public class Bag<E> implements  List<E>,RandomAccess {
	private Object[] list;
	private int size=0;
	
	public Bag() {
		list=new Object[16];
	}
	
	public Bag(int size){
		list=new Object[size];
	}
	
	public Bag(Collection<E> elements){
		this(elements.size());
		for(E e:elements){
			add(e);
		}
	}
	
	private void resize(){
		Object[] nList=new Object[list.length*2];
		System.arraycopy(list, 0, nList, 0, size);
		list=nList;
	}
	
	@Override
	public boolean add(E e) {
		if(size==list.length)
			resize();
		list[size++]=e;
		return true;
	}
	/**
	 * slow O(n)
	 */
	@Override
	public boolean contains(Object o) {
		for(int i=0;i<size;i++)
			if(o.equals(list[i]))
				return true;
		return false;
	}

	

	@Override
	public Iterator<E> iterator() {
		return listIterator(0);
	}

	
	/**
	 * finds index and calls remove(int) 
	 */
	@Override
	public boolean remove(Object o) {
		for(int i=0;i<size;i++)
			if(o.equals(list[i])){
				size--;
				list[i]=list[size];
				list[size]=null;
				return true;
			}
		return false;
	}

	

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		for(E e:c)
			add(e);
		return true;
	}

	@Override
	public void clear() {
		Arrays.fill(list, null);
		size=0;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(int i=0;i<size;i++){
			if(!c.contains(list[i]))
				return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return size==0;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean flag=false;
		int i=0;
		while(i<size){
			if(c.contains(list[i])){
				size--;
				list[i]=list[size];
				list[size]=null;
				flag=true;
			}else{
				i++;
			}	
		}
		return flag;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean flag=false;
		int i=0;
		while(i<size){
			if(!c.contains(list[i])){
				size--;
				list[i]=list[size];
				list[size]=null;
				flag=true;
			}else{
				i++;
			}	
		}
		return flag;

		
	}

	@Override
	public Object[] toArray() {
		Object[] array=new Object[size];
		System.arraycopy(list, 0, array, 0, size);
		return array;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if(a.length<size){
			a=(T[])new Object[size];
		}
		System.arraycopy(list, 0, a, 0, size);
		return a;
	}

	/**
	 * to avoid degenerate O(n) performance.The existing element at index is swaped to the end.
	 */
	@Override
	public void add(int index, E element) {
		if(size==list.length)
			resize();
		list[size]=list[index];
		list[index]=element;
		size++;
		
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		while(list.length<c.size()+size)
			resize();
		for(E e:c){
			add(index++,e);
		}
		return true;
	}

	@Override
	public E get(int index) {
		assert index<size;
		return (E)list[index];
	}

	@Override
	public int indexOf(Object o) {
		for(int i=0;i<size;i++){
			if(o.equals(list[i]))
				return i;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		for(int i=size-1;i>=0;i--){
			if(o.equals(list[i]))
				return i;
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		
		return listIterator(0);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		
		return new LocalListIterator<E>(index);
	}

	/**
	 * again a swap is done to remove the item
	 */
	@Override
	public E remove(int index) {
		assert index<size;
		E e=(E)list[index];
		size--;
		list[index]=list[size];
		list[size]=null;
		return e;
	}

	@Override
	public E set(int index, E element) {
		assert index<size;
		E e=(E)list[index];
		list[index]=element;
		return e;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
	
	private class LocalListIterator<E> implements ListIterator<E> {
		private int i=0;
		
		public LocalListIterator(int start) {
			i=start;
		}
		
		@Override
		public void add(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasNext() {
			
			return i<size;
		}

		@Override
		public boolean hasPrevious() {
			
			return i>0;
		}

		@Override
		public E next() {
			assert i<size;
			E e=(E)list[i];
			i++;
			
			return e; 
		}

		@Override
		public int nextIndex() {
		
			return i;
		}

		@Override
		public E previous() {
			i--;
			return (E)list[i];
		}

		@Override
		public int previousIndex() {
			return i-1;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
			
		}

		@Override
		public void set(E e) {
			list[i-1]=e;
			
		}
		
	}

	public String toString(){
		StringBuilder builder=new StringBuilder();
		builder.append("Bag{");
		for(int i=0;i<size;i++){
			E e=get(i);
			builder.append(e);
			if(i<size-1){
				builder.append(',');
			}
		}
		builder.append('}');
		return builder.toString();
	}
	
	public static void main(String[] args){
		Bag<Number> bag=new Bag<Number>();
		for(int i=0;i<10;i++){
			bag.add(Integer.valueOf(i));
			System.out.println(bag);
		}
		Collections.shuffle(bag);
		Collections.shuffle(bag);
		Collections.shuffle(bag);
		System.out.println(bag);
		while(!bag.isEmpty()){
			bag.remove(bag.size()-1);
			Collections.shuffle(bag);
			System.out.println(bag+"\t"+bag.size());
		}
	}

	public void sort() {
		Arrays.sort(list,0,size);
	}

	public void addRandom(E item,Random random) {
		add(item);
		int n=random.nextInt(size);
		E swp=(E)list[n];
		list[n]=list[size-1];
		list[size-1]=swp;
	}
	
}
