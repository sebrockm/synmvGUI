import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


@SuppressWarnings("serial")
public class SynmvFrame extends JFrame {

	private final JPanel jobcontainer = new JPanel();
	private final JLabel label = new JLabel();
	private final JButton loadFile = new JButton("load jobs");
	private final JButton loadConfig = new JButton("load schedule");
	private final JButton storeConfig = new JButton("save schedule");
	private SynmvJob[] jobs = new SynmvJob[0];
	
	private final JScrollPane scroll = new JScrollPane();
	private final JFileChooser fileChooser = new JFileChooser();
	private final JFileChooser configChooser = new JFileChooser();
	
	private class ReadJobsFirstException extends Exception {
		public ReadJobsFirstException() {
			super();
		}
		@SuppressWarnings("unused")
		public ReadJobsFirstException(String message) {
			super(message);
		}
	}
	
	private class InvalidFileFormatException extends Exception {
		@SuppressWarnings("unused")
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
	
	private void storeConfigToFile(String filename) {
		BufferedWriter writer = null;
		
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			
			SynmvJob tmp = jobs[0];
			while(tmp.getPred() != null) {
				tmp = tmp.getPred();
			}
			int i;
			for(i = 0; i < jobs.length && tmp != null; i++) {
				writer.write(tmp.getID() + " " + (i+1));
				writer.newLine();
				tmp = tmp.getNext();
			}
			if(tmp != null || i != jobs.length) {
				throw new RuntimeException("something seriously went wrong");
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

	
	private int[] readConfigFromFile(String filename) 
			throws FileNotFoundException, ReadJobsFirstException, InvalidFileFormatException {
		if(jobs == null || jobs.length == 0) {
			throw new ReadJobsFirstException();
		}
		int[] res = new int[jobs.length];
		int[] posToId = new int[jobs.length];
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		
		try {
			int lineNo = 0;
			for(int i = 0; i < jobs.length; i++) {
				String line;
				do {
					line = reader.readLine();
					lineNo++;
					if(line == null) {
						throw new InvalidFileFormatException(res.length + 
								" jobs are required but " + filename + " contains only " + i);
					}
					line = line.trim();
				} while(line.isEmpty() || line.charAt(0) == '#');
				
				StringTokenizer tok = new StringTokenizer(line);
				if(tok.countTokens() != 2) {
					throw new InvalidFileFormatException("invalid file format in file " + 
							filename + " in line " + lineNo + ": " + line);
				}
				
				int id;
				try {
					id = Integer.parseInt(tok.nextToken());
				} catch (NumberFormatException e) {
					throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
				}
				if(id <= 0 || id > res.length) {
					throw new InvalidFileFormatException("invalid job id in file " + 
							filename + " in line " + lineNo + ": " + line);
				}
				if(res[id-1] != 0) {
					throw new InvalidFileFormatException("job id " + id + " appears more than once in file " + filename);
				}
				
				int pos;
				try {
					pos = Integer.parseInt(tok.nextToken());
				} catch (NumberFormatException e) {
					throw new InvalidFileFormatException("in file " + filename + " in line " + lineNo + ": " + e.getMessage());
				}
				if(pos <= 0 || pos > res.length) {
					throw new InvalidFileFormatException("invalid job position in file " + 
							filename + " in line " + lineNo + ": " + line);
				}
				if(posToId[pos-1] != 0) {
					throw new InvalidFileFormatException("job position " + pos + " appears more than once in file " + filename);
				}
				
				res[id-1] = pos;
				posToId[pos-1] = id;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return res;
	}
	
	public SynmvFrame() throws FileNotFoundException {
		super();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(1000, 500));
		this.setVisible(true);
		
		jobcontainer.setLayout(null);
		jobcontainer.setPreferredSize(this.getSize());
		jobcontainer.addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent arg0) {
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
		});

		
		scroll.setViewportView(jobcontainer);
		this.add(scroll);

		
		jobcontainer.add(loadFile);
		loadFile.setLocation(200, 400);
		loadFile.setSize(150, 40);
		
		jobcontainer.add(loadConfig);
		loadConfig.setLocation(400, 400);
		loadConfig.setSize(150, 40);
		
		jobcontainer.add(storeConfig);
		storeConfig.setLocation(600, 400);
		storeConfig.setSize(150,  40);
		
		jobcontainer.add(label);
		label.setLocation(20, 400);
		label.setSize(280, 15);

		
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		configChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

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
								for(SynmvJob job : jobs) {
									if(job != null) {
										job.setLocations();
										cmax = Math.max(cmax, job.getOffset(job.getMachineCount()-1) + job.getTime(job.getMachineCount()-1));
									}
								}
								label.setText("Cmax: " + cmax);
							}
						});
					}
				}
			}
		});
		
		storeConfig.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(jobs == null || jobs.length == 0) {
					JOptionPane.showMessageDialog(SynmvFrame.this, "there is nothing to store");
					return;
				}
				int ret = configChooser.showSaveDialog(SynmvFrame.this);
				if(ret == JFileChooser.APPROVE_OPTION) {
					storeConfigToFile(configChooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		
		loadConfig.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(jobs == null || jobs.length == 0) {
					JOptionPane.showMessageDialog(SynmvFrame.this, "read jobs file first");
					return;
				}
				
				int ret = configChooser.showOpenDialog(SynmvFrame.this);
				if(ret == JFileChooser.APPROVE_OPTION) {
					int[] config = null;
					try {
						config = readConfigFromFile(configChooser.getSelectedFile().getAbsolutePath());
					} catch (FileNotFoundException e) {
						JOptionPane.showMessageDialog(SynmvFrame.this, e.getMessage(), "file not found", JOptionPane.ERROR_MESSAGE);
					} catch (ReadJobsFirstException e) {
						JOptionPane.showMessageDialog(SynmvFrame.this, "read jobs file first");
					} catch (InvalidFileFormatException e) {
						JOptionPane.showMessageDialog(SynmvFrame.this, e.getMessage(), "invalide file format", JOptionPane.ERROR_MESSAGE);
					}
					
					if(config != null) {
						SynmvJob[] newJobs = new SynmvJob[jobs.length];
						for(int i = 0; i < jobs.length; i++) {
							jobs[i].unlink();
							newJobs[config[jobs[i].getID()-1]-1] = jobs[i];
						}
						
						jobs = newJobs;
						for(int i = 1; i < jobs.length; i++) {
							jobs[i].setPred(jobs[i-1]);
							jobs[i-1].setNext(jobs[i]);
						}
					}
				}
			}
		});

		this.pack();
	}	
	
}
