package at.mabs.stats;

/**
 * just for managing x,y type functions and a few operations over them.
 * 
 * @author greg
 * 
 */
public class Density {
	private double[] x;
	private double[] y;
	private int index;

	private Density(){
		//serilization
	}
	
	public Density(int size) {
		x = new double[size];
		y = new double[size];
		index = 0;
	}

	public void add(double x, double y) {
		this.x[index] = x;
		this.y[index] = y;
		index++;
	}

	public double getX(int i) {
		return x[i];
	}

	public double getY(int i) {
		return y[i];
	}

	public int size() {
		return x.length;
	}

	private void resetIndex() {
		index = 0;
	}

	public double getY(double d) {
		//index=0;
		while (index < x.length - 1 && x[index + 1] < d) {
			index++;
		}
		while (index>0 && x[index] > d) {
			index--;
		}

		double x1 = x[index];
		double x2 = x[index + 1];
		if (x2 < d || x1 > d)
			return 0;
		double t = (x2-d ) / (x2 - x1);
		return y[index] * t + (1 - t) * y[index + 1];
	}

	public void toString(StringBuilder builder){
		for(int i=0;i<x.length;i++)
			builder.append(x[i]+"\t"+y[i]+"\n");
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		toString(sb);
		return sb.toString();
	}
	
	private int getIndex() {
		return index;
	}

	public static double norm(Density a, Density b) {
		a.resetIndex();
		b.resetIndex();
		int ia = 0;
		int ib = 0;
		
		double sum = 0;
		
		while(ia<a.size()-1 && ib<b.size()-1){
			double startx=Math.max(a.getX(ia), b.getX(ib));
			double endx=Math.min(a.getX(ia+1), b.getX(ib+1));
			if(endx>startx){
				sum+=(endx-startx)*(a.getY(startx)-b.getY(startx)+a.getY(endx)-b.getY(endx))/2;
			}
			if(endx==a.getX(ia+1)){
				ia++;
			}else{
				ib++;
			}
		}

		return sum;
	}

	public static void main(String[] args) {
		double[] x1 = { 0,5,6,7, 12 };
		double[] x2 = { -30,-20,0,11,15 };
		Density a = new Density(x1.length);
		Density b = new Density(x2.length);
		for (int i = 0; i < x1.length; i++) {
			a.add(x1[i], x1[i]+1);
			b.add(x2[i], x2[i]*-1);
		}
		//System.out.println(a.getY(2.0)+"\t"+b.getY(2.0));
		System.out.println(norm(a, b));
	}
}
