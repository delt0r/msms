package at.mabs.external;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

/**
 * get the mode estimator from files. The last column is the "sorted" one (tab
 * delimited).
 * 
 * @author greg
 * 
 */
public class ModeWork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			int n = Integer.parseInt(args[0]);
			String filename = args[1];
			int count = Integer.parseInt(args[2]);
			int col= Integer.parseInt(args[3]);
			
			ArrayList<Double> data=new ArrayList<Double>();
			
			for (int i = 1; i <= count; i++) {
				String thisFileName = filename + i;
				BufferedReader reader = new BufferedReader(new FileReader(thisFileName));
				String line=reader.readLine();
				data.clear();
				while(line!=null){
					data.add(value(line,col));
					line=reader.readLine();
				}
				Collections.sort(data);
				double mode=Double.NaN;
				double dt=Double.MAX_VALUE;
				for(int j=0;j<data.size()-n;j++){
					double d=data.get(j+n-1)-data.get(j);
					if(Math.abs(d)<dt){
						dt=Math.abs(d);
						mode=(data.get(j+n-1)+data.get(j))/2;
					}
				}
				reader.close();
				System.out.println(mode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static double value(String line,int col){
		StringTokenizer st=new StringTokenizer(line);
		String last=null;
		for(int i=0;i<col;i++)
			last=st.nextToken();
		return Double.parseDouble(last);
		
		
	}

}
