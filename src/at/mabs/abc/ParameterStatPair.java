package at.mabs.abc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import at.mabs.util.random.Random64;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * deals with filtering. That is we have box-cox transforms and transformations.
 * In particular we support S'=SB+B_0 Where B is whatever matrix you want.
 * 
 * @author bob
 * 
 */
public class ParameterStatPair implements Comparable<ParameterStatPair> {
	private double distance = Double.NaN;
	private final double[] parameters;
	private final double[] stats;
	private double[] transformedStats;

	public ParameterStatPair(double[] parameters, double[] statD) {

		this.parameters = parameters;
		this.stats = statD;
	}

	public ParameterStatPair(double[] parameters, double[] statD, double dis) {
		distance = dis;
		this.parameters = parameters;
		this.stats = statD;
	}

	@Override
	public int compareTo(ParameterStatPair o) {
		assert distance != Double.NaN;
		assert o.distance != Double.NaN;
		if (distance > o.distance)
			return 1;
		if (distance < o.distance)
			return -1;
		return -1;
	}

	public String toString() {
		String s = "";
		for (int i = 0; i < parameters.length; i++) {
			s += parameters[i] + "\t";
		}
		for (int i = 0; i < stats.length; i++) {
			s += stats[i] + "\t";
		}
		s += distance;
		return s;
	}

	public double[] getParameters() {
		return parameters;
	}

	public double[] getStats() {
		return stats;
	}

	public double[] getTransformedStats() {
		return transformedStats;
	}

	public double getDistance() {
		return distance;
	}

	public void transform(TransformData tdata){
		if (tdata != null) {
			transformedStats=tdata.transformStats(stats);
		} else {
			transformedStats = stats;
		}
	}
	
	public void calculateDistance( ParameterStatPair data,int truncate) {
		// if tdata null we set transfored==stat.
		

		double distance = 0;
		int mindex=Math.min(transformedStats.length, truncate);
		for (int i = 0; i < mindex; i++) {
			double delta = transformedStats[i] - data.transformedStats[i];
			distance += delta * delta;
		}
		//System.out.println("DIST:"+distance);
		this.distance = Math.sqrt(distance);
	}

	public static ParameterStatPair packIntoParamStat(double[] stats, List<PriorDensity> priors) {
		double[] params = new double[priors.size()];
		for (int i = 0; i < priors.size(); i++) {
			params[i] = priors.get(i).getValue();
		}
		ParameterStatPair psp = new ParameterStatPair(params, stats);
		return psp;
	}

	/**
	 * holds the required data for transformations. ie box-cox transforms and
	 * the like
	 * 
	 * @author bob
	 * 
	 */
	public static class TransformData {
		private double eps = .1;
		private double[] lambdas;// box cox lambdas. sorta..

		private Matrix b;// transform data.
		private Matrix b0;

		private TransformData(double[] l, Matrix b, Matrix b0) {
			this.lambdas = l;
			this.b = b;
			this.b0 = b0;
		}

		public double[] transformStats(double[] stats) {
			if (lambdas != null) {
				stats = boxcox(stats);
			}
			if (b == null)
				return stats;
			Matrix x = new Matrix(stats, 1);
			Matrix r=x.times(b);//.plus(b0);
			r.plusEquals(b0);
			return r.getColumnPackedCopy();
		}

		private double[] boxcox(double[] stats) {
			double[] trans = new double[stats.length];
			for (int i = 0; i < stats.length; i++) {
				double l = lambdas[i];
				if (Math.abs(l) < .1) {
					trans[i] = stats[i];
				} else {
					trans[i] = (Math.pow(stats[i], l) - 1) / l;
				}
			}
			return trans;
		}
	}

	public static TransformData calculateTransformData(List<ParameterStatPair> data, boolean boxCox, boolean pca, boolean pls) {
		if (data==null || data.size()==0)
			return null;

		double[] lams = null;
		if (boxCox) {
			//lams = calcBoxCoxLams(data);
		}
		Matrix b = null;
		Matrix b0 = null;
		
		int scount = data.get(0).stats.length;
		int pcount = data.get(0).parameters.length;
		Matrix x = new Matrix( data.size(),scount);
		Matrix y = new Matrix(data.size(),pcount);
		Matrix mux=new Matrix(1,scount);
		Matrix muy=new Matrix(1,pcount);
		for (int r = 0; r < data.size(); r++) {
			ParameterStatPair psp = data.get(r);
			for (int i = 0; i < scount; i++) {
				x.set(r, i, psp.stats[i]);
				mux.set(0, i, mux.get(0, i)+psp.stats[i]/data.size());
			}
			for (int i = 0; i < pcount; i++) {
				y.set(r,i, psp.parameters[i]);
				muy.set(0,i,muy.get(0,i)+psp.parameters[i]/data.size());
			}
		}
		for (int r = 0; r < data.size(); r++) {
			for (int i = 0; i < scount; i++) {
				x.set(r, i, x.get(r, i)-mux.get(0, i));
			}
			for (int i = 0; i < pcount; i++) {
				y.set(r,i, y.get(r, i)-muy.get(0, i));
				
			}
		}
		//System.out.println("PLS:"+pls);
		if (pca) {
			// create X..
			
			SingularValueDecomposition svd = x.svd();// could be expensive!
			// condition the singular values.
			double[] sig = svd.getSingularValues().clone();
			System.out.println("Singular Values:"+Arrays.toString(sig));
			Matrix diag = new Matrix(sig.length, sig.length);
			for (int i = 0; i < sig.length; i++) {
				if (sig[i] > 1e-6) {
					sig[i] = 1 / sig[i];
				} else {
					sig[i] = 0;
				}
				diag.set(i, i, sig[i]);
			}
			Matrix v = svd.getV();
			Matrix u = svd.getU();
			//printMatrix(muy);
			b = v.times(diag).times(u.transpose()).times(y);
			//printMatrix(b);
			b0 = muy.minus(mux.times(b));//new Matrix(1, pcount, 0);//muy.minus(mux.times(b));//
			System.out.println("intercepts:");
			printMatrix(b0);

		} else if (pls) {
			//System.out.println("PLS");
			//pls.. normals algo..
			Matrix e=x.copy();
			Matrix f=y.copy();
			
			Matrix W=Matrix.identity(scount,scount);
			Matrix T=Matrix.identity(scount,scount);
			Matrix P=Matrix.identity(scount,scount);
			Matrix Q=Matrix.identity(pcount,scount);
			
			for(int i=0;i<scount;i++){
				Matrix s=e.transpose().times(f);
				SingularValueDecomposition svd=s.svd();
				
				Matrix wc=pullCol(svd.getU(), 0);
				putCol(W, wc, i);
				
				Matrix t=e.times(wc);
				normalize(t);
				putCol(T, t, i);
				
				Matrix p=e.transpose().times(t);
				putCol(P, p, i);
				
				Matrix q=f.transpose().times(t);
				//System.out.println("q");
				//printMatrix(q);
				
				putCol(Q, q, i);
				
				//System.out.println("t");
				//printMatrix(T);
				
				e.minusEquals(t.times(p.transpose()));
				f.minusEquals(t.times(q.transpose()));
				//System.out.println("new e");
				//printMatrix(e);
				//System.out.println("\n\n");
				if(norm(e)<1e-6){
					break;
				}
			}
			Matrix pi=pinverse(P.transpose().times(W));
			b=W.times(pi).times(Q.transpose());
			b0 = muy.minus(mux.times(b));

			//System.out.println("B");
			//printMatrix(b);
		}
		return new TransformData(lams, b, b0);
	}
	
	private static Matrix pinverse(Matrix m){
		SingularValueDecomposition svd=m.svd();
		double[] d=svd.getSingularValues();
		Matrix diag=new Matrix(d.length,d.length);
		for(int i=0;i<d.length;i++){
			if(d[i]>1e-6){
				diag.set(i, i, 1/d[i]);
			}
		}
		return svd.getV().times(diag).times(svd.getU().transpose());
	}
	
	private static void normalize(Matrix m){
		
		m.timesEquals(1/norm(m));
	}
	
	private static double norm(Matrix  m){
		double norm=0;
		for(int r=0;r<m.getRowDimension();r++){
			for(int c=0;c<m.getColumnDimension();c++){
				double delta=m.get(r, c);
				norm+=delta*delta;
			}
		}
		return Math.sqrt(norm);
	}
	
	private static void putCol(Matrix a,Matrix col,int colIndex){
		for(int r=0;r<a.getRowDimension();r++){
			a.set(r, colIndex, col.get(r, 0));
		}
	}
	
	private static Matrix pullCol(Matrix a,int colIndex){
		Matrix col=new Matrix(a.getRowDimension(),1);
		for(int r=0;r<a.getRowDimension();r++){
			col.set(r, 0, a.get(r, colIndex));
		}
		return col;
	}
	

	private static void printMatrix(Matrix m) {
		System.out.println("Matrix, rows " + m.getRowDimension() + "\tcols " + m.getColumnDimension());
		for (int r = 0; r < m.getRowDimension(); r++) {
			for (int c = 0; c < m.getColumnDimension(); c++) {
				System.out.print(m.get(r, c)+"\t");
			}
			System.out.println();
		}
		System.out.println();
	}

	private static double[] calcBoxCoxLams(List<ParameterStatPair> data) {
		double[] lams = new double[data.get(0).stats.length];
		double eps = .1;
		double[] lams2 = new double[lams.length];
		for (int i = 0; i < lams.length; i++) {
			// find the most negative.. add a little bit to it.
			double min = 0;
			for (ParameterStatPair psp : data) {
				min = Math.min(min, psp.stats[i]);
			}
			lams2[i] = -min + eps;
			// start with a lam of -2 stop when the fix is never better...
			double lastSSE = Double.POSITIVE_INFINITY;
			double lastL = Double.NaN;
			for (double l = -2; l <= 2; l += .1) {
				// if(Math.abs(l)<eps){
				// l=eps;
				// }
				double sse = 0;
				for (ParameterStatPair psp : data) {
					double delta = psp.stats[i] - (Math.pow(psp.stats[i] + lams2[i], l) - 1) / l;
					sse = delta * delta;
				}
				System.out.println("SSE:" + sse + "\t" + l);
				if (sse > lastSSE) {
					System.out.println("\n\n");
					break;

				} else {
					lastSSE = sse;
					lastL = l;
				}
			}
			lams[i] = lastL;
		}
		Arrays.fill(lams, 2);
		return lams;
	}

	public static void main(String[] args) {
		// just use a exp distribution for a box cox test.
		List<ParameterStatPair> data = new ArrayList<ParameterStatPair>();
		Random rand = new Random64();
		for (int i = 0; i < 80; i++) {
			double a = rand.nextDouble() * 10 - 5;
			double b = rand.nextDouble() * 10 - 5;
			// now for the stats.
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			double sum = 0;
			double sum2 = 0;
			for (int j = 0; j < 10; j++) {
				double v = rand.nextGaussian() * a + b;
				max = Math.max(max, v);
				min = Math.min(min, v);
				sum += v;
				sum2 += v * v;
			}
			data.add(new ParameterStatPair(new double[] { a, b }, new double[] { Math.exp(a*b),a*b, (a+b)*1e-6, a-b, b-a }));
		}

		TransformData td = calculateTransformData(data, false, false, true);
		for(ParameterStatPair psp:data){
			psp.transform(td);
			System.out.println(Arrays.toString(psp.getTransformedStats())+"\t"+Arrays.toString(psp.parameters));
		}
	}

}