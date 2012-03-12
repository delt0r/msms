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
package at.mabs.util.random;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * a linear random method based on xor shifts--which is a fast way to do LFSR --ie one clock per bit
 * is slow. This is faster per step that java Random.
 * 
 * This does better than LCC generators (ie passes the monkey tests and DNA tests where LCG dont).
 * In other words it does not have the hyperplane problem. 
 * 
 * This has a period of 2**128-1. This is quite easy to prove as follows.
 * 
 * the counter can be shown to be a LFSR with period 2**64-1. However we have a simple counter in
 * the stepped variable. That is after 2**64 counts stepped mod 2**64 == 0. Hence the phase is
 * shifted by one and the period of stepped and counter are relatively prime. We combine them with
 * Addition, which is slightly nonlinear due to carry. Of course we could just use a simple ++ counter.
 * But thats boring
 * 
 * We could use * for this as well and have a stronger condition for non lineararity.
 * 
 * We speed up the nextDouble function as well.
 * 
 * The main method dumps random data to standard out. this can be used with the dieharder tests. However
 * there seems to be a problems with dieharder tests when using pipes.
 * 
 * @author bob
 * 
 */
public final class Random64 extends Random {
	private static final double LONG_2_DOUBLE =1.0 / (double)(1L<<53);
	private static final long MASK_53=(1l<<53)-1;
	private static final long serialVersionUID =-6678124822567014769L;

	private static final long PRIME =0xd4d6712ee634312dl;
	private long counter =0;
	private long stepped =0;

	public Random64() {
		super();
		setSeed(System.nanoTime()+this.hashCode());
	}

	public Random64(long seed) {
		super(seed);
		setSeed(seed);
	}
	
	private void step(){
		counter ^=(counter << 21);
		counter ^=(counter >>> 35);
		counter ^=(counter << 4);
		stepped +=PRIME;
	}
	/*
	 * could use all 64 bits over 2 calls?
	 */
	@Override
	protected int next(int bits) {
		step();
		// Hate the dumb mask. 
		return (int) (((counter + stepped) >>> 31) & ((1l << bits) - 1));
	}

	@Override
	public void setSeed(long seed) {
		//System.out.println("SetSeed:"+seed);
		counter =seed;
		stepped=0xC0FFEBABEl;
		if (counter == 0)
			counter =1;
		step();
		stepped+=seed^counter;
	}

	/**
	 * just faster....
	 */
	@Override
	public double nextDouble() {
		step();
		
		return (((counter+stepped)>>>8) & MASK_53)*LONG_2_DOUBLE;
	}
	
	
	@Override
	public long nextLong() {
		step();
		//the inline rotate is to avoid the lowest bit bias/correleation
		return counter+(((stepped << 17) | (stepped >>> -17))^stepped);
	}

	
	@Override
	public int nextInt() {
		step();
		return (int)((counter+stepped)>>>32);
	}
	
	
	public static void main(String[] args) {
		
		
		Random64 rand =new Random64();
		int x =0;
		long t =System.nanoTime();
		double min=1;
		double max=0;
		double sum=0;
		for (int i =0; i < 2000000000; i++) {
			double r=rand.next(32);
			x=(int)r;
			//min=Math.min(min, r);
			//max=Math.max(max, r);
			sum+=r;
			//if((i&0xffffff)==0){
			//	System.out.println(sum/(i+1)+"\t"+min+"\t"+max);
			//}
		}
		System.out.println((System.nanoTime() - t) * 1e-9 + "\t" + x);
		System.exit(0);
//		
		try {
			byte[] buffer =new byte[1024 * 1024*8];
			for (;;) {
				rand.nextBytes(buffer);
				System.out.write(buffer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
