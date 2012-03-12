package at.mabs.abc;

public interface Kernal {
	public void setBandwidth(double bw);
	public double getBandwidth();
	public double kernal(double x);
	
	public static class Gaussian implements Kernal{
		private static final double ONE_OVER_SQRTPI=1.0/Math.sqrt(2*Math.PI);
		private double sigma=1;
		
		@Override
		public double getBandwidth() {
		
			return sigma;
		}
		@Override
		public void setBandwidth(double bw) {
			sigma=bw;
		}
		
		@Override
		public double kernal(double x) {
			
			return Math.exp(-(x*x)/(2*sigma*sigma))*ONE_OVER_SQRTPI/sigma;
		}
		
		
	}
	
	public static class Lorentzian implements Kernal{
		
		private double bandwidth;
		
		@Override
		public double getBandwidth() {
		
			return bandwidth;
		}
		@Override
		public void setBandwidth(double bw) {
			bandwidth=bw;
		}
		
		@Override
		public double kernal(double x) {
			x=x/bandwidth;
			return 1.0/(Math.PI*bandwidth*(1+x*x));
		}
	}
	
	
}
