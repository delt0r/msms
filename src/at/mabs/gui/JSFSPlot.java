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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JPanel;

public class JSFSPlot extends JPanel implements AxisModel {
	private int[][][] jafsData;
	// private int demeCount;
	// private int[] sampleConfig;
	private int currentGraph;

	private int maxZ=1000;
	private ArrayList<Integer> zRanks=new ArrayList<Integer>();
	
	
	private String[] xlabs={"no data"};
	private String[] ylabs={"no data"};
	
	public JSFSPlot() {
		setBackground(Color.white);
		setBorder(new Axis(this));
	}

	public void setCurrentGraph(int currentGraph) {
		this.currentGraph =currentGraph;
	}
	
	public int getGraphCount() {
		return jafsData.length;
	}
	
	
	
	public void setData(int[][][] data, int demeCount) {
		if(data.length==0 || demeCount==1)
			return;
		assert data.length * 2 == (demeCount * (demeCount - 1));
		// first set deme labels...
		xlabs =new String[data.length];
		ylabs =new String[data.length];
		int counter =0;
		for (int i =0; i < demeCount; i++) {
			for (int j =i + 1; j < demeCount; j++) {
				xlabs[counter] ="Allele Freq. in Deme " + (i + 1);
				ylabs[counter] ="Allele Freq. in Deme " + (j + 1);
				counter++;
			}
		}
		jafsData =data;
		maxZ=-Integer.MAX_VALUE;
		zRanks.clear();
		for(int i=0;i<data.length;i++){
			for(int j=0;j<data[i].length;j++){
				for(int k=0;k<data[i][j].length;k++){
					maxZ=Math.max(maxZ, data[i][j][k]);
					zRanks.add(data[i][j][k]);
				}
			}
		}
		Collections.sort(zRanks);
		setMaxZRank(.9);
		System.out.println("Max:"+maxZ);
		
	}

	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		Graphics2D g2d =(Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if(jafsData==null || jafsData.length==0 || !isEnabled())
			return;
		Insets insets =getInsets();
		double width =getWidth() - insets.left - insets.right;
		double height =getHeight() - insets.bottom - insets.top;
		
		
		
		int[][] data=jafsData[currentGraph];
		
		double dx=width/data.length;
		double dy=height/data[0].length;
		
		for(int i=0;i<data.length;i++){
			for(int j=0;j<data[i].length;j++){
				int value=data[i][j];
				if(value==0)
					continue;
				value=Math.min(value, maxZ);
				Color color=Color.getHSBColor((float)value/(maxZ), 1f, 1f);
				g2d.setColor(color);
				Shape s=new Rectangle2D.Double(i*dx+insets.left, height-dy*j+insets.top-dy, dx, dy);
				g2d.fill(s);
				g2d.draw(s);
			}
		}
	}
	
	@Override
	public Rectangle2D getAxisBounds() {
		if(jafsData==null || jafsData.length==0)
			return new Rectangle2D.Double(0,0,0,0);
		return new Rectangle2D.Double(0, 0, jafsData[currentGraph].length, jafsData[currentGraph][0].length);
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public double[] getXTicks() {
		return null;
	}

	@Override
	public String getXlabel() {
		return xlabs[currentGraph];
	}

	@Override
	public double[] getYTicks() {
		return null;
	}

	@Override
	public String getYlabel() {
		return ylabs[currentGraph];
	}

	public void setMaxZ(int maxZ) {
		this.maxZ =maxZ;
	}
	
	public void setMaxZRank(double rank){
		int index=(int)Math.floor(rank*zRanks.size());
		index=Math.min(zRanks.size()-1, index);
		maxZ=zRanks.get(index);
		System.out.println("NewMaxZ:"+maxZ);
		
	}
	
	public int getMaxZ() {
		return maxZ; 
	} 
	
}
