import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class represents the window the SynmvJobs are displayed in.
 * 
 * @author sebrockm
 *
 */
@SuppressWarnings("serial")
public class SynmvFrame extends JFrame {
	
	/**
	 * Indicator string that is followed by a schedule.
	 */
	private static final String SCHEDULE_INDICATOR = "#schedule";
	
	/**
	 * Indicator string that is followed by due dates.
	 */
	private static final String DUEDATE_INDICATOR = "#duedates";
	
	/**
	 * Indicator string that is followed by weights.
	 */
	private static final String WEIGHT_INDICATOR = "#weights";

	
	/**
	 * Container that is used as the jobs' parent.
	 */
	private final JPanel jobcontainer;
	
	/**
	 * JLabel that displays the Cmax and Lmax text.
	 */
	private final JLabel label = new JLabel();
	
	/**
	 * menu bar
	 */
	private final JMenuBar menubar = new JMenuBar();
	
	/**
	 * File-menu
	 */
	private final JMenu fileMenu = new JMenu("File");
	
	/**
	 * load jobs
	 */
	private final JMenuItem loadFile = new JMenuItem("load jobs");
	
	/**
	 * save jobs
	 */
	private final JMenuItem storeFile = new JMenuItem("save jobs");
	
	/**
	 * Edit-menu
	 */
	private final JMenu editMenu = new JMenu("Edit");
	
	/**
	 * undo item
	 */
	private final JMenuItem undoItem = new JMenuItem("undo");
	
	/**
	 * redo item
	 */
	private final JMenuItem redoItem = new JMenuItem("redo");
	
	/**
	 * zoom in item
	 */
	private final JMenuItem zoomIn = new JMenuItem("zoom in");
	
	/**
	 * zoom out item
	 */
	private final JMenuItem zoomOut = new JMenuItem("zoom out");
	
	/**
	 * reset zoom item
	 */
	private final JMenuItem resetZoom = new JMenuItem("reset zoom");
	
	/**
	 * Options-menu
	 */
	private final JMenu optionsMenu = new JMenu("Options");
	
	/**
	 * Continuous shift checkbox.
	 * If disabled a mouse shift of a job will not be visualized until the mouse is released.
	 */
	private final JCheckBoxMenuItem continuousShift = new JCheckBoxMenuItem("continuous shift", true);

	/**
	 * Sub menu for variants radio buttons.
	 */
	private final JMenu variantsSubMenu = new JMenu("Variants");
	
	private final JRadioButtonMenuItem synchronous = new JRadioButtonMenuItem("synchronous", true);
	
	private final JRadioButtonMenuItem asynchronous = new JRadioButtonMenuItem("asynchronous", false);
	
	private final JRadioButtonMenuItem noWait = new JRadioButtonMenuItem("no-wait", false);
	
	private final JRadioButtonMenuItem blocking = new JRadioButtonMenuItem("blocking", false);
	
	
	/**
	 * An array of SynmvJobs that shall be displayed in the window.
	 */
	private SynmvJob[] jobs = new SynmvJob[0];
	
	/**
	 * scroll pane
	 */
	private final JScrollPane scroll = new JScrollPane();
	
	/**
	 * JFileChooser for opening and saving job files.
	 */
	private final JFileChooser fileChooser = new JFileChooser();
	
	/**
	 * This exception shall be thrown, if a file read has an invalid format.
	 * 
	 * @author sebrockm
	 *
	 */
	@SuppressWarnings("unused")	
	private class InvalidFileFormatException extends Exception {
		public InvalidFileFormatException() {
			super();
		}
		public InvalidFileFormatException(String message) {
			super(message);
		}
	}
	
	/**
	 * Callback that is given to the SynmvJob class.
	 */
	private final Runnable callback = new Runnable(){
		@Override
		public void run() {
			for(SynmvJob job : jobs) {
				job.deleteOldOffsets();
			}
			
			float cmax = 0;
			float lmax = Float.NEGATIVE_INFINITY;
			float sumCmax = 0;
			int umax = 0;
			float tmax = 0;
			LinkedList<SynmvJob> critLmax = new LinkedList<SynmvJob>();
			for(SynmvJob job : jobs) {
				if(job != null) {
					job.setLocations();
					float finished = job.getEndTime();
					
					cmax = Math.max(cmax, finished);
					sumCmax += finished;
					if(finished > job.getDuedate()) {
						umax++;
						tmax = Math.max(tmax, finished - job.getDuedate());
					}
					
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
			text += "    " + (char)931 + "Cj: " + sumCmax; 
			if(SynmvJob.hasDuedates) {
				text += "    Lmax: " + lmax;
				text += "    Tmax: " + tmax;
				text += "    Umax: " + umax;
				for(SynmvJob job : jobs) {
					job.setDefaultColor();
				}
				for(SynmvJob job : critLmax) {
					job.highlight();
				}
			}
			label.setText(text);
			int width = label.getFontMetrics(label.getFont()).stringWidth(label.getText());
			label.setSize(width, label.getHeight());
		}
	};

	/**
	 * Reads a schedule from a line and delivers error messages (InvalidFileFormatException)
	 * 
	 * @param line
	 * 			the string containing the schedule
	 * @param n
	 * 			the expected length of the schedule
	 * @param filename
	 * 			the name of the file line is taken from
	 * @param lineNo
	 * 			the line number of line in file
	 * @return the schedule list
	 * @throws InvalidFileFormatException
	 * 			if the schedule is not valid. 
	 */
	private ArrayList<Integer> readSchedule(String line, int n, String filename, int lineNo) throws InvalidFileFormatException {
		line = line.trim();
		StringTokenizer tok = new StringTokenizer(line);
		if(tok.countTokens() != n) {
			throw new InvalidFileFormatException("in file " + filename + " in line " + 
					lineNo + " the schedule contains " + tok.countTokens() + " jobs, but " +
					"must contain " + n + " jobs");
		}
		
		ArrayList<Integer> schedule = new ArrayList<Integer>(n);
		while(tok.hasMoreTokens()) {
			int job;
			try {
				job = Integer.parseInt(tok.nextToken());
			}
			catch(NumberFormatException e) {
				throw new InvalidFileFormatException("in file " + filename + " in line " + 
						lineNo + " the schedule contains an invalid number: " + e.getMessage());
			}
			if(job < 1 || job > n) {
				throw new InvalidFileFormatException("in file " + filename + " in line " +
						lineNo + " a job id is not between 1 and " + n);
			}
			if(schedule.contains(job)) {
				throw new InvalidFileFormatException("in file " + filename + " in line " + 
						lineNo + " the schedule contains " + job + " at least twice");
			}
			schedule.add(job);
		}
		return schedule;
	}
	
	/**
	 * Returns the next line delivered by the buffers readLine() method that is not empty
	 * and is no comment (except for the (DUEDATE|SCHEDULE|WEIGHT)_INDICATOR) or null, if
	 * there is no next line with these properties.
	 * 
	 * @param buf
	 * 			the BufferedReader to read lines from
	 * @return the next non empty and no comment line
	 * @throws IOException if an reading error occurs
	 */
	private String getNextLine(BufferedReader buf) throws IOException {
		String line;
		while((line = buf.readLine()) != null) {
			line = line.trim();
			if(!line.isEmpty()) {
				if(line.startsWith(DUEDATE_INDICATOR) ||
						line.startsWith(SCHEDULE_INDICATOR) ||
						line.startsWith(WEIGHT_INDICATOR)) {
					return line;
				}
				if(line.charAt(0) != '#') {
					return line;
				}
			}
		}
		return null;
	}
	
	/**
	 * Reads jobs from a file.
	 * 
	 * @param filename
	 * 			name of the file to be read
	 * @return an array of read SynmvJobs
	 * @throws FileNotFoundException
	 * 			if the given file was not found
	 * @throws InvalidFileFormatException
	 * 			if the given file has an invalid format
	 */
	private SynmvJob[] readJobsFromFile(String filename) throws FileNotFoundException, InvalidFileFormatException {
		SynmvJob[] retjobs = null;
		BufferedReader buf = new BufferedReader(new FileReader(filename));
		
		try {
			//skip empty lines and comments
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
			
			//number of machines and jobs
			StringTokenizer tok = new StringTokenizer(line);
			if(tok.countTokens() != 2) {
				throw new InvalidFileFormatException("first line of " + filename + " is invalid: " + line);
			}
			
			//read number of machines
			int m;
			try {
				m = Integer.parseInt(tok.nextToken());
			} catch (NumberFormatException e) {
				throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
			}
			
			//read number of jobs
			int n = 0;
			try {
				n = Integer.parseInt(tok.nextToken());
			} catch (NumberFormatException e) {
				throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
			}
			retjobs = new SynmvJob[n];
			
			ArrayList<Integer> schedule = null;
			
			//read process times
			int i = 0;
			while(i < n && (line = buf.readLine()) != null) {
				lineNo++;
				line = line.trim();
				
				//look for schedule
				if(i == 0 && line.startsWith(SCHEDULE_INDICATOR)) {
					line = buf.readLine();
					lineNo++;
					if(line == null) {
						break;
					}
					schedule = readSchedule(line, n, filename, lineNo);
					continue;
				}
				
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
				i++;
			}
			if(i < n) {
				throw new InvalidFileFormatException(n + 
						" jobs are required but " + filename + " contains only " + i);
			}
			
			// look for additional information
			while((line = buf.readLine()) != null) {
				lineNo++;
				line = line.trim();
				
				//look for schedule
				if(line.startsWith(SCHEDULE_INDICATOR)) {
					if(schedule != null) {
						throw new InvalidFileFormatException("in file " + filename + " in line " +
								lineNo + " there is a second schedule indicator");
					}
					line = buf.readLine();
					lineNo++;
					if(line == null) {
						throw new InvalidFileFormatException("file " + filename + " ends with schedule indicator");
					}
					schedule = readSchedule(line, n, filename, lineNo);
				}
				//look for due dates
				else if(line.startsWith(DUEDATE_INDICATOR) || //either there is a due date indicator
						(!SynmvJob.hasDuedates && !SynmvJob.hasWeights && //or neither due dates nor weights have been read yet
						!line.isEmpty() && line.charAt(0) != '#' && new StringTokenizer(line).countTokens() == 2)) {
					
					if(line.startsWith(DUEDATE_INDICATOR)) {
						line = buf.readLine();
						if(line == null) {
							throw new InvalidFileFormatException("file " + filename + " ends with due date indicator");
						}
					}
					i = 0;
					do {
						line = line.trim();
						lineNo++;
						if(line.isEmpty() || line.charAt(0) == '#') {
							continue;
						}
						
						tok = new StringTokenizer(line);
						if(tok.countTokens() != 2) {
							throw new InvalidFileFormatException("in file " + filename + " in line " +
									lineNo + " there are " + tok.countTokens() + " instead of 2 tokens");
						}
						
						int id;
						try {
							id = Integer.parseInt(tok.nextToken());
						} catch (NumberFormatException e) {
							throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
						}
						if(id < 1 || id > n) {
							throw new InvalidFileFormatException("in file " + filename + " in line " +
									lineNo + " the job id is not between 1 and " + n);
						}
						
						float duedate;
						try {
							duedate = Float.parseFloat(tok.nextToken());
						} catch (NumberFormatException e) {
							throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
						}
						
						retjobs[id-1].setDuedate(duedate);
						i++;
					} while(i < n && (line = buf.readLine()) != null);
					if(i < n) {
						throw new InvalidFileFormatException("in file " + filename + " there are only " + i + " due dates instead of " + n);
					}
					else {
						SynmvJob.hasDuedates = true;
					}
				}
				else if(line.startsWith(WEIGHT_INDICATOR) || //either there is a weight indicator
						(SynmvJob.hasDuedates && !SynmvJob.hasWeights && //or due dates have been read already and weights have not
								!line.isEmpty() && line.charAt(0) != '#' && new StringTokenizer(line).countTokens() == 2)) {
					
					if(line.startsWith(WEIGHT_INDICATOR)) {
						line = buf.readLine();
						if(line == null) {
							throw new InvalidFileFormatException("file " + filename + " ends with weight indicator");
						}
					}
					i = 0;
					do {
						line = line.trim();
						lineNo++;
						if(line.isEmpty() || line.charAt(0) == '#') {
							continue;
						}
						
						tok = new StringTokenizer(line);
						if(tok.countTokens() != 2) {
							throw new InvalidFileFormatException("in file " + filename + " in line " +
									lineNo + " there are " + tok.countTokens() + " instead of 2 tokens");
						}
						
						int id;
						try {
							id = Integer.parseInt(tok.nextToken());
						} catch (NumberFormatException e) {
							throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
						}
						if(id < 1 || id > n) {
							throw new InvalidFileFormatException("in file " + filename + " in line " +
									lineNo + " the job id is not between 1 and " + n);
						}
						
						float weight;
						try {
							weight = Float.parseFloat(tok.nextToken());
						} catch (NumberFormatException e) {
							throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
						}
						
						retjobs[id-1].setWeight(weight);
						i++;
					} while(i < n && (line = buf.readLine()) != null);
					if(i < n) {
						throw new InvalidFileFormatException("in file " + filename + " there are only " + i + " weights instead of " + n);
					}
					else {
						SynmvJob.hasWeights = true;
					}
				}
				//empty or comment line
				else if(line.isEmpty() || line.charAt(0) == '#') {
					continue;
				}
				else {
					throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + 
							" there is unknown information: " + line);
				}
			}
			
			if(schedule == null) {// default schedule
				for(int j = 1; j < n; j++) {
					retjobs[j].setPred(retjobs[j-1]);
					retjobs[j-1].setNext(retjobs[j]);
				}
			}
			else {
				for(int j = 1; j < n; j++) {
					retjobs[schedule.get(j)-1].setPred(retjobs[schedule.get(j-1)-1]);
					retjobs[schedule.get(j-1)-1].setNext(retjobs[schedule.get(j)-1]);
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
		
		SynmvJob.actionList.clear();
		SynmvJob.undoneActionList.clear();
		return retjobs;
	}
	
	/**
	 * Stores the current schedule in a file that can be read again later.
	 * 
	 * @param filename
	 * 			name of the file the schedule shall be stored in
	 * @throws IOException 
	 * 			if an IO error occurs
	 */
	private void storeJobsToFile(String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

		//write m n
		writer.write(jobs[0].getMachineCount() + " " + jobs.length);
		writer.newLine();
		
		//write process times
		for(SynmvJob job : jobs) {
			for(int i = 0; i < job.getMachineCount(); i++) {
				writer.write(job.getTime(i) + " ");
			}
			writer.newLine();
		}
		
		//write schedule
		writer.newLine();
		writer.write(SCHEDULE_INDICATOR);
		writer.newLine();
		SynmvJob tmp = jobs[0].getFirstPredecessor();
		while(tmp != null) {
			writer.write(tmp.getID() + " ");
			tmp = tmp.getNext();
		}
		writer.newLine();
		
		//write due dates
		if(SynmvJob.hasDuedates) {
			writer.newLine();
			writer.write(DUEDATE_INDICATOR);
			writer.newLine();
			for(SynmvJob job : jobs) {
				writer.write(job.getID() + " " + job.getDuedate());
				writer.newLine();
			}
		}
		
		//write weights
		if(SynmvJob.hasWeights) {
			writer.newLine();
			writer.write(WEIGHT_INDICATOR);
			writer.newLine();
			for(SynmvJob job : jobs) {
				writer.write(job.getID() + " " + job.getWeight());
				writer.newLine();
			}
		}
		
		writer.close();
	}


	/**
	 * Creates a new SynmvFrame.
	 * This means all components will be initialized, sized, placed and event
	 * listeners will be added.
	 */
	public SynmvFrame() {
		super();
		
		SynmvJob.callback = callback;

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
					SynmvJob.factor *= Math.pow(1.1, -rot);
					SynmvJob.runCallback();
				}
			}
		});

		this.add(label,BorderLayout.SOUTH);
		label.setSize(280, 15);
		
		scroll.setViewportView(jobcontainer);
		this.add(scroll);

		this.add(menubar, BorderLayout.NORTH);
		menubar.add(fileMenu);
		fileMenu.add(loadFile);
		fileMenu.add(storeFile);
		menubar.add(editMenu);
		editMenu.add(undoItem);
		editMenu.add(redoItem);
		editMenu.add(zoomIn);
		editMenu.add(zoomOut);
		editMenu.add(resetZoom);
		menubar.add(optionsMenu);
		optionsMenu.add(variantsSubMenu);
		variantsSubMenu.add(synchronous);
		variantsSubMenu.add(asynchronous);
		variantsSubMenu.add(noWait);
		variantsSubMenu.add(blocking);
		optionsMenu.add(continuousShift);
		
		ButtonGroup variantsGroup = new ButtonGroup();
		variantsGroup.add(synchronous);
		variantsGroup.add(asynchronous);
		variantsGroup.add(noWait);
		variantsGroup.add(blocking);
		
	
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
					}
					SynmvJob.runCallback();
				}
			}
		});
		loadFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		
		storeFile.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				if(jobs == null || jobs.length == 0) {
					JOptionPane.showMessageDialog(SynmvFrame.this, "there is nothing to store");
					return;
				}
				int ret = fileChooser.showSaveDialog(SynmvFrame.this);
				if(ret == JFileChooser.APPROVE_OPTION) {
					try {
						storeJobsToFile(fileChooser.getSelectedFile().getAbsolutePath());
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(SynmvFrame.this, e1.getMessage(), "cannot write to file", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		storeFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		
		undoItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!SynmvJob.actionList.isEmpty()){
					SynmvJobAction a = SynmvJob.actionList.removeFirst();
					a.undo();
					SynmvJob.undoneActionList.addFirst(a);
				}
			}
		});
		undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
		
		redoItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!SynmvJob.undoneActionList.isEmpty()) {
					SynmvJobAction a = SynmvJob.undoneActionList.removeFirst();
					a.run();
					SynmvJob.actionList.addFirst(a);
				}
			}
		});
		redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
		
		zoomIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SynmvJob.factor *= 1.1f;
				SynmvJob.runCallback();
			}
		});
		zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK));
		
		zoomOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SynmvJob.factor /= 1.1f;
				SynmvJob.runCallback();
			}
		});
		zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK));
	
		resetZoom.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SynmvJob.factor = SynmvJob.FACTOR;
				SynmvJob.runCallback();
			}
		});
		resetZoom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_DOWN_MASK));
		
		synchronous.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if(synchronous.isSelected()) {
					SynmvJob.variant = SynmvJob.Variant.synchronous;
					SynmvJob.runCallback();
				}
			}
		});
		
		asynchronous.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if(asynchronous.isSelected()) {
					SynmvJob.variant = SynmvJob.Variant.asynchronous;
					SynmvJob.runCallback();
				}
			}
		});
		
		noWait.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if(noWait.isSelected()) {
					SynmvJob.variant = SynmvJob.Variant.noWait;
					SynmvJob.runCallback();
				}
			}
		});

		blocking.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if(blocking.isSelected()) {
					SynmvJob.variant = SynmvJob.Variant.blocking;
					SynmvJob.runCallback();
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
