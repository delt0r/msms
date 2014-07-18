package at.mabs.external;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.util.Util;

public class DistanceFilter {
	private ArrayList<Integer> simIndexBuild = new ArrayList<Integer>();
	private int[] simIndex;

	private ArrayList<Integer> dataIndexBuild = new ArrayList<Integer>();
	private int[] dataIndex;
	private List<double[]> dataList = new ArrayList<double[]>();
	private List<TreeSet<double[]>> bestOfList = new ArrayList<TreeSet<double[]>>();

	private int keep = 1000;

	private String datafileName;// null means first line of the input.
	private boolean isBin = false;

	@CLNames(names = { "-simRange", "-sr" })
	@CLDescription("A range in the simulation file that is used for distance calulations. Inclusive!")
	public void addSimRange(int start, int end) {
		for (int i = start - 1; i <= end - 1; i++) {
			simIndexBuild.add(i);
		}
	}

	@CLNames(names = { "-simIndex", "-si" })
	@CLDescription("A range in the simulation file that is used for distance calulations. Inclusive!")
	public void addSimIndex(int index) {
		simIndexBuild.add(index - 1);
	}

	@CLNames(names = { "-binarySim", "-bin" })
	@CLDescription("Simulation data is in binary format")
	public void setBin() {
		isBin = true;
	}

	@CLNames(names = { "-dataRange", "-dr" })
	@CLDescription("A range in the data file that is used for distance calulations. Inclusive!")
	public void addDataRange(int start, int end) {
		for (int i = start - 1; i <= end - 1; i++) {
			dataIndexBuild.add(i);
		}
	}

	@CLNames(names = { "-dataIndex", "-di" })
	@CLDescription("A range in the data file that is used for distance calulations. Inclusive!")
	public void addDataIndex(int index) {
		dataIndexBuild.add(index - 1);
	}

	@CLNames(names = { "-data", "-d", "-datafile", "-df" })
	public void dataFileName(String fileName) {
		datafileName = fileName;
	}

	@CLNames(names = { "-keep", "-k" })
	public void setKeep(int k) {
		keep = k;
	}

	public void run() {
		// first get source index
		simIndex = Util.toArrayPrimitiveInteger(simIndexBuild);
		if (dataIndexBuild.isEmpty()) {
			dataIndex = simIndex;
			System.err.println("WARNING: no data indices specified. Using source indices instead. This is probably wrong.");
		} else {
			dataIndex = Util.toArrayPrimitiveInteger(dataIndexBuild);
		}

		// data file.
		try {
			readData();
			initBestOf();
			// we are now data happy.
			if (isBin) {
				readProcessBinSim();
			} else {
				readProcessTextSim();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		print();

	}

	private void readProcessBinSim() throws IOException {
		DataInputStream dis = new DataInputStream(new BufferedInputStream(System.in));
		String line = dis.readUTF();
		StringTokenizer st = new StringTokenizer(line);
		st.nextElement();
		st.nextElement();
		int colCount = Integer.parseInt(st.nextToken());
		double[] simLine = new double[colCount];
		int endIndex = simLine.length - 1;
		try {
			while (true) {
				
				for (int i = 0; i < simLine.length; i++) {
					simLine[i] = dis.readDouble();
				}
				// now calulate distance..
				checkDistance(simLine);

			}
		} catch (EOFException eof) {
			// noop cus java is a little stupid.
		}
	}

	private void readProcessTextSim() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		StringTokenizer st = new StringTokenizer(line);

		double[] simLine = new double[st.countTokens()];
		int endIndex = simLine.length - 1;
		while (line != null) {
			st = new StringTokenizer(line);
			for (int i = 0; i < simLine.length; i++) {
				simLine[i] = Double.parseDouble(st.nextToken());
			}
			// now calulate distance..
			checkDistance(simLine);

			line = br.readLine();
		}
	}

	private void readData() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(datafileName)));
		String dataLine = br.readLine();
		while (dataLine != null) {

			ArrayList<String> tokens = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(dataLine, " \t,");
			while (st.hasMoreTokens()) {
				tokens.add(st.nextToken());
			}

			double[] data = new double[dataIndex.length];
			for (int i = 0; i < dataIndex.length; i++) {
				data[i] = Double.parseDouble(tokens.get(dataIndex[i]));
			}
			System.err.println("Using data elements:" + Arrays.toString(data));
			dataList.add(data);
			dataLine = br.readLine();
		}
		System.err.println("TotalDataCount:" + dataList.size());
	}

	private void initBestOf() {
		final Comparator distanceComp = new Comparator<double[]>() {
			@Override
			public int compare(double[] o1, double[] o2) {
				if (o1[o1.length - 1] > o2[o2.length - 1])
					return 1;
				return -1;
			}
		};

		for (int i = 0; i < dataList.size(); i++) {
			TreeSet<double[]> best = new TreeSet<double[]>(distanceComp);
			bestOfList.add(best);
		}
	}

	private void checkDistance(double[] simLine) {
		int endIndex = simLine.length - 1;
		for (int j = 0; j < dataList.size(); j++) {
			double[] data = dataList.get(j);
			TreeSet<double[]> bestOf = bestOfList.get(j);

			double d2 = 0;
			for (int i = 0; i < simIndex.length; i++) {
				double d = simLine[simIndex[i]] - data[i];
				d2 += d * d;
			}
			d2 = Math.sqrt(d2);
			simLine[endIndex] = d2;// insert into the end;
			if (bestOf.size() < keep || bestOf.last()[endIndex] > d2) {
				bestOf.add(simLine.clone());

				if (bestOf.size() > keep)
					bestOf.pollLast();
			}
		}

	}

	private void print() {
		for (TreeSet<double[]> bestOf : bestOfList) {
			for (double[] r : bestOf) {
				for (double d : r) {
					System.out.print(d + "\t");
				}
				System.out.println();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DistanceFilter df = new DistanceFilter();
		CmdLineParser<DistanceFilter> parser = null;
		try {
			parser = new CmdLineParser<DistanceFilter>(DistanceFilter.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println("ARGS:\n" + Arrays.toString(args));
			return;
		}

		try {
			parser.processArguments(args, df);
			df.run();
		} catch (Exception e) {
			System.err.println(parser.longUsage());
			System.out.println("ARGS:\n" + Arrays.toString(args));
			e.printStackTrace();

		}

	}

}
