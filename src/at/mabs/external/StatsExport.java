package at.mabs.external;

import at.mabs.cmdline.CmdLineParser;
import at.mabs.config.CommandLineMarshal;
import java.io.*;
import java.util.*;

/**
 * reads in next gen sequences from std in, spits out a stats thing.
 * 
 * @author bob
 * 
 */
public class StatsExport {
	public static void main(String[] args) {
		try {
			CommandLineMarshal clm=new CommandLineMarshal();
			CmdLineParser<CommandLineMarshal> parser =CommandLineMarshal.getCacheParser();
			parser.processArguments(args, clm);
			
			

			// hack has the filename we care about.
			String fileName=clm.HACK_PARAMS[0];
			BufferedReader br=new BufferedReader(new FileReader(fileName));
			
			HashMap<String,Integer> namesPlaces=new HashMap<String, Integer>();
			String line=br.readLine();
			while(line!=null){
				StringTokenizer st=new StringTokenizer(line);
				String name=st.nextToken();
				int place=Integer.parseInt(st.nextToken());
				namesPlaces.put(name, place);
				line=br.readLine();
			}
			//System.out.println(namesPlaces);
			//need to order as places. 
			
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
