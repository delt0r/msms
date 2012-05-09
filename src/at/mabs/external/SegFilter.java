package at.mabs.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
/**
 * for the 1001 data. No idea if the format is common. but its sorta like this. 
 * 
 * <code>
 * 345	 N A A A
 * </code>
 * ie postion then data for each sample. We want to filter everything that is not segregating. 
 */
public class SegFilter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
			String line=reader.readLine();
			while(line!=null){
				StringTokenizer st=new StringTokenizer(line);
				st.nextToken();//dump the postion;
				String value=st.nextToken();//get the next value...
				while(st.hasMoreTokens()){
					if(!value.equals(st.nextToken())){
						System.out.println(line);
						break;
					}
				}
				line=reader.readLine();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
