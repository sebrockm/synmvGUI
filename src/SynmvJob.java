import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Deque;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;


/**
 * This class represents one job.
 * It contains the java.swing components that are used to display the job 
 * as well as the internal logic that is used to calculate e.g. the positions.
 * 
 * @author sebrockm
 *
 */
public class SynmvJob {
	
	/**
	 * Enumeration for the different variants.
	 * @author sebrockm
	 *
	 */
	public static enum Variant {
		synchronous,
		asynchronous,
		noWait,
		blocking
	}

	/**
	 * Deque that stores all done actions to be able to undo them.
	 */
	public static final Deque<SynmvJobAction> actionList = new LinkedList<SynmvJobAction>();
	
	/**
	 * Deque that stores all undone actions to be able to redo them.
	 */
	public static final Deque<SynmvJobAction> undoneActionList = new LinkedList<SynmvJobAction>();
	
	/**
	 * References the chosen instance or null if none is chosen.
	 */
	public static SynmvJob chosen = null;
	
	/**
	 * References the instance the mouse is currently over.
	 */
	public static SynmvJob mouseOver = null;

	/**
	 * The initial factor the job's times are multiplied with
	 * to calculate the time field's horizontal size in pixels.
	 */
	public static final float FACTOR = 10;
	
	/**
	 * The initial height of the times in pixels.
	 */
	public static final int HEIGHT = 60;
	
	/**
	 * The current factor the job's times are multiplied with
	 * to calculate the time field's horizontal size in pixels.
	 */
	public static float factor = FACTOR;
	
	/**
	 * The offset in pixels from the frame's left border to the first
	 * job's first time field.
	 */
	public static int xOffset = 50;
	
	/**
	 * The offset in pixels from the frame's top border to the top
	 * time fields of the jobs.
	 */
	public static int yOffset = 80;
	
	/**
	 * Corresponds to the continuous shift JCheckbox.
	 */
	public static boolean continuousShift = true;
	
	/**
	 * Corresponds to the variants JRadioButtons.
	 */
	public static Variant variant = Variant.synchronous;
	
	/**
	 * Indicates whether the jobs have due dates.
	 */
	public static boolean hasDuedates = false;
	
	/**
	 * Indicates whether the jobs have weights.
	 */
	public static boolean hasWeights = false;
	
	/**
	 * This Runnable is used as a callback that is invoked every time
	 * the layout of the SynmvJobs among each other changes.
	 * It shall calculate their new positions.
	 */
	public static Runnable callback;
	
	
	/**
	 * The job's id.
	 */
	private final int id;
	
	/**
	 * An array of process times the machines need to process the job.
	 * times[0] belongs to machine 1 etc.
	 */
	private final float[] times;
	
	/**
	 * The job's due date. A negative due date means the job has none.
	 */
	private float duedate;
	
	/**
	 * The job's weight. A weight of 1 is default.
	 */
	private float weight;
	
	/**
	 * The JPanel the job will be displayed in.
	 */
	private final JPanel parent;
	
	/**
	 * An array of JLabels each graphically representing one process time.
	 */
	private final JLabel[] slots;
	
	/**
	 * JTextFields that are placed on the slots to enter a new process time.
	 */
	private final JTextField[] textFields;
	
	/**
	 * A JLabel showing the number (position) in the current schedule.
	 * It is displayed right above the first slot.
	 */
	private final JLabel number = new JLabel();
	
	/**
	 * An info box that appears on double click on a job.
	 */
	private final JFrame infobox = new JFrame("info");
	
	/**
	 * A JLabel displaying the job's id in the info box.
	 */
	private final JLabel idLabel = new JLabel();
	
	/**
	 * A JTextField used to display the job's current position in the info box.
	 * It can be used to enter a new position the job shall shift to or swap with.
	 */
	private final JTextField cycleField = new JTextField();
	
	/**
	 * JTextFields that display the job's process times in the info box.
	 * They also allow the user to change those times by entering new ones.
	 */
	private final JTextField[] infoTimeFields;
	
	/**
	 * A JLabel in the info box that displays the current start time of the job.
	 */
	private final JLabel startTimeLabel = new JLabel();
	
	/**
	 * A JLabel in the info box that displays the current time the job will be done.
	 */
	private final JLabel endTimeLabel = new JLabel();
	
	/**
	 * A JTextField in the info box that displays the due date of the job or
	 * '-' if it does not have one. It can be used to enter a new due date.
	 */
	private final JTextField duedateField = new JTextField();
	
	/**
	 * A JTextField in the info box that displays the weight of the job or
	 * '1' if it does not have one. It can be used to enter a new weight.
	 */
	private final JTextField weightField = new JTextField();
	
	/**
	 * A reference to the job's predecessor in the current schedule or null if
	 * this job is the first one.
	 */
	private SynmvJob pred;
	
	/**
	 * A reference to the job's follower in the current schedule or null if
	 * this job is the last one.
	 */
	private SynmvJob next;
	
	/**
	 * true, when the job is being moved and the mouse button is still pressed.
	 */
	private boolean mouseHold = false;

	
	/**
	 * Calculates the the time this job will be on a machine in the current
	 * schedule depending on the predecessors' and followers' times on their
	 * machines.
	 * 
	 * @param machine
	 *            the machine number starting with 0
	 * @return the cycle time of this job on a machine in the current schedule
	 */
	private float maxLen(int machine) {
		//search first relevant
		SynmvJob tmp = this;
		int futurePos;
		for(futurePos = 0; futurePos < machine; futurePos++) {
			if(tmp.next == null) {
				break;
			}
			tmp = tmp.next;
		}
	
		//find max
		float len = 0;
		for(int i = machine-futurePos; i < times.length; i++) {
			len = Math.max(len, tmp.getTime(i));
			if(tmp.pred == null) {
				break;
			}
			tmp = tmp.pred;
		}
		
		return len;
	}
	
	/**
	 * Calculates the offset of this job on a machine. This is the time when
	 * that machine begins to process this job in the current schedule.
	 * This method does care about the variants flag which results in a
	 * synchronous, asynchronous, no-wait or blocking calculation.
	 * 
	 * @param machine
	 * 			the machine number starting with 0
	 * @return the offset of this job on a machine
	 */
	public float getOffset(int machine) {
		if(machine < 0 || machine >= getMachineCount()) {
			throw new IllegalArgumentException("'machine' must be in [0,getMachineCount()[");
		}
		
		switch(SynmvJob.variant) {
		case synchronous:
			if(pred == null) {
				if(machine == 0) {
					return 0;
				}
				return getOffset(machine-1) + maxLen(machine-1);
			}
			return pred.getOffset(machine) + pred.maxLen(machine);
			
		case asynchronous:
			if(pred == null && machine == 0) {
				return 0;
			}
			if(pred == null) {
				return getOffset(machine-1) + getTime(machine-1);
			}
			if(machine == 0) {
				return pred.getOffset(machine) + pred.getTime(machine);
			}
			return Math.max(getOffset(machine-1) + getTime(machine-1), pred.getOffset(machine) + pred.getTime(machine));
			
		case noWait:
			if(machine == 0) {
				return getNoWaitOffset();
			}
			return getOffset(machine-1) + getTime(machine-1);
			
		case blocking:
			break;
		default:
			throw new RuntimeException("unknown variant, this cannot happen...");
		}
		
		return 0;
	}
	
	/**
	 * Calculates the offset of the first job when the no-wait variant is selected.
	 * @return no-wait offset
	 */
	private float getNoWaitOffset() {
		if(pred == null) {
			return 0;
		}
		
		float predSum = pred.getTime(0);
		float thisSum = 0;
		float offset = predSum;
		for(int i = 1; i < getMachineCount(); i++) {
			predSum += pred.getTime(i);
			thisSum += getTime(i-1);
			offset = Math.max(offset, predSum - thisSum);
		}
		
		return offset + pred.getNoWaitOffset();
	}
	
	/**
	 * 
	 * @return the number of machines the job will be processed on
	 */
	public int getMachineCount() {
		return slots.length;
	}
	
	/**
	 * This method sets the locations, sizes and texts (if any) 
	 * of all visible components of this job.
	 * It also actualizes the info box, if it is visible, and it resizes
	 * the parent container if necessary.
	 */
	public void setLocations() {
		for(int i = 0; i < slots.length; i++) {
			slots[i].setSize(Math.max(10, (int)Math.ceil(factor * getTime(i))), HEIGHT);	
			slots[i].setLocation(xOffset+(int)(factor * getOffset(i)), yOffset+i*HEIGHT);
			
			int y = (slots[i].getSize().height - textFields[i].getSize().height) / 2;
			int x = (slots[i].getWidth() - textFields[i].getWidth()) / 2;
			textFields[i].setLocation(x, y);
		}
			
		number.setSize(slots[0].getSize());
		Point loc = slots[0].getLocation();
		loc.y -= number.getSize().height;
		number.setLocation(loc);
		number.setText("" + (countPredecessors()+1));
	
		if(infobox.isVisible()) {
			showInfobox();
		}
		
		//size of parent depends on last job
		if(next == null) {
			parent.setSize(slots[slots.length-1].getSize().width + slots[slots.length-1].getLocation().x, 
					slots.length * HEIGHT + yOffset);
			parent.setPreferredSize(parent.getSize());
		}
	}

	/**
	 * Returns the process time of this job on a machine.
	 * 
	 * @param machine
	 * 			machine index beginning with 0
	 * @return process time
	 */
	public float getTime(int machine) {
		return times[machine];
	}
	
	/**
	 * Creates a new SynmvJob without due date.
	 * 
	 * @param container
	 * 			the parent container
	 * @param id
	 * 			the job's id
	 * @param times
	 * 			an array of process times
	 */
	public SynmvJob(final JPanel container, int id, float[] times) {
		this(container, id, times, -1.f, 1.f);
	}
	
	/**
	 * Initializes the JTextFields on the slots. Sets initial texts and 
	 * adds ActionListeners and DocumentListeners.
	 */
	private void initTextFields() {
		for(int i = 0; i < textFields.length; i++) {
			textFields[i].setVisible(false);
			textFields[i].setHorizontalAlignment(SwingConstants.CENTER);
			textFields[i].setFont(slots[i].getFont());
			
			final int ii = i;
			textFields[i].addActionListener(new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					float t;
					try {
						t = Float.parseFloat(textFields[ii].getText());
					} catch (NumberFormatException e) {
						t = SynmvJob.this.times[ii];
					}
					
					SynmvJob.this.times[ii] = t;
					SynmvJob.this.slots[ii].setText("" + t);
					runCallback();
				}
			});
				
			final FontMetrics metric = textFields[i].getFontMetrics(textFields[i].getFont());
			textFields[i].setSize(metric.stringWidth("" + times[i]) + 5, HEIGHT/3);
			textFields[i].getDocument().addDocumentListener(new DocumentListener() {	
				private void update(DocumentEvent e) {
					int width;
					try {
						int oldw = metric.stringWidth(slots[ii].getText());
						int neww = metric.stringWidth(textFields[ii].getDocument().getText(0, textFields[ii].getDocument().getLength()));
						width = Math.max(oldw, neww);
					} catch (BadLocationException e1) {
						width = slots[ii].getWidth();
					}
					width += 5;
					textFields[ii].setSize(width, textFields[ii].getHeight());
					textFields[ii].setLocation((slots[ii].getWidth()-width)/2, textFields[ii].getY());
				}			
				@Override
				public void removeUpdate(DocumentEvent e) {
					update(e);
				}			
				@Override
				public void insertUpdate(DocumentEvent e) {
					update(e);
				}			
				@Override
				public void changedUpdate(DocumentEvent e) {
					update(e);
				}
			});
		}
	}
	
	/**
	 * Initializes the slots. Sets initial texts and colors and adds the
	 * textFields and MouseListeners.
	 */
	private void initSlots() {
		setDefaultColor();
		for(int i = 0; i < slots.length; i++) {
			slots[i].setText(textFields[i].getText());
			slots[i].setVisible(true);
			slots[i].setOpaque(true);
			slots[i].setBorder(new LineBorder(Color.DARK_GRAY));
			slots[i].setHorizontalAlignment(SwingConstants.CENTER);
				
			slots[i].add(textFields[i]);
			
			slots[i].addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON3 && chosen != null) {
						SynmvJobSwapAction action = new SynmvJobSwapAction(chosen, SynmvJob.this);
						action.run();
						actionList.addFirst(action);
						undoneActionList.clear();
					}
					
					if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
						showInfobox();
					}
				}
	
				@Override
				public void mouseEntered(MouseEvent e) {
					mouseOver = SynmvJob.this;
					for(JLabel slot: slots) {
						slot.setBorder(new LineBorder(Color.RED));
					}
					
					if(SynmvJob.continuousShift) {
						if(chosen != null && chosen.mouseHold && chosen != SynmvJob.this) {
							SynmvJobShiftAction action = new SynmvJobShiftAction(chosen, SynmvJob.this);
							action.run();
							actionList.addFirst(action);
							undoneActionList.clear();
						}
					}
				}
	
				@Override
				public void mouseExited(MouseEvent e) {
					if(mouseOver == SynmvJob.this) {
						mouseOver = null;
					}
					for(JLabel slot: slots) {
						slot.setBorder(new LineBorder(Color.DARK_GRAY));
					}
				}
	
				@Override
				public void mousePressed(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) {
						mouseHold = true;
						if(chosen != null) {
							chosen.highlight(Color.GRAY);
							for(JTextField field : chosen.textFields) {
								field.setVisible(false);
							}
						}
						chosen = SynmvJob.this;
						chosen.highlight(Color.RED);				
						for(JTextField field : textFields) {
							field.setVisible(true);
						}
					}
				}
	
				@Override
				public void mouseReleased(MouseEvent e) {
					if(!SynmvJob.continuousShift) {
						if(chosen != null && chosen.mouseHold && mouseOver != null) {
							SynmvJobShiftAction action = new SynmvJobShiftAction(chosen, mouseOver);
							action.run();
							actionList.addFirst(action);
							undoneActionList.clear();
						}
					}
					chosen.mouseHold = false;
					mouseHold = false;
					runCallback();
				}
			});
		}
	}
	
	/**
	 * Initializes the info box and all of its components.
	 * Sets the box's layout and adds ActionListeners to the
	 * components.
	 */
	private void initInfobox() {
		infobox.setVisible(false);
		
		int rows = 6 + times.length;
		infobox.setLayout(new GridLayout(rows, 2));
		
		infobox.add(new JLabel("id "));
		idLabel.setText("" + id);
		infobox.add(idLabel);
		
		infobox.add(new JLabel("cycle "));
		JPanel container = new JPanel();
		container.setLayout(new GridLayout(1, 3));
		container.add(cycleField);
		JButton shift = new JButton("shift");
		JButton swap = new JButton("swap");
		container.add(shift);
		container.add(swap);
		infobox.add(container);
		
		shift.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				int pos;
				try {
					pos = Integer.parseInt(cycleField.getText());
				} catch (NumberFormatException ex) {
					cycleField.setText("" + (countPredecessors()+1));
					return;
				}
				
				if(pos < 1 || pos > countPredecessors() + 1 + countFollowers()) {
					cycleField.setText("" + (countPredecessors()+1));
					return;
				}
				
				int dir = pos - countPredecessors() - 1;
				SynmvJob shiftTo = getNthNext(dir);
				SynmvJobShiftAction action = new SynmvJobShiftAction(SynmvJob.this, shiftTo);
				action.run();
				actionList.addFirst(action);
				undoneActionList.clear();
			}
		});
		
		swap.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int pos;
				try {
					pos = Integer.parseInt(cycleField.getText());
				} catch (NumberFormatException ex) {
					cycleField.setText("" + (countPredecessors()+1));
					return;
				}
				
				if(pos < 1 || pos > countPredecessors() + 1 + countFollowers()) {
					cycleField.setText("" + (countPredecessors()+1));
					return;
				}
				
				SynmvJob tmp = getFirstPredecessor();
				for(int i = 1; i < pos; i++) {
					tmp = tmp.getNext();
				}
				
				SynmvJobSwapAction action = new SynmvJobSwapAction(SynmvJob.this, tmp);
				action.run();
				actionList.addFirst(action);
				undoneActionList.clear();
			}
		});
				
		for(int i = 0; i < times.length; i++) {
			infobox.add(new JLabel("time " + (i+1) + " "));
			infoTimeFields[i] = new JTextField("" + times[i]);
			infobox.add(infoTimeFields[i]);
			final int ii = i;
			infoTimeFields[i].addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					float t;
					try {
						t = Float.parseFloat(infoTimeFields[ii].getText());
					} catch (NumberFormatException e) {
						infoTimeFields[ii].setText("" + times[ii]);
						return;
					}
					
					times[ii] = t;
					slots[ii].setText("" + t);
					textFields[ii].setText(slots[ii].getText());
					runCallback();
				}
			});
		}
		
		infobox.add(new JLabel("start time "));
		infobox.add(startTimeLabel);
		
		infobox.add(new JLabel("end time "));
		infobox.add(endTimeLabel);

		infobox.add(new JLabel("duedate "));
		infobox.add(duedateField);
		duedateField.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				float t;
				try {
					t = Float.parseFloat(duedateField.getText());
				} catch (NumberFormatException ex) {
					duedateField.setText("" + (duedate < 0.f ? "-" : duedate));
					return;
				}
				
				duedate = t;
				runCallback();
			}
		});
		
		infobox.add(new JLabel("weight "));
		infobox.add(weightField);
		weightField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				float t;
				try {
					t = Float.parseFloat(weightField.getText());
				} catch (NumberFormatException ex) {
					weightField.setText("" + weight);
					return;
				}
				
				weight = t;
				runCallback();
			}
		});
	}
	
	/**
	 * Shows the info box, if it has been invisible, and actualizes its JLabels,
	 * JTextFields and layout.
	 */
	private void showInfobox() {
		infobox.setVisible(true);
		
		cycleField.setText("" + (countPredecessors()+1));
		for(int i = 0; i < times.length; i++) {
			infoTimeFields[i].setText("" + times[i]);
		}
		startTimeLabel.setText("" + getStartTime());
		endTimeLabel.setText("" + getEndTime());
		duedateField.setText("" + (duedate < 0.f ? "-" : duedate));
		weightField.setText("" + weight);
		
		infobox.pack();
	}
	
	/**
	 * Creates a new SynmvJob with due date.
	 * 
	 * @param container
	 * 			the parent container
	 * @param id
	 * 			the job's id
	 * @param times
	 * 			an array of the job's process times
	 * @param duedate
	 * 			the due date, a negative due date means there is none
	 * @param weight
	 * 		the weight
	 */
	public SynmvJob(final JPanel container, int id, float[] times, float duedate, float weight) {
		this.id = id;
		this.parent = container;
		this.times = times;
		this.duedate = duedate;
		this.weight = weight;
		this.number.setVisible(true);
		this.number.setHorizontalAlignment(SwingConstants.CENTER);
		
		infoTimeFields = new JTextField[times.length];
		slots = new JLabel[times.length];
		textFields = new JTextField[times.length];
		
		for(int i = 0; i < textFields.length; i++) {
			textFields[i] = new JTextField("" + times[i]);
			slots[i] = new JLabel();
		}
		
		initTextFields();
		initSlots();
		setLocations();
		initInfobox();
	}
	
	/**
	 * 
	 * @return the job's id
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * 
	 * @return the job's due date
	 */
	public float getDuedate() {
		return duedate;
	}
	
	/**
	 * 
	 * @param duedate
	 * 		new due date
	 */
	public void setDuedate(float duedate) {
		this.duedate = duedate;
	}
	
	/**
	 * 
	 * @return the job's weight
	 */
	public float getWeight() {
		return weight;
	}
	
	/**
	 * 
	 * @param weight
	 * 		new weight
	 */
	public void setWeight(float weight) {
		this.weight = weight;
	}
	
	/**
	 * Sets a new predecessor and invokes the callback.
	 * 
	 * @param pred
	 * 			new predecessor
	 */
	public void setPred(SynmvJob pred) {
		this.pred = pred;
		runCallback();
	}
	
	/**
	 * 
	 * @return the job's predecessor or null, if this job is the first one
	 */
	public SynmvJob getPred() {
		return pred;
	}
	
	/**
	 * Returns the nth predecessor of this job or null if there are less predecessors than n.
	 * @param n
	 * 		number of predecessors
	 * @return the nth predecessor or null
	 */
	public SynmvJob getNthPred(int n) {
		SynmvJob tmp = this;
		for(int i = 0; i < n && tmp != null; i++) {
			tmp = tmp.pred;
		}
		for(int i = 0; i > n && tmp != null; i++) {
			tmp = tmp.next;
		}
		return tmp;
	}
	
	/**
	 * Sets a new follower and invokes the callback.
	 * 
	 * @param next
	 * 			new follower
	 */
	public void setNext(SynmvJob next) {
		this.next = next;
		runCallback();
	}
	
	/**
	 * 
	 * @return the job's follower or null, if this job is the last one.
	 */
	public SynmvJob getNext() {
		return next;
	}
	
	/**
	 * Returns the nth follower of this job or null if there are less followers than n.
	 * @param n
	 * 		number of followers
	 * @return the nth follower or null
	 */
	public SynmvJob getNthNext(int n) {
		return getNthPred(-n);
	}
	
	/**
	 * Removes this job from the schedule and links its former predecessor
	 * and follower together. Runs the callback afterwards.
	 */
	public void unlink() {
		if(pred != null) {
			pred.next = next;
		}
		if(next != null) {
			next.pred = pred;
		}
		next = pred = null;
		runCallback();
	}

	/**
	 * Swaps (exchanges) this job with another and invokes the callback.
	 * 
	 * @param other
	 * 			the job to be swapped with
	 */
	public void swapWith(SynmvJob other) {
		if(other == next) {
			swapWithNext();
		}
		else if(other == pred) {
			swapWithPred();
		}
		else if(other == this) {
			return;
		}
		else if(other != null) {
			SynmvJob thispred = this.pred;
			SynmvJob thisnext = this.next;
			SynmvJob otherpred = other.pred;
			SynmvJob othernext = other.next;
			
			this.pred = otherpred;
			this.next = othernext;
			other.pred = thispred;
			other.next = thisnext;
			
			if(thispred != null) {
				thispred.next = other;
			}
			if(thisnext != null) {
				thisnext.pred = other;
			}
			if(otherpred != null) {
				otherpred.next = this;
			}
			if(othernext != null) {
				othernext.pred = this;
			}
			
			runCallback();
		}
	}
	
	/**
	 * Swaps this job with its predecessor. A call to this
	 * method is equivalent to swapWith(getPred()).
	 */
	public void swapWithPred() {
		if(pred != null) {
			SynmvJob tmpPred = pred;
			
			if(tmpPred.pred != null) {
				tmpPred.pred.next = this;
			}
			pred = tmpPred.pred;
			
			if(next != null) {
				next.pred = tmpPred;
			}
			tmpPred.next = next;
			
			next = tmpPred;
			tmpPred.pred = this;
			
			runCallback();
		}
	}
	
	/**
	 * Swaps this job with its follower. A call to this 
	 * method is equivalent to swapWith(getNext()).
	 */
	public void swapWithNext() {
		if(next != null) {
			next.swapWithPred();
		}
	}
	
	/**
	 * Adds this jobs components to the parent container.
	 */
	public void addToParent() {
		for(JLabel slot : slots) {
			parent.add(slot);
		}
		parent.add(number);
	}
	
	/**
	 * Removes this jobs components from the parent container.
	 */
	public void removeFromParent() {
		for(JLabel slot : slots) {
			parent.remove(slot);
		}
		parent.remove(number);
	}
	
	/**
	 * Counts the number of predecessors of this job, which can be used
	 * to calculate this job's position in the schedule.
	 * 
	 * @return the number of predecessors
	 */
	public int countPredecessors() {
		if(pred == null)
			return 0;
		return pred.countPredecessors() + 1;
	}
	
	/**
	 * Counts the number of followers of this job.
	 * 
	 * @return the number of followers
	 */
	public int countFollowers() {
		if(next == null)
			return 0;
		return next.countFollowers() + 1;
	}
	
	/**
	 * Returns a reference to the first predecessor i.e. the first job in the
	 * schedule.
	 * 
	 * @return the first job in the schedule
	 */
	public SynmvJob getFirstPredecessor() {
		if(pred == null)
			return this;
		return pred.getFirstPredecessor();
	}
	
	/**
	 * Returns a reference to the last follower i.e. the last job in the
	 * schedule.
	 * 
	 * @return the last job in the schedule
	 */
	public SynmvJob getLastFollower() {
		if(next == null)
			return this;
		return next.getLastFollower();
	}
	
	/**
	 * Calculates the time when this job starts on the first machine.
	 * This method does care about the variants flag which results in a
	 * synchronous, asynchronous, no-wait or blocking calculation.
	 * 
	 * @return the start time of the job
	 */
	public float getStartTime() {
		return getOffset(0);
	}
	
	/**
	 * Calculates the time when this job is done on the last machine.
	 * This method does care about the variants flag which results in a
	 * synchronous, asynchronous, no-wait or blocking calculation.
	 * 
	 * @return the end time of the job
	 */
	public float getEndTime() {
		switch(SynmvJob.variant) {
		case synchronous:
			return getOffset(getMachineCount()-1) + maxLen(getMachineCount()-1);
		case asynchronous:
			return getOffset(getMachineCount()-1) + getTime(getMachineCount()-1);
		case noWait:
			return getOffset(getMachineCount()-1) + getTime(getMachineCount()-1);
		case blocking:
			break;
		default:
			throw new RuntimeException("unknown variant, this cannot happen...");
		}
		
		return 0;
	}
	
	/**
	 * Shifts this job to the position of another job. That means this job is
	 * swapped with its predecessor or follower respectively until it is swapped
	 * with the other on.
	 * 
	 * @param other
	 * 			the job to be shifted to
	 * @return the number of positions this job has moved, negative means it was shifted to the left
	 */
	public int shiftTo(SynmvJob other) {
		int dir = other.countPredecessors() - this.countPredecessors();
		if(dir > 0) {
			for(int i = 0; i < dir; i++) {
				swapWithNext();
			}
		}
		else if(dir < 0) {
			for(int i = 0; i > dir; i--) {
				swapWithPred();
			}
		}
		return dir;
	}
	
	/**
	 * Highlights the job's slots with a color.
	 * 
	 * @param c
	 * 			color
	 */
	public void highlight(Color c) {
		for(JLabel slot : slots) {
			slot.setBackground(c);
		}
	}
	
	/**
	 * Highlights the job's slots with Color.ORANGE.
	 * A call to this method is equivalent to highlight(Color.ORANGE).
	 */
	public void highlight() {
		highlight(Color.ORANGE);
	}
	
	/**
	 * Sets the slots' color to the default one, which is Color.GRAY or
	 * Color.RED, if this is the chosen SynmvJob.
	 */
	public void setDefaultColor() {
		if(this == chosen) {
			highlight(Color.RED);
		}
		else {
			highlight(Color.GRAY);
		}
	}
	
	/**
	 * Runs the callback, if it is not null.
	 */
	public static void runCallback() {
		if(callback != null) {
			callback.run();
		}
	}
}
