import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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


public class SynmvJob {

	public static SynmvJob chosen = null;
	
	private Runnable callback;
	
	private final int id;
	private final float[] times;
	private float duedate;

	public static final int height = 60;
	public static float factor = 10;
	public static int xOffset = 50;
	public static int yOffset = 80;
	
	private final JTextField[] textFields;
	private final JLabel[] slots;
	private final JLabel number = new JLabel();
	private final JPanel parent;
	
	private final JFrame infobox = new JFrame("info");
	private final JLabel idLabel = new JLabel();
	private final JTextField cycleField = new JTextField();
	private final JTextField[] infoTimeFields;
	private final JLabel startTimeLabel = new JLabel();
	private final JLabel endTimeLabel = new JLabel();
	private final JTextField duedateField = new JTextField();
	
	private SynmvJob pred;
	private SynmvJob next;
	
	private boolean mouseHold = false;

	
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
	
	public float getOffset(int machine) {
		if(machine < 0 || machine >= getMachineCount()) {
			throw new IllegalArgumentException("'machine' must be in [0,getMachineCount()[");
		}
		if(pred == null) {
			if(machine == 0) {
				return 0;
			}
			return getOffset(machine-1) + maxLen(machine-1);
		}
		return pred.getOffset(machine) + pred.maxLen(machine);
	}
	
	public int getMachineCount() {
		return slots.length;
	}
	
	public void setLocations() {
		int max = 0;
		for(int i = 0; i < slots.length; i++) {
			slots[i].setSize(Math.max(10, (int)Math.ceil(factor * getTime(i))), height);	
			slots[i].setLocation(xOffset+(int)(factor * getOffset(i)), yOffset+i*height);

			max = Math.max(max, slots[i].getSize().width + slots[i].getLocation().x);
			
			parent.setSize((int) max, slots.length * height + yOffset);
			parent.setPreferredSize(parent.getSize());
			
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
	}
	
	public void setCallback(Runnable callback) {
		this.callback = callback;
		callback.run();
	}

	
	public float getTime(int machine) {
		return times[machine];
	}
	
	public SynmvJob(final JPanel container, int id, float[] times) {
		this(container, id, times, -1.f);
	}
	
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
					if(callback != null) {
						callback.run();
					}
				}
			});
				
			final FontMetrics metric = textFields[i].getFontMetrics(textFields[i].getFont());
			textFields[i].setSize(metric.stringWidth("" + times[i]) + 5, height/3);
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
	
	private void initSlots() {
		for(int i = 0; i < slots.length; i++) {
			slots[i].setText(textFields[i].getText());
			slots[i].setBackground(Color.GRAY);
			slots[i].setVisible(true);
			slots[i].setOpaque(true);
			slots[i].setBorder(new LineBorder(Color.DARK_GRAY));
			slots[i].setHorizontalAlignment(SwingConstants.CENTER);
				
			slots[i].add(textFields[i]);
			
			slots[i].addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON3 && chosen != null) {
						chosen.swapWith(SynmvJob.this);
					}
					
					if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
						showInfobox();
					}
				}
	
				@Override
				public void mouseEntered(MouseEvent e) {
					for(JLabel slot: slots) {
						slot.setBorder(new LineBorder(Color.RED));
					}
					if(pred != null && pred.mouseHold) {
						swapWithPred();
					}
					else if(next != null && next.mouseHold) {
						swapWithNext();
					}
				}
	
				@Override
				public void mouseExited(MouseEvent e) {
					for(JLabel slot: slots) {
						slot.setBorder(new LineBorder(Color.DARK_GRAY));
					}
				}
	
				@Override
				public void mousePressed(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) {
						mouseHold = true;
						if(chosen != null) {
							for(JLabel slot : chosen.slots) {
								slot.setBackground(Color.GRAY);
							}
							for(JTextField field : chosen.textFields) {
								field.setVisible(false);
							}
						}
						chosen = SynmvJob.this;
						for(JLabel slot : chosen.slots) {
							slot.setBackground(Color.RED);
						}				
						for(JTextField field : textFields) {
							field.setVisible(true);
						}
					}
				}
	
				@Override
				public void mouseReleased(MouseEvent e) {
					mouseHold = false;
					if(callback != null) {
						callback.run();
					}
				}
			});
		}
	}
	
	private void initInfobox() {
		infobox.setVisible(false);
		
		int rows = 5 + times.length;
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
				
				while(pos <= countPredecessors()) {
					swapWithPred();
				}
				while(pos >= countPredecessors() + 2) {
					swapWithNext();
				}
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
				tmp.swapWith(SynmvJob.this);
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
					if(callback != null) {
						callback.run();
					}
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
				if(callback != null) {
					callback.run();
				}
			}
		});
	}
	
	private void showInfobox() {
		infobox.setVisible(true);
		
		cycleField.setText("" + (countPredecessors()+1));
		for(int i = 0; i < times.length; i++) {
			infoTimeFields[i].setText("" + times[i]);
		}
		startTimeLabel.setText("" + getStartTime());
		endTimeLabel.setText("" + getEndTime());
		duedateField.setText("" + (duedate < 0.f ? "-" : duedate));
		
		infobox.pack();
	}
	
	public SynmvJob(final JPanel container, int id, float[] times, float duedate) {
		this.id = id;
		this.parent = container;
		this.times = times;
		this.duedate = duedate;
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
	
	public int getID() {
		return id;
	}
	
	public float getDuedate() {
		return duedate;
	}
	
	public void setDuedate(float duedate) {
		this.duedate = duedate;
	}
	
	public void setPred(SynmvJob pred) {
		this.pred = pred;
		if(callback != null) {
			callback.run();
		}
	}
	
	public SynmvJob getPred() {
		return pred;
	}
	
	public void setNext(SynmvJob next) {
		this.next = next;
		if(callback != null) {
			callback.run();
		}
	}
	
	public SynmvJob getNext() {
		return next;
	}
	
	public void unlink() {
		if(pred != null) {
			pred.next = next;
		}
		if(next != null) {
			next.pred = pred;
		}
		next = pred = null;
		if(callback != null) {
			callback.run();
		}
	}

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
			
			if(callback != null) {
				callback.run();
			}
		}
	}
	
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
			
			if(callback != null) {
				callback.run();
			}
		}
	}
	
	public void swapWithNext() {
		if(next != null) {
			next.swapWithPred();
		}
	}
	
	public void addToParent() {
		for(JLabel slot : slots) {
			parent.add(slot);
		}
		parent.add(number);
	}
	
	public void removeFromParent() {
		for(JLabel slot : slots) {
			parent.remove(slot);
		}
		parent.remove(number);
	}
	
	public int countPredecessors() {
		if(pred == null)
			return 0;
		return pred.countPredecessors() + 1;
	}
	
	public int countFollowers() {
		if(next == null)
			return 0;
		return next.countFollowers() + 1;
	}
	
	public SynmvJob getFirstPredecessor() {
		if(pred == null)
			return this;
		return pred.getFirstPredecessor();
	}
	
	public SynmvJob getLastFollower() {
		if(next == null)
			return this;
		return next.getLastFollower();
	}
	
	public float getStartTime() {
		return getOffset(0);
	}
	
	public float getEndTime() {
		return getOffset(getMachineCount()-1) + maxLen(getMachineCount()-1);
	}
}
