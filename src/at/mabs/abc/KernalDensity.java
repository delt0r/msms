package at.mabs.abc;

import java.util.Arrays;

/**
 * simple and crude.
 * 
 * @author bob
 * 
 */
public class KernalDensity {
	private double bandwidth;
	private double[] data =new double[16];
	private int size;
	private Kernal kernal;
	
	public KernalDensity(Kernal k) {
		kernal=k;
	}

	public void setBandwidth(double bandwidth) {
		assert bandwidth > 0;
		//System.out.println("SetBW:" + bandwidth + "\t" + this.getLast()+"\t"+this.getFirst());
		this.bandwidth =bandwidth;
	}

	public double getBandwidth() {
		return bandwidth;
	}

	public double kernalDensity(double p) {
		double kde =0;
		kernal.setBandwidth(bandwidth);
		int n=size;
		for (int i =0; i < size; i++) {
			kde+=kernal.kernal(data[i]);
		}
		return kde/n;
	}
	
	public double mean(){
		double s=0;
		for (int i =0; i < size; i++) {
			s+=data[i];
		}
		return s/size;
	}
	
	public double get(int i){
		assert i<size;
		return data[i];
	}

	public void addData(double d) {
		if(Double.isNaN(d)){
			System.out.println("NaN data");
			return;
		}
		if (data.length == size) {
			resize();
		}
		data[size] =d;
		size++;
	}

	public void clear() {
		size =0;
		//bandwidth=0;
		Arrays.fill(data, 0);
	}

	public int size() {
		return size;
	}

	public void sort() {
		assert size > 0;
		Arrays.sort(data, 0, size);
	}

	public double getLast() {
		assert size > 0;
		return data[size - 1];
	}

	public double getFirst() {
		assert size > 0;
		return data[0];
	}

	private void resize() {
		double[] newData =new double[data.length * 2];
		System.arraycopy(data, 0, newData, 0, data.length);
		data =newData;
	}
	
	
}
