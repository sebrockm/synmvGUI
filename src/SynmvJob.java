import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;


public class SynmvJob {

	private static SynmvJob chosen = null;
	
	private Runnable callback;
	
	private final int id;

	private static final int height = 60;
	public static float factor = 10;
	private final float[] times;
	private final JTextField[] textFields;
	private final JLabel[] slots;
	private final JLabel number = new JLabel();
	private final JPanel parent;
	
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
			slots[i].setLocation(50+(int)(factor * getOffset(i)), 50+i*height);

			max = Math.max(max, slots[i].getSize().width + slots[i].getLocation().x);
			
			parent.setSize((int) max, parent.getSize().height);
			parent.setPreferredSize(parent.getSize());
			
			int y = slots[i].getSize().height / 2 - textFields[i].getSize().height / 2;
			textFields[i].setLocation(0, y);
			textFields[i].setSize(slots[i].getSize().width, textFields[i].getSize().height);
		}
		
		number.setSize(slots[0].getSize());
		Point loc = slots[0].getLocation();
		loc.y -= number.getSize().height;
		number.setLocation(loc);
		number.setText("" + (countPredecessors()+1));
	}
	
	public void setCallback(Runnable callback) {
		this.callback = callback;
		callback.run();
	}

	
	public float getTime(int machine) {
		return times[machine];
	}
	
	public SynmvJob(final JPanel container, int id, float[] times) {
		this.id = id;
		this.parent = container;
		this.times = times;
		this.number.setVisible(true);
		this.number.setHorizontalAlignment(SwingConstants.CENTER);
		slots = new JLabel[times.length];
		textFields = new JTextField[times.length];
		for(int i = 0; i < times.length; i++) {
			slots[i] = new JLabel();
			slots[i].setText("" + times[i]);
			slots[i].setBackground(Color.GRAY);
			slots[i].setVisible(true);
			slots[i].setOpaque(true);
			slots[i].setBorder(new LineBorder(Color.DARK_GRAY));
			slots[i].setHorizontalAlignment(SwingConstants.CENTER);
			
			textFields[i] = new JTextField(slots[i].getText());
			slots[i].add(textFields[i]);
			textFields[i].setVisible(false);
			textFields[i].setSize(50, 20);
			textFields[i].setHorizontalAlignment(SwingConstants.CENTER);
			textFields[i].setFont(slots[i].getFont());
			
			slots[i].addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
					
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

				@Override
				public void mouseReleased(MouseEvent e) {
					mouseHold = false;
					if(callback != null) {
						callback.run();
					}
				}
			});
			
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
		}
		setLocations();
		
	}
	
	public int getID() {
		return id;
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
}
