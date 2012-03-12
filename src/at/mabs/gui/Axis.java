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

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.border.Border;

import at.mabs.util.Util;

/**
 * paints a nice set of axies --note that insets can change a bit.. don't 
 * @author bob
 *
 */
public class Axis implements Border {
	private int xaxisSize=60;
	private int yaxisSize=70;
	private int axisSpace=5;
	private int labelSpace=10;
	private int tickLength=5;
	private int otherBorder=8;
	private AxisModel model;
	
	private NumberFormat format=new DecimalFormat("#0.#E0");
	
	
	public Axis(AxisModel model) {
		this.model=model;
	}
	@Override
	public Insets getBorderInsets(Component c) {
		
		return new Insets(otherBorder, yaxisSize, xaxisSize, otherBorder);
	}

	@Override
	public boolean isBorderOpaque() {
		return false;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d=(Graphics2D)g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//g2d.setClip(0, otherBorder, width-otherBorder, height);
		//xaxis:
		double ypos=height-xaxisSize+axisSpace;
		//xaxis
		g2d.draw(new Line2D.Double(yaxisSize,ypos,width-otherBorder,ypos));
		
		Rectangle2D bounds=model.getAxisBounds();
		
		double[] ticks=model.getXTicks();
		if(ticks==null){
			ticks=defaultTicks(bounds.getMinX(), bounds.getMaxX());
		}
		double xscale=(width-yaxisSize-otherBorder)/bounds.getWidth();
		//System.out.println("xscale:"+xscale+"\t"+bounds.getWidth()+" w:"+width+"\t"+c.getWidth());
		for(double sx:ticks){
			double xpos=(sx-bounds.getMinX())*xscale+yaxisSize;
			g2d.draw(new Line2D.Double(xpos,ypos,xpos,ypos+tickLength));
			String label=toFormattedString(sx);
			Rectangle2D rec =g2d.getFontMetrics().getStringBounds(label, g2d);
			g2d.drawString(label, (float)(xpos-rec.getCenterX()), (float)(ypos+labelSpace+rec.getHeight()));
		}
		String xlab=model.getXlabel();
		Rectangle2D rec =g2d.getFontMetrics().getStringBounds(xlab, g2d);
		g2d.drawString(xlab, (float)((width-yaxisSize)/2-rec.getCenterX()+yaxisSize), (float)(ypos+2*labelSpace+2*rec.getHeight()));
		
		//now y axis
		
		double xpos=yaxisSize-axisSpace;
		ypos=height-xaxisSize;
		//xaxis
		g2d.draw(new Line2D.Double(xpos,otherBorder,xpos,ypos));
		
		
		ticks=model.getYTicks();
		if(ticks==null){
			ticks=defaultTicks(bounds.getMinY(), bounds.getMaxY());
		}
		double yscale=(height-xaxisSize-otherBorder)/bounds.getHeight();
		
		for(double sy:ticks){
			double ly=height-xaxisSize-(sy-bounds.getMinY())*yscale;
			//System.out.println(sy+"\t"+ly);
			g2d.draw(new Line2D.Double(xpos,ly,xpos-tickLength,ly));
			String label=toFormattedString(sy);
			rec =g2d.getFontMetrics().getStringBounds(label, g2d);
			g2d.drawString(label, (int)(xpos-rec.getWidth()-labelSpace), (int)(ly-rec.getCenterY()));
		}
		//use translate to get it nice and centered.. then rotate. 
		String ylab=model.getYlabel();
		rec =g2d.getFontMetrics().getStringBounds(ylab, g2d);
		
		double dy=(height-xaxisSize-otherBorder)/2+rec.getCenterX()+otherBorder;
		double dx=yaxisSize-axisSpace-labelSpace*2-rec.getHeight()*2;
		g2d.translate(dx, dy);
		g2d.rotate(-Math.PI/2);
		g2d.drawString(ylab,0,0);
		
		g2d.dispose();
	}
	
	private double[] defaultTicks(double min,double max){
		double log10=Math.ceil(Math.log10(max));
		double pten=Math.pow( 10,log10);
		//System.out.println("log10:"+log10+"\t"+pten);
		//now we get 10 of those...
		double d=pten;
		while(((max-min)/d)<2)
			d/=5;
		double[] ticks=new double[(int)Math.ceil((max-min)/d)];
		//need to find the starting "index"
		int start=(int)Math.ceil(min/d);
		
		for(int i=0;i<ticks.length;i++){
			ticks[i]=(start+i)*d;
		}
		return ticks;
	}
	
	
	
	public int getXaxisSize() {
		return xaxisSize;
	}
	public void setXaxisSize(int xaxisSize) {
		this.xaxisSize =xaxisSize;
	}
	public int getYaxisSize() {
		return yaxisSize;
	}
	public void setYaxisSize(int yaxisSize) {
		this.yaxisSize =yaxisSize;
	}
	public int getAxisSpace() {
		return axisSpace;
	}
	public void setAxisSpace(int axisSpace) {
		this.axisSpace =axisSpace;
	}
	public int getLabelSpace() {
		return labelSpace;
	}
	public void setLabelSpace(int labelSpace) {
		this.labelSpace =labelSpace;
	}
	public int getTickLength() {
		return tickLength;
	}
	public void setTickLength(int tickLength) {
		this.tickLength =tickLength;
	}
	private String toFormattedString(double d){
		
		if((d-(int)d)==0 && Math.abs(d)<1000){
			return ""+(int)d;
		}
		return format.format(d);
	}

}
