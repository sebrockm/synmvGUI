import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


@SuppressWarnings("serial")
public class SynmvFrame extends JFrame {

	private final JPanel jobcontainer;
	private final JLabel label = new JLabel();
	private final JMenuBar menubar = new JMenuBar();
	private final JMenu fileMenu = new JMenu("File");
	private final JMenuItem loadFile = new JMenuItem("load jobs");
	private final JMenuItem storeFile = new JMenuItem("save jobs");
	private final JMenu optionsMenu = new JMenu("Options");
	private final JCheckBoxMenuItem continuousShift = new JCheckBoxMenuItem("continuous shift", true);
	private final JCheckBoxMenuItem synchronous = new JCheckBoxMenuItem("synchronous", true);
	private SynmvJob[] jobs = new SynmvJob[0];
	
	private final JScrollPane scroll = new JScrollPane();
	private final JFileChooser fileChooser = new JFileChooser();
	
	@SuppressWarnings("unused")	
	private class InvalidFileFormatException extends Exception {
		public InvalidFileFormatException() {
			super();
		}
		public InvalidFileFormatException(String message) {
			super(message);
		}
	}
	
	private SynmvJob[] readJobsFromFile(String filename) throws FileNotFoundException, InvalidFileFormatException {
		SynmvJob[] retjobs = null;
		BufferedReader buf = new BufferedReader(new FileReader(filename));
		
		try {
			String line;
			int lineNo = 0;
			do {
				line = buf.readLine();
				lineNo++;
				if(line == null) {
					throw new InvalidFileFormatException("the file " + filename + " is empty");
				}
				line = line.trim();
			} while(line.isEmpty() || line.charAt(0) == '#');
			StringTokenizer tok = new StringTokenizer(line);
			if(tok.countTokens() != 2) {
				throw new InvalidFileFormatException("first line of " + filename + " is invalid: " + line);
			}
			
			int m;
			try {
				m = Integer.parseInt(tok.nextToken());
			} catch (NumberFormatException e) {
				throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
			}
			
			int n = 0;
			try {
				n = Integer.parseInt(tok.nextToken());
			} catch (NumberFormatException e) {
				throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
			}
			retjobs = new SynmvJob[n];
			
			int i = 0;
			while(i < n && (line = buf.readLine()) != null) {
				lineNo++;
				line = line.trim();
				if(line.isEmpty() || line.charAt(0) == '#')
					continue;
				
				tok = new StringTokenizer(line);
				if(tok.countTokens() != m) {
					throw new InvalidFileFormatException("in file " + filename + " in line " + 
							lineNo + " there are " + tok.countTokens() + " numbers instead of " + m);
				}
				float[] times = new float[m];
				for(int j = 0; j < m; j++) {
					try {
						times[j] = Float.parseFloat(tok.nextToken());
					} catch (NumberFormatException e) {
						throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
					}
				}
				retjobs[i] = new SynmvJob(jobcontainer, i+1, times);
				if(i > 0) {
					retjobs[i].setPred(retjobs[i-1]);
					retjobs[i-1].setNext(retjobs[i]);
				}
				i++;
			}
			if(i < n) {
				throw new InvalidFileFormatException(n + 
						" jobs are required but " + filename + " contains only " + i);
			}
			else { // look for duedates
				i = 0;
				while(i < n && (line = buf.readLine()) != null) {
					lineNo++;
					line = line.trim();
					if(line.isEmpty() || line.charAt(0) == '#')
						continue;
					
					tok = new StringTokenizer(line);
					if(tok.countTokens() != 2) {
						break;
					}
					
					int id;
					try {
						id = Integer.parseInt(tok.nextToken());
					} catch (NumberFormatException e) {
						break;
					}
					if(id < 1 || id > n) {
						System.err.println("line " + lineNo + " id=" + id);
						break;
					}
					
					float duedate;
					try {
						duedate = Float.parseFloat(tok.nextToken());
					} catch (NumberFormatException e) {
						break;
					}
					
					retjobs[id-1].setDuedate(duedate);
					i++;
				}
				if(i < n) { // unset all duedates
					for(SynmvJob job : retjobs) {
						job.setDuedate(-1.f);
					}
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				buf.close();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		return retjobs;
	}
	
	private void storeJobsToFile(String filename) {
		BufferedWriter writer = null;
		
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			
			SynmvJob tmp = jobs[0].getFirstPredecessor();
			
			writer.write(tmp.getMachineCount() + " " + jobs.length);
			writer.newLine();
			
			while(tmp != null) {
				for(int i = 0; i < tmp.getMachineCount(); i++) {
					writer.write(tmp.getTime(i) + " ");
				}
				writer.newLine();
				
				tmp = tmp.getNext();
			}
			
			if(jobs[0].getDuedate() >= 0.f) {
				tmp = jobs[0].getFirstPredecessor();
				writer.newLine();
				int i = 1;
				while (tmp != null) {
					writer.write(i + " " + tmp.getDuedate());
					writer.newLine();
					tmp = tmp.getNext();
					i++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	public SynmvFrame() throws FileNotFoundException {
		super();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(1000, 500));
		this.setVisible(true);
		
		jobcontainer = new JPanel() {	
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				
				SynmvJob job = SynmvJob.chosen;
				if(jobs != null && job != null) {
					Graphics2D g2d = (Graphics2D) g;
					Color old = g2d.getColor();
					g2d.setColor(Color.RED);
					if(job.getDuedate() >= 0.f) {
						int x = SynmvJob.xOffset + (int)(job.getDuedate() * SynmvJob.factor);
						int y = SynmvJob.yOffset - SynmvJob.HEIGHT/2;
						int l = job.getMachineCount() * SynmvJob.HEIGHT + SynmvJob.HEIGHT;
						g2d.drawLine(x, y, x, y+l);
					}
					g2d.setColor(old);
				}
			}
		};
		
		jobcontainer.setLayout(null);
		jobcontainer.addMouseWheelListener(new MouseWheelListener() {	
			@Override
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				if(arg0.isControlDown()) {
					int rot = arg0.getWheelRotation();
					if(rot < 0) {
						SynmvJob.factor *= 1.1f;
					}
					else if(rot > 0) {
						SynmvJob.factor /= 1.1f;
					}
					
					for(SynmvJob job : jobs) {
						job.setLocations();
					}
				}
			}
		});
		
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				if(arg0.isControlDown()) {
					switch(arg0.getKeyChar()) {
						case '+': SynmvJob.factor *= 1.1f; break;
						case '-': SynmvJob.factor /= 1.1f; break;
						case '0': SynmvJob.factor = SynmvJob.FACTOR; break;
					}

					for(SynmvJob job : jobs) {
						job.setLocations();
					}
				}
			}
		});

		
		scroll.setViewportView(jobcontainer);
		this.add(scroll);

		this.add(menubar, BorderLayout.NORTH);
		menubar.add(fileMenu);
		fileMenu.add(loadFile);
		fileMenu.add(storeFile);
		menubar.add(optionsMenu);
		optionsMenu.add(synchronous);
		optionsMenu.add(continuousShift);
		
		jobcontainer.add(label, BorderLayout.SOUTH);
		label.setSize(280, 15);
	
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

		loadFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg) {
				int ret = fileChooser.showOpenDialog(SynmvFrame.this);
				if(ret == JFileChooser.APPROVE_OPTION) {
					SynmvJob[] tmp = null;
					try {
						tmp = readJobsFromFile(fileChooser.getSelectedFile().getAbsolutePath());
					} catch (FileNotFoundException e) {
						JOptionPane.showMessageDialog(SynmvFrame.this, e.getMessage(), "file not found", JOptionPane.ERROR_MESSAGE);
					} catch (InvalidFileFormatException e) {
						JOptionPane.showMessageDialog(SynmvFrame.this, e.getMessage(), "invalide file format", JOptionPane.ERROR_MESSAGE);
					}
					
					if(tmp == null) {
						return;
					}
					if(jobs != null) {
						for(SynmvJob job : jobs) {
							if(job != null) {
								job.removeFromParent();
							}
						}
					}
					jobs = tmp;
					
					for(int i = 0; i < jobs.length; i++) {
						jobs[i].addToParent();
						jobs[i].setCallback(new Runnable(){
							@Override
							public void run() {
								float cmax = 0;
								float lmax = 0;
								LinkedList<SynmvJob> critLmax = new LinkedList<SynmvJob>();
								for(SynmvJob job : jobs) {
									if(job != null) {
										job.setLocations();
										float finished = job.getEndTime();
										cmax = Math.max(cmax, finished);
										
										if(lmax == finished - job.getDuedate()) {
											critLmax.add(job);
										}
										else if(lmax < finished - job.getDuedate()) {
											lmax = finished - job.getDuedate();
											critLmax.clear();
											critLmax.add(job);
										}
									}
								}
								String text = "Cmax: " + cmax;
								if(jobs[0].getDuedate() >= 0.f) {
									text += "    Lmax: " + lmax;
									for(SynmvJob job : jobs) {
										job.setDefaultColor();
									}
									for(SynmvJob job : critLmax) {
										job.highlight();
									}
								}
								label.setText(text);
							}
						});
					}
				}
			}
		});
		
		storeFile.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				if(jobs == null || jobs.length == 0) {
					JOptionPane.showMessageDialog(SynmvFrame.this, "there is nothing to store");
					return;
				}
				int ret = fileChooser.showSaveDialog(SynmvFrame.this);
				if(ret == JFileChooser.APPROVE_OPTION) {
					storeJobsToFile(fileChooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		
		synchronous.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				SynmvJob.synchronous = synchronous.getState();
				for(SynmvJob job : jobs) {
					job.runCallback();
				}
			}
		});
		continuousShift.addChangeListener(new ChangeListener() {	
			@Override
			public void stateChanged(ChangeEvent arg0) {
				SynmvJob.continuousShift = continuousShift.getState();
			}
		});

		this.pack();
	}	
	
}
