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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

import at.mabs.util.Util;
/**
 * very simple Histogram. For SFS so we have the data in counted form. 
 * @author bob
 *
 */
public class SFSHistogram extends JPanel implements AxisModel{
	private int[] data={208300,108883,76812,60866,52117,46210,43734,42766,43662,47312,61480,182892,50485,32984,25485,20999,17409,14995,13124,11736,10746,9847,899};
	
	private int totalCount=Util.sum(data);

	private double maxY=Util.max(data);//zero is always the min
	
	private double minX=1;
	private double maxX=data.length;
	
	
	public SFSHistogram() {
		setMinimumSize(new Dimension(200,200));
		//setMaximumSize(new Dimension(1024,1024));
		setBackground(Color.white);
		setBorder(new Axis(this));
	}
	@Override
	protected void paintComponent(Graphics g) {
		//System.out.println("Paint "+getBounds());
		super.paintComponent(g);
		Graphics2D g2d=(Graphics2D)g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		double width=getWidth()-1;
		double height=getHeight();
		//adjust for borders... note that we will use a "axis" border
		Insets inset=getInsets();
		width-=inset.left+inset.right;
		height-=inset.bottom+inset.top;
		g2d.translate(inset.left, inset.top);
		
		double dx=width/data.length;
		double dy=height/maxY;
		//System.out.println(dx+"\t"+width+"\t"+data.length);
		for(int i=0;i<data.length;i++){
			double x=i*dx;
			double h=data[i]*dy;
			g2d.draw(new Rectangle2D.Double(x, height-h, dx, h));
		}
		g2d.dispose();
		
	}
	
	public void setData(int[] bins){
		this.data=bins;
		totalCount=Util.sum(data);
		maxY=Util.max(data);
		maxX=bins.length;
	}
	
	@Override
	public Rectangle2D getAxisBounds() {
	
		return new Rectangle2D.Double(minX-0.5,0,maxX,maxY);
	}
	
	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getXlabel() {	
		return "Allele frequency";
	}
	
	@Override
	public String getYlabel() {
	
		return "count";
	}
	
	@Override
	public double[] getXTicks() {
		int space=Math.max(data.length/10,1);
		double[] counts=new double[data.length/space];
		for(int i=0;i<counts.length;i++){
			counts[i]=(i+1)*space;
		}
		//counts[0]=1;
		return counts;
	}
	
	@Override
	public double[] getYTicks() {
		return null;
	}
	
	public static void main(String[] args) {
		JFrame frame=new JFrame();
		SFSHistogram hist=new SFSHistogram();
		hist.setBorder(new Axis(hist));
		frame.add(hist);
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
	}
	
	
}
