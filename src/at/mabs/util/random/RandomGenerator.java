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

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import at.mabs.cern.jet.random.Binomial;
import at.mabs.cern.jet.random.Poisson;



/**
 * Just a single place to manage the random number generation. All thread based. class are permitied to 
 * cache instances. 
 * @author bob
 *
 */
public final class RandomGenerator {
	
	private static final ThreadLocal<Long> seedValue=new ThreadLocal<Long>(){
		@Override
		protected Long initialValue() {
			return hashLong(System.nanoTime()+(new Object()).hashCode());
		}
	};
	
	private static final ThreadLocal<Random> randomLocal=new ThreadLocal<Random>(){
		protected Random initialValue() {
			return new Random64(seedValue.get());
		};
	};
	
	private static final ThreadLocal<Poisson> poissonLocal=new ThreadLocal<Poisson>(){
		protected Poisson initialValue() {
			return new Poisson(randomLocal.get());
		};
	};
	
	
	private static final ThreadLocal<Binomial> binomialLocal=new ThreadLocal<Binomial>(){
		protected Binomial initialValue() {
			return new Binomial(randomLocal.get());
		};
	};
	
	/** 
	 *Thomas Wang's 64 bit Mix Function
	 */
	private final static long hashLong(long key) {
		key += ~(key << 32);
		key ^= (key >>> 22);
		key += ~(key << 13);
		key ^= (key >>> 8);
		key += (key << 3);
		key ^= (key >>> 15);
		key += ~(key << 27);
		key ^= (key >>> 31);
		return key;
	}
	
	
	public static double randExp(double rate) {
		return -Math.log(getRandom().nextDouble()) / rate;
	}
	
	public static void main(String[] args){
		long time=System.nanoTime();
		long reps=100000000;
		int b=0;
		int n=1000;
		for(long i=0;i<reps;i++){
			b=getBinomial().generateBinomial(n, .5);
		}
		System.out.println("Bin:"+b+"\t"+(System.nanoTime()-time)*1e-9/reps);
		time=System.nanoTime();
		double g=0;
		for(long i=0;i<reps;i++){
			g=getRandom().nextGaussian();
		}
		System.out.println("Norm:"+g+"\t"+(System.nanoTime()-time)*1e-9/reps);
		time=System.nanoTime();
		for(long i=0;i<reps;i++){
			b=getPoisson().nextInt(11.0);
		}
		System.out.println("pos:"+b+"\t"+(System.nanoTime()-time)*1e-9/reps);
	}
	
	public static Random getRandom(){
		return randomLocal.get();
	}
	
	public static Poisson getPoisson(){
		return poissonLocal.get();
	}
	
	public static Binomial getBinomial(){
		return binomialLocal.get();
	}
	
	public static long resetSeed(){
		seedValue.remove();
		return seedValue.get();
	}
	
	public static void setThreadSeed(long seed){
		seedValue.set(seed);
		
		randomLocal.get().setSeed(seed);
		binomialLocal.get().setRandom(randomLocal.get());
		poissonLocal.get().setRandom(randomLocal.get());
	}
}
