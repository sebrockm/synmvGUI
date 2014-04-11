import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


@SuppressWarnings("serial")
public class SynmvFrame extends JFrame {

	private final JPanel jobcontainer = new JPanel();
	private final JLabel label = new JLabel();
	private final JButton button = new JButton("load");
	private SynmvJob[] jobs = new SynmvJob[0];
	
	private final JScrollPane scroll = new JScrollPane();
	private final JFileChooser chooser = new JFileChooser();
	
	private SynmvJob[] readJobsFromFile(String filename) throws FileNotFoundException {
		SynmvJob[] retjobs = null;
		BufferedReader buf = new BufferedReader(new FileReader(filename));
		
		try {
			String line;
			do {
				line = buf.readLine();
				if(line == null)
					return null;
				line = line.trim();
			} while(line.isEmpty() || line.charAt(0) == '#');
			StringTokenizer tok = new StringTokenizer(line);
			
			int m = Integer.parseInt(tok.nextToken());
			int n = Integer.parseInt(tok.nextToken());
			retjobs = new SynmvJob[n];
			
			int i = 0;
			while(i < n && (line = buf.readLine()) != null) {
				line = line.trim();
				if(line.isEmpty() || line.charAt(0) == '#')
					continue;
				
				tok = new StringTokenizer(line);
				float[] times = new float[m];
				for(int j = 0; j < m; j++) {
					times[j] = Float.parseFloat(tok.nextToken());
				}
				retjobs[i] = new SynmvJob(jobcontainer, times);
				if(i > 0) {
					retjobs[i].setPred(retjobs[i-1]);
					retjobs[i-1].setNext(retjobs[i]);
				}
				i++;
			}
			if(i < n) {
				System.err.println("Nur " + i + " statt " + n + " jobs in " + filename + " gefunden!");
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
	
	public SynmvFrame() throws FileNotFoundException {
		super();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(1000, 500);
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
					job.setPositions();
				}
			}
		});

		
		scroll.setViewportView(jobcontainer);
		this.add(scroll);

		
		jobcontainer.add(button);
		button.setLocation(300, 400);
		button.setText("load");
		button.setSize(100, 40);
		jobcontainer.add(label);
		label.setLocation(20, 430);
		label.setSize(280, 10);
		
		chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));


		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					int ret = chooser.showOpenDialog(SynmvFrame.this);
					if(ret == JFileChooser.APPROVE_OPTION) {
						SynmvJob[] tmp = null;
						try {
							tmp = readJobsFromFile(chooser.getSelectedFile().getAbsolutePath());
						} catch (IOException e1) {
							e1.printStackTrace();
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
											job.setPositions();
											cmax = Math.max(cmax, job.getOffset(job.getMachineCount()-1) + job.getLen(job.getMachineCount()-1));
										}
									}
									label.setText("Cmax: " + cmax);
								}
								
							});
						}
					}
				}
			}
		});
		
		

	}	
	
}
