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

/**
 * List of primitive ints
 * @author greg
 *
 */
public final class SimpleIntegerList {
	private int size;
	private int[] list;
	
	public SimpleIntegerList(int s){
		list=new int[s];
	}
	
	public void add(int v){
		if(size==list.length)
			resize();
		list[size++]=v;
	}
	
	private void resize(){
		int[] nlist=new int[list.length*2];
		System.arraycopy(list, 0, nlist, 0, size);
		list=nlist;
	}
	
	public void clear(){
		size=0;
	}
	
	public int get(int i){
		if(i>=size)
			throw new IndexOutOfBoundsException("size is "+size+" tried index "+i);
		return list[i];
	}
	
	public void sort(){
		Arrays.sort(list,0,size);
	}
	
	public int size(){
		return size;
	}
	
	public int[] getBackingArray(){
		return list;
	}
	
	public int indexOf(int e){
		for(int i=0;i<size;i++){
			if(list[i]==e)
				return i;
		}
		return -1;
	}
}
