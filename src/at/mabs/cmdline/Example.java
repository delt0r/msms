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
package at.mabs.cmdline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Example {
	private List list=new ArrayList();
	
	/**
	 * Only need one description per Switch and must always be the same for the same switch. 
	 * However usage can be different. If no usage is provided, a default will be generated. 
	 * Its only used for "help". 
	 * @param d
	 */
	@CLNames(names={"-D","--doubles"})
	@CLDescription("Adds doubles to the object store")
	@CLUsage("value ...")
	public void addSingleDouble(double d){
		System.out.println("Adding a double");
		list.add(d);
	}
	
	
	/**
	 * Duplicate switches *must* have identical CLNames parameters
	 * @param a
	 * @param b
	 */
	@CLNames(names={"-D","--doubles"})
	@CLUsage("")//don't add usage
	public void addTwoDouble(double a,double b){
		System.out.println("Adding 2 doubles");
		list.add(a);
		list.add(b);
	}
	
	/**
	 * an array type is for one to many options. You can only have one array per method, but many 
	 * other arguments are fine. Note that arrays can end up with zero length. 
	 * @param many
	 */
	@CLNames(names={"-D","--doubles"})
	@CLUsage("")
	public void addManyDoubles(double[] many){
		System.out.println("Adding many doubles");
		for(double d:many){
			list.add(d);
		}
	}
	
	/**
	 * the empty name is options that precede any other valid switch.
	 * @param strings
	 */
	@CLNames(names={""})
	public void switchlessOptions(String[] strings){
		System.out.println("The switchless:"+strings);
		if(strings==null)
			return;
		for(String s:strings){
			list.add(s);
		}
	}
	
	/**
	 * I can set the rank to force the invokation order. Order without rank is the order they
	 * appear. With rank we can force things to be invoked before or after other switches regardless of what
	 * order they appear in. the default is zero. Invokation is lowest first. 
	 * @param a
	 * @param others
	 * @param b
	 */
	@CLNames(names={"-IdI","-i"},rank=-1)
	@CLDescription("We can mix types easily")
	public void addIntDoublesInt(int a,double[] others,int b){
		System.out.println("Int double[] Int");
		list.add(a);
		for(double d:others){
			list.add(d);
		}
		list.add(b);
	}
	
	@CLNames(names={"-true","?"})
	@CLDescription("Set a parameter to true")
	public void setToTrue(){
		System.out.println("Set to true");
		list.add(true);
	}
	
	public static void main(String[] args) {
		try {
			Example example =new Example();
			CmdLineParser cmp =new CmdLineParser(Example.class);
			System.out.println(cmp.longUsage());
			cmp.processArguments(args,example);
			System.out.println("Output:"+example.list);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
