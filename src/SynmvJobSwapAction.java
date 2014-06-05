/**
 * 
 */

/**
 * This class is used to do and undo a swap of two SynmvJobs.
 * 
 * @author sebrockm
 *
 */
public class SynmvJobSwapAction extends SynmvJobAction {
	private SynmvJob j1;
	private SynmvJob j2;

	/**
	 * Creates a new SynmvJobSwapAction that swaps or reswaps two jobs.
	 * 
	 * @param j1
	 * 			first SynmvJob to swap
	 * @param j2
	 * 			swap partner of j1
	 */
	public SynmvJobSwapAction(SynmvJob j1, SynmvJob j2) {
		super();
		this.j1 = j1;
		this.j2 = j2;
	}

	@Override
	public void run() {
		super.run();
		j1.swapWith(j2);
		
	}

	@Override
	public void undo() {
		super.undo();
		j2.swapWith(j1);
	}

}
