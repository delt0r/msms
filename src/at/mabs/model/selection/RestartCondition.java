package at.mabs.model.selection;

public interface RestartCondition {
	/**
	 * 
	 * @param frequencys
	 * @param demeCount
	 * @return true if we restart.
	 */
	public boolean isMeet(double[] frequencys, int demeCount);

	public static class Default implements RestartCondition {
		
		public Default() {
			
		}
		
		public boolean isMeet(double[] frequencys, int demeCount) {
			for (int i=0;i<demeCount;i++) {
				double f=frequencys[i];
				if (f > 0)
					return false;
			}
			return true;
		}
	}
}