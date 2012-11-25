package at.mabs.util.random;

import cern.jet.random.StudentT;

/**
 * another wrapper for the colt stuff, ie a way to flag the cdf method. The api was never quite finished. At least that is what it looks like. 
 * @author bob
 *
 */
public interface CDF {
	/**
	 * Cumulative distribution function. Must always return valid numbers on the interval (0,1). 
	 * @param v
	 * @return a number between 0 and 1.
	 */
	public double cdf(double v);
	
	public static final class StudentTcdf implements CDF{
		private StudentT studentT;
		
		public StudentTcdf(StudentT t) {
			studentT=t;
		}
		
		public void dof(double dof){
			studentT.setState(dof);
		}
		
		@Override
		public double cdf(double v) {
		
			return studentT.cdf(v);
		}
	}
}
