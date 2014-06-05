/**
 * 
 */

/**
 * This abstract class is used to do and undo an action an SynmvJobs, 
 * like swapping or shifting them.
 * 
 * @author sebrockm
 *
 */
public abstract class SynmvJobAction implements Runnable {
	
	/**
	 * Indicates whether the action can be done or undone.
	 */
	private boolean done;
	
	/**
	 * Creates an undone SynmvJobAction.
	 */
	public SynmvJobAction() {
		done = false;
	}
	
	/**
	 * Performs the action, if it has not been performed already.
	 * Subclasses must override this method and call super.run() first.
	 */
	@Override
	public void run() {
		if(!done) {
			done = true;
		}
		else {
			throw new RuntimeException("A done SynmvJobAction cannot be done again.");
		}
	}
	
	/**
	 * Performs the undo of the action, if it has not been undone already.
	 * Subclasses must override this method and call super.undo() first.
	 */
	public void undo() {
		if(done) {
			done = false;
		}
		else {
			throw new RuntimeException("An undone SynmvJobSwapAction cannot be undone (again).");
		}
	}
}
