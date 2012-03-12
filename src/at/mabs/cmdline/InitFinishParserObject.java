package at.mabs.cmdline;

public interface InitFinishParserObject {
	/**
	 * called before parsing
	 */
	public void init();
	
	/**
	 * called when finished.
	 */
	public void finished();
}
