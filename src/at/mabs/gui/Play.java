/*
This code is licensed under the LGPL v3 or greater with the classpath exception, 
with the following additions and exceptions.  

packages cern.* have retained the original cern copyright notices.

packages at.mabs.cmdline and at.mabs.util.* 
have the option to be licensed under a BSD(simplified) or Apache 2.0 or greater  license 
in addition to LGPL. 

Note that you have permission to replace this license text to any of the permitted licenses. 

Main text for LGPL can be found here:
http://www.opensource.org/licenses/lgpl-license.php

For BSD:
http://www.opensource.org/licenses/bsd-license.php

for Apache:
http://www.opensource.org/licenses/apache2.0.php

classpath exception:
http://www.gnu.org/software/classpath/license.html
*/
package at.mabs.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cern.colt.Arrays;

import at.MSLike;
import at.ProgressControl;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.config.CommandLineMarshal;
import at.mabs.model.SampleConfiguration;
import at.mabs.stats.AlleleFrequencySpectrum;
import at.mabs.stats.MSStats;
import at.mabs.stats.StatsCollector;
import at.mabs.stats.StringStatsCollector;
import at.mabs.stats.ThetaEstimators;
import at.mabs.stats.ThetaW;
import at.mabs.util.NullPrintStream;
import at.mabs.util.Util;

/**
 * simple GUI cmd line for playing around. not ment to be for serious work. Use the command line
 * version for that.
 * 
 * @author bob
 * 
 */
public class Play extends JPanel {

	private JLabel thetaW =new JLabel("NA");
	private JLabel pi =new JLabel("NA");
	private JLabel tjd =new JLabel("NA");
	
	private JLabel error=new JLabel();

	private JTextArea commandLineText =new JTextArea();
	private boolean hasChanged=false;
	
	private LinkedList<String> history=new LinkedList<String>();
	private ListIterator<String> historyPos=null;
	
	private SFSHistogram histogram =new SFSHistogram();
	
	private ThetaPlot thetaPlots=new ThetaPlot();
	private ThetaPlot tjdPlot=new ThetaPlot();
	
	private JSFSPlot jafsplot=new JSFSPlot();
	private JSlider maxZSlider=new JSlider(0,1000,900);
	private JSlider graphSlider=new JSlider(0,1,0);
	
	private JButton historyForward =new JButton(">");
	private JButton historyBack =new JButton("<");
	
	
	private JButton run =new JButton("run");
	private JButton simple =new JButton("simple");
	private JButton migration =new JButton("migration");
	private JButton selection =new JButton("selection");
	private JButton complcated=new JButton("Complicated");
	
	private JButton tomatoSimple=new JButton("Tomato Simple");
	private JButton tomato=new JButton("Tomato");
	private JButton tomatoSelection=new JButton("Tomato Selection");
	
	
	private volatile boolean isRunning;
	private volatile boolean isCanceled;

	private JProgressBar progressBar =new JProgressBar();
	// private List dataCollectors

	private GridBagLayout gridBag =new GridBagLayout();
	
	private File currentDir=new File(".");

	public Play() {

		run.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				run(commandLineText.getText());
				if(!history.isEmpty() && history.getFirst().equals(commandLineText.getText()))
					return;
				history.addFirst(commandLineText.getText());
				if(history.size()>250)
					history.removeLast();
				historyPos=history.listIterator();
			}
		});
		
		historyForward.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(historyPos.hasPrevious())
					commandLineText.setText(historyPos.previous());
			}
		});
		
		historyBack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(historyPos.hasNext())
					commandLineText.setText(historyPos.next());
			}
		});

		simple.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				commandLineText.setText("-ms 20 1000 -t 100 -r 50 20");
			}
		});

		migration.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				commandLineText.setText("-ms 20 1000 -t 100 -r 50 20 -I 2 10 10 .25 ");
			}
		});

		selection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				commandLineText.setText("-ms 20 1000 -t 100 -r 500 20 -SAA 1000 -SaA 500 -N 10000 -SF 0 -Sp .5");
			}
		});
		
		complcated.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				commandLineText.setText("-N 1e4 -ms 80 1000 -t 1000\n" + 
						"-I 4 20 20 20 20 .2 -es 0 3 .52 -ej 0 4 5 -ej .042 5 3 -ej .052 3 2\n" + 
						"-ej .28 2 1 -en .44 1 .5 -en .052 2 .5 -n 2 .2 -n 3 .2 -n 5 .1 -r 1000\n" + 
						"20 -Smu .1 -SI .052 5 0 .01 0 0 0 -Sc 0 2\n" + 
						"1000  500 0 -Sp 0.5 ");
				
			}
		});
		
		tomatoSimple.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				commandLineText.setText("-ms 50 5000 -t 100 -r 50 20 -I 2 25 25 .25 -ej 1.4 1 2 -threads 2");
			}
		});
		

		tomato.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				commandLineText.setText("-ms 50 5000 -t 100 -r 50 20 -I 10 5 5 5 5 5 5 5 5 5 5 .25 " +
						" -ej .1 6 1 -ej .1 3 1 -ej .1 4 1 -ej .1 5 1" +
						" -ej .1 7 2 -ej .1 8 2 -ej .1 9 2 -ej .1 10 2" +
						" -ej 1.4 1 2 -threads 2");
			}
		});

		tomatoSelection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				commandLineText.setText("-ms 50 5000 -t 100 -r 50 20 -I 10 5 5 5 5 5 5 5 5 5 5 .25 " +
						" -ej .1 6 1 -ej .1 3 1 -ej .1 4 1 -ej .1 5 1" +
						" -ej .1 7 2 -ej .1 8 2 -ej .1 9 2 -ej .1 10 2" +
						" -ej 1.4 1 2 " +
						" -N 10000 -SAA 1000 -SaA 500 -SI 1.4 10 0 0 0 0 0 0 0 0 0 0 " +
						" -Smu .1 -threads 2");
			}
		});

		
		maxZSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				jafsplot.setMaxZRank((double)maxZSlider.getValue()/maxZSlider.getMaximum());
				jafsplot.repaint();
			}
		});
		
		graphSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				jafsplot.setCurrentGraph(graphSlider.getValue());
				jafsplot.repaint();
			}
		});
		
		error.setForeground(Color.RED);
		progressBar.setStringPainted(true);
		
		
		commandLineText.setColumns(40);
		commandLineText.setRows(5);
		commandLineText.setLineWrap(true);
		commandLineText.setWrapStyleWord(true);
		commandLineText.setMinimumSize(commandLineText.getPreferredSize());
		commandLineText.setText("-ms 20 1000 -t 100");
		commandLineText.setEditable(true);
		// commandLineText.setBackground(Color.white);
		commandLineText.setBorder(new EtchedBorder());
		setBorder(new EtchedBorder());

		GridBagConstraints gbc =new GridBagConstraints();
		gbc.insets =new Insets(5, 5, 5, 5);
		setLayout(gridBag);
		
		
		
	
		Box small=Box.createHorizontalBox();
		small.add(historyBack);
		small.add(historyForward);
		gbc.gridx =0;
		gbc.gridy=0;
		gbc.gridwidth=3;
		gbc.anchor=GridBagConstraints.WEST;
		addWidget(small, gbc);
		
		small=Box.createHorizontalBox();
		
		small.add(Box.createHorizontalStrut(10));
		small.add(simple);
		small.add(migration);
		small.add(selection);
		small.add(complcated);
		
		small.add(tomatoSimple);
		small.add(tomato);
		small.add(tomatoSelection);
		//gbc.gridx =1;
		//gbc.gridy=0;
		//gbc.gridwidth=2;
		gbc.anchor=GridBagConstraints.EAST;
		addWidget(small, gbc);
		gbc.anchor=GridBagConstraints.CENTER;
		gbc.weightx =1;
		
		JScrollPane scroll=new JScrollPane(commandLineText);
		scroll.setMinimumSize(commandLineText.getPreferredSize());
		scroll.setPreferredSize(new Dimension(400, 100));
		gbc.fill =GridBagConstraints.BOTH;
		gbc.gridx =0;
		gbc.gridwidth =3;
		gbc.gridy++;
		addWidget(scroll, gbc);

		gbc.gridy++;
		addWidget(error, gbc);
		
		
		gbc.gridy++;
		addWidget(progressBar, gbc);
		gbc.fill =GridBagConstraints.NONE;
		gbc.gridwidth=1;
		gbc.gridx=1;
		gbc.gridy++;
		addWidget(run, gbc);

		
		gbc.gridy++;
		gbc.gridx=0;
		gbc.gridwidth =1;
		addWidget(new JLabel("Wattersons Theta"), gbc);

		gbc.gridx =1;
		addWidget(new JLabel("Pi"), gbc);

		gbc.gridx =2;
		addWidget(new JLabel("Tajima's D"), gbc);
		
		gbc.gridy++;
		gbc.gridx=0;
		gbc.gridwidth =1;
		addWidget(thetaW, gbc);

		gbc.gridx =1;
		addWidget(pi, gbc);

		gbc.gridx =2;
		addWidget(tjd, gbc);

		Box box =Box.createHorizontalBox();
		//box.setBorder(BorderFactory.createCompoundBorder(new EtchedBorder(), new EmptyBorder(2, 2, 2, 2)));
		histogram.setMinimumSize(new Dimension(400, 400));
		// histogram.setPreferredSize(new Dimension(400,400));
		// pan.setLayout(new FlowLayout());
		box.add(histogram);
		box.setPreferredSize(new Dimension(300, 300));

		final  JTabbedPane tabPane=new JTabbedPane();
		tabPane.addTab("Site Frequency Spectrum", box);
		
		box =Box.createHorizontalBox();
		//box.setBorder(BorderFactory.createCompoundBorder(new EtchedBorder(), new EmptyBorder(2, 2, 2, 2)));
		thetaPlots.setMinimumSize(new Dimension(400, 400));
		// histogram.setPreferredSize(new Dimension(400,400));
		// pan.setLayout(new FlowLayout());
		thetaPlots.setyLabel("Theta_w(black) and Pi(red)");
		box.add(thetaPlots);
		box.setPreferredSize(new Dimension(300, 300));
		
		tabPane.addTab("Theta and Pi Estimates", box);
		
		box =Box.createHorizontalBox();
		//box.setBorder(BorderFactory.createCompoundBorder(new EtchedBorder(), new EmptyBorder(2, 2, 2, 2)));
		tjdPlot.setyLabel("Tajima's D");
		tjdPlot.setMinimumSize(new Dimension(400, 400));
		// histogram.setPreferredSize(new Dimension(400,400));
		// pan.setLayout(new FlowLayout());
		box.add(tjdPlot);
		box.setPreferredSize(new Dimension(300, 300));
		
		tabPane.addTab("Tajima's D", box);
		
		box =Box.createHorizontalBox();
		//box.setBorder(BorderFactory.createCompoundBorder(new EtchedBorder(), new EmptyBorder(2, 2, 2, 2)));
		
		jafsplot.setMinimumSize(new Dimension(400, 400));
		maxZSlider.setOrientation(JSlider.VERTICAL);
		box.add(jafsplot);
		box.add(maxZSlider);
		box.setPreferredSize(new Dimension(300, 300));
		
		Box downBox=Box.createVerticalBox();
		downBox.add(box);
		downBox.add(graphSlider);
		
		tabPane.addTab("JSFS plots", downBox);
		
		gbc.gridx =0;
		gbc.gridy++;
		gbc.gridwidth =3;
		gbc.weighty =1;
		gbc.fill =GridBagConstraints.BOTH;
		addWidget(tabPane, gbc);
		
		
		JButton pngOut =new JButton("Export PNG");
		pngOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser =new JFileChooser(currentDir);
				int value =chooser.showSaveDialog(Play.this);
				if (value == JFileChooser.CANCEL_OPTION)
					return;
				File file =chooser.getSelectedFile();
				if(file.exists()){
					if(JOptionPane.showConfirmDialog(Play.this, "Save over file "+file.getName(),"Warning",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.OK_OPTION)
						return;
				}
				try {
					Component comp=tabPane.getSelectedComponent();
					BufferedImage image =new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_ARGB);
					comp.paint(image.getGraphics());
					ImageIO.write(image, "png", file);
					currentDir=file.getParentFile();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		
		
		gbc.gridy++;
		gbc.gridwidth =1;
		gbc.weighty =0;
		gbc.fill =GridBagConstraints.NONE;
		addWidget(pngOut, gbc);
		
		JButton quit =new JButton("quit");
		quit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		
		
		gbc.gridx=2;
		gbc.fill =GridBagConstraints.NONE;
		addWidget(quit, gbc);
		
		
		JButton help =new JButton("help");
		help.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {			
				try{
					URI uri=new URI("http://www.mabs.at/ewing/msms/msmsplay.shtml");
					Desktop.getDesktop().browse(uri);
				}catch(Exception ex){
					JOptionPane.showMessageDialog(Play.this, "Could not open browser.\n Please go to http://www.mabs.at/ewing/msms/msmsplay.shtml");
					ex.printStackTrace();
				}
			}
		});
		
		gbc.gridx=1;
		gbc.fill =GridBagConstraints.NONE;
		addWidget(help, gbc);
		
	}

	private void addWidget(JComponent c, GridBagConstraints gbc) {
		gridBag.setConstraints(c, gbc);
		add(c);

	}

	public void run(String s) {
		if (isRunning) {
			isCanceled =true;
			return;
		}
		StringTokenizer st =new StringTokenizer(s);

		final String[] args =new String[st.countTokens()];
		int counter=0;
		while(st.hasMoreElements()){
			args[counter++]=st.nextToken();
		}
		
		SampleConfiguration sampleConfig;
		
		CommandLineMarshal msmsparser = new CommandLineMarshal();
		try {
			CmdLineParser<CommandLineMarshal> marshel =CommandLineMarshal.getCacheParser();// new CmdLineParser<CommandLineMarshal>(msmsparser);
			marshel.processArguments(args,msmsparser);
			sampleConfig = msmsparser.getSampleConfig();
			
		} catch (Exception e) {
			error.setText("Error:"+e.getMessage());
			throw new RuntimeException(e);
		}
		
		final int repsFinal=msmsparser.getRepeats();
		final ProgressControl control =new ProgressControl() {
			AtomicInteger iterationCompleted=new AtomicInteger();
			@Override
			public void setReps(int reps) {
				//noop
			}
			@Override
			public void iterationComplete() {
				progressBar.setValue(iterationCompleted.incrementAndGet());
				
			}
			@Override
			public boolean isCanceled() {
				return isCanceled;
			}
			@Override
			public void error(Exception e) {
				//System.out.println("GotError:"+e);
				error.setText("Error:"+e.getMessage());
				e.printStackTrace();
				throw new RuntimeException(e.getMessage());	
			}
		};

		isRunning =true;
		isCanceled =false;
		run.setText("Cancel");
		progressBar.setMaximum(repsFinal);
		error.setText("");
		final SampleConfiguration sampleConfigFinal =sampleConfig;

		Thread thread =new Thread() {
			public void run() {
				
				try {
					AlleleFrequencySpectrum afsCollector =new AlleleFrequencySpectrum(true, true, true, sampleConfigFinal);
					ThetaEstimators te =new ThetaEstimators(sampleConfigFinal, .05, .05, true);

					List<StringStatsCollector> collectors =new ArrayList<StringStatsCollector>();
					collectors.add(afsCollector);
					collectors.add(te);
					collectors.add(new MSStats());
					

					MSLike.main(args, collectors, System.out, control);//new NullPrintStream(), control);

					int[][][] jafsData=afsCollector.getCumulantJAFS();
					
					if(jafsData.length>0){
						jafsplot.setData(jafsData, sampleConfigFinal.getDemeCount());
						maxZSlider.setValue((int)(0.9*maxZSlider.getMaximum()));
						graphSlider.setMaximum(jafsplot.getGraphCount()-1);
						jafsplot.setEnabled(true);
					}else{
						jafsplot.setEnabled(false);
					}
					
					int[] data =afsCollector.getGlobalAFS();
					
					histogram.setData(data);
					
					double[][] plotData=te.getWindowedData();
					for(double[] row:plotData){
						System.out.println(Arrays.toString(row));
					}
					System.out.println();
					thetaPlots.setData(plotData,0,1);
					tjdPlot.setData(plotData, 2, 2);
					
					thetaW.setText(Util.defaultFormat.format( te.getThetaW())+"("+Util.defaultFormat.format( te.getThetaWStd())+")");
					pi.setText(Util.defaultFormat.format( te.getPi())+"("+Util.defaultFormat.format( te.getPiStd())+")");
					tjd.setText(Util.defaultFormat.format( te.getTjD())+"("+Util.defaultFormat.format( te.getTjDStd())+")");
					
				} catch (Exception e) {
					control.error(e);
					//e.printStackTrace();
				}finally{
					isRunning =false;
					run.setText("run");
				}
				repaint();
			}
		};

		thread.start();

	}

	public static void main(String[] args) {
		System.err.println("Warning: use MSMSPlay to launch");
		JFrame frame =new JFrame("MSMSPlay");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Play play =new Play();
		frame.add(play);
		frame.setSize(600,800);
		frame.setVisible(true);
	}

}
