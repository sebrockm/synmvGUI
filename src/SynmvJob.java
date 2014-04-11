import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;


public class SynmvJob {

	private Runnable callback;

	private static final int height = 60;
	public static float factor = 10;
	private final float[] times;
	private final JLabel[] slots;
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
			len = Math.max(len, tmp.getLen(i));
			if(tmp.pred == null) {
				break;
			}
			tmp = tmp.pred;
		}
		
		return len;
	}
	
	public float getOffset(int machine) {
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
	
	public void setPositions() {
		int max = 0;
		for(int i = 0; i < slots.length; i++) {
			slots[i].setSize(Math.max(1, (int)Math.ceil(factor * getLen(i))), height);	
			slots[i].setLocation(50+(int)(factor * getOffset(i)), 50+i*height);

			max = Math.max(max, slots[i].getSize().width + slots[i].getLocation().x);
			
			parent.setSize((int) max, parent.getSize().height);
			parent.setPreferredSize(parent.getSize());
		}
	}
	
	public void setCallback(Runnable callback) {
		this.callback = callback;
		callback.run();
	}
	
	public float getLen(int machine) {
		return times[machine];
	}
	
	public SynmvJob(final JPanel container, float[] times) {
		this.parent = container;
		this.times = times;
		slots = new JLabel[times.length];
		for(int i = 0; i < times.length; i++) {
			slots[i] = new JLabel();
			slots[i].setText("" + times[i]);
			slots[i].setBackground(Color.GRAY);
			slots[i].setVisible(true);
			slots[i].setOpaque(true);
			slots[i].setBorder(new LineBorder(Color.DARK_GRAY));
			
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
		setPositions();
		
	}
	
	public void setPred(SynmvJob pred) {
		this.pred = pred;
		if(callback != null) {
			callback.run();
		}
	}
	
	public void setNext(SynmvJob next) {
		this.next = next;
		if(callback != null) {
			callback.run();
		}
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
	}
	
	public void removeFromParent() {
		for(JLabel slot : slots) {
			parent.remove(slot);
		}
	}
}
