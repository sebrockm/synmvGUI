
/**
 * This class is used to do and undo a shift of a SynmvJob
 * 
 * @author sebrockm
 *
 */
public class SynmvJobShiftAction extends SynmvJobAction {
	private SynmvJob shifted;
	private SynmvJob target;
	private int wayback;

	/**
	 * Creates a new SynmvJobShiftAction that shifts a job to another or undoes this action.
	 * @param shifted
	 * 			the job to be shifted
	 * @param target
	 * 			the target position
	 */
	public SynmvJobShiftAction(SynmvJob shifted, SynmvJob target) {
		super();
		this.shifted = shifted;
		this.target = target;
		wayback = 0;
	}

	@Override
	public void run() {
		super.run();
		wayback = -shifted.shiftTo(target);
	}
	
	@Override
	public void undo() {
		super.undo();
		for(int i = 0; i < wayback; i++) {
			shifted.swapWithNext();
		}
		for(int i = 0; i > wayback; i--) {
			shifted.swapWithPred();
		}
	}
}
