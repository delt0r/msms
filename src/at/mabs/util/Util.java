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

import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;




public class Util {
	public static DecimalFormat defaultFormat;
	public static final DecimalFormatSymbols dfs;
	static {
		dfs =DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		defaultFormat =new DecimalFormat("0.00000", dfs);
		// defaultFormat.getDecimalFormatSymbols().setDecimalSeparator(',');
		// System.out.println(defaultFormat.getDecimalFormatSymbols().getDecimalSeparator());
	}
	 
	
	/**
	 * nulls are not permited.
	 * 
	 * @param l
	 * @return
	 */
	public static final int[] toArrayPrimitiveInteger(List<Integer> l) {
		int[] array =new int[l.size()];
		if (l instanceof RandomAccess) {
			for (int i =0; i < array.length; i++) {
				array[i] =l.get(i);

			}
			return array;
		}
		int c =0;
		for (int i : l) {
			array[c++] =i;
		}
		return array;
	}

	/**
	 * nulls are not permited.
	 * 
	 * @param l
	 * @return
	 */
	public static final double[] toArrayPrimitiveDouble(List<Double> l) {
		double[] array =new double[l.size()];
		if (l instanceof RandomAccess) {
			for (int i =0; i < array.length; i++) {
				array[i] =l.get(i);

			}
			return array;
		}
		int c =0;
		for (double i : l) {
			array[c++] =i;
		}
		return array;
	}

	public static final List<Double> asListBox(double[] a) {
		List<Double> list =new ArrayList<Double>();
		for (int i =0; i < a.length; i++)
			list.add(a[i]);
		return list;
	}

	public static final List<Integer> asListBox(int[] a) {
		List<Integer> list =new ArrayList<Integer>();
		for (int i =0; i < a.length; i++)
			list.add(a[i]);
		return list;
	}

	/**
	 * uses a default constructor if T has one. Otherwise we barf by wraping exceptions into
	 * runtimes.
	 * 
	 * @param <T>
	 * @param array
	 * @param t
	 */
	public static final <S, T extends S> void initArray(S[] array, Class<T> classInfo) {
		for (int i =0; i < array.length; i++) {
			try {
				array[i] =classInfo.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static final int sumInteger(List<Integer> list) {
		int accumulator =0;
		if (list instanceof RandomAccess) {
			for (int i =0; i < list.size(); i++) {
				accumulator +=list.get(i);
			}
		} else {
			Iterator<Integer> iter =list.iterator();
			while (iter.hasNext())
				accumulator +=iter.next();
		}
		return accumulator;
	}

	public static final double sum(List<Double> list) {
		double accumulator =0;
		if (list instanceof RandomAccess) {
			for (int i =0; i < list.size(); i++) {
				accumulator +=list.get(i);
			}
		} else {
			Iterator<Double> iter =list.iterator();
			while (iter.hasNext())
				accumulator +=iter.next();
		}
		return accumulator;
	}
	
	public static final double sum(double[] array){
		double cumulant=0;
		for(int i=0;i<array.length;i++)
			cumulant+=array[i];
		return cumulant;
	}
	
	public static final int sum(int[] array){
		int cumulant=0;
		for(int i=0;i<array.length;i++)
			cumulant+=array[i];
		return cumulant;
	}
	
	public static final long sum(long[] array){
		long cumulant=0;
		for(int i=0;i<array.length;i++)
			cumulant+=array[i];
		return cumulant;
	}

	public static final double tjD(double thetaw,double pi,double n) {
		if(thetaw==0)
			return 0;
		
		double a1 =0;
		double a2 =0;
		for (int i =1; i < n; i++) {
			a1 +=1.0 / i;
			a2 +=1.0 / (i * i);
		}
		
		//since thetaw is S/a1...
		double S=thetaw*a1;
		
		double b1 =(n + 1) / (3 * (n - 1));
		double b2 =2.0 * (n * n + n + 3) / (9 * n * (n - 1));

		double c1 =b1 - 1.0 / a1;
		double c2 =b2 - (n + 2) / (a1 * n) + a2 / (a1 * a1);

		double e1 =c1 / a1;
		double e2 =c2 / (a1 * a1 + a2);

		double sqr =e1 * S + e2 * S * (S - 1);
		//System.out.println(a1+" "+a2+" "+b1+" "+);
		if(!(sqr >= 0))//catches the case where sqr==nan
			return Double.NaN;
		assert (sqr >= 0):"TjD("+thetaw+","+pi+","+n+"):"+sqr;
		double denom =Math.sqrt(sqr);
		double neum =pi - S / a1;
		//if (Double.isNaN(neum / denom));
		//	return Do
		return neum / denom;
	}

	public final static void arrayAdd(int[] sumFreq, int[] freq) {
		for (int i =0; i < freq.length; i++) {
			sumFreq[i] +=freq[i];
		}

	}

	public final static double chiSqrDiff(int[] hackLast, int[] freq) {
		double acc =0;
		for (int i =0; i < freq.length; i++) {
			double del =hackLast[i] - freq[i];
			acc +=del * del;// /freq[i];
		}
		return acc / freq.length;

	}

	public static double freqMean(int[] freq) {
		double sum =0;
		double count =0;
		for (int i =0; i < freq.length; i++) {
			sum +=freq[i] * (i + 1);
			count +=freq[i];
		}
		return sum / count;
	}

	public static double freqMedian(int[] freq) {
		double count =0;
		for (int i =0; i < freq.length; i++)
			count +=freq[i];
		count =count / 2;
		for (int i =0; i < freq.length; i++) {
			if (count < freq[i])
				return i + 1;
			count -=freq[i];
		}
		return -1;
	}

	public static int freqMode(int[] freq) {
		int max =0;
		int maxIndex =-2;
		for (int i =0; i < freq.length; i++) {
			if (freq[i] > max) {
				max =freq[i];
				maxIndex =i;
			}
		}
		return maxIndex + 1;
	}

	public static double haromicNumber(int n) {
		double h=0;
		for(int i=1;i<=n;i++)
			h+=1.0/i;
		return h;
	}

	public static double max(int[] data) {
		int max=-Integer.MAX_VALUE;
		for(int i:data )
			max=Math.max(max, i);
		return max;
	}
	
	public static double distance(int[] a, int[] b) {
		double d =0;
		if (b == null || b.length != a.length)
			throw new RuntimeException("Wa?" + a.length + "\t" + b.length);// return
		// Double.MAX_VALUE;
		for (int i =0; i < a.length; i++) {
			d +=(a[i] - b[i]) * (a[i] - b[i]);
		}
		return Math.sqrt(d);
	}

	public static double distance(int[][][] a, int[][][] b) {
		double d =0;
		if (b == null || b.length != a.length)
			throw new RuntimeException("Wa?" + a + "\t" + b);

		for (int i =0; i < a.length; i++) {
			for (int j =0; j < a[i].length; j++) {
				for (int k =0; k < a[i][j].length; k++) {
					d +=(a[i][j][k] - b[i][j][k]) * (a[i][j][k] - b[i][j][k]);
				}
			}
		}
		return Math.sqrt(d);
	}

	
	

}
