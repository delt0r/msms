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
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Simple line plots for theta_w etc.
 * 
 * @author bob
 * 
 */
public class ThetaPlot extends JPanel implements AxisModel {
	private static final Color[] COLORS = { Color.black, Color.red, Color.blue, Color.green };
	// very simply x, value(std);
	// must be ordered
	private double[][] data={{-.6,1,.1,2,.1},{.1,3,.1,7,.5},{.5,-7,.2,7.2,.1},{1.1,7,1,6,1},{2.3,0,7,6,1}};
	private double maxY;
	private double minY;
	private double minX;
	private double maxX;
	
	private int startParameter;
	private int endParameter;
	
	private String yLabel="Theta";
	private Axis axis;

	public ThetaPlot() {
		setBackground(Color.white);
		setData(data,0,1);
		axis=new Axis(this);
		setBorder(axis);
	}

	public void setData(double[][] data,int paramStart,int paramEnd) {
		if(data==null || data.length==0)
			return;
		this.data =data;
		double max =-Double.MAX_VALUE;
		double min =Double.MAX_VALUE;
		for (int i =0; i < data.length-2; i++) {
			for (int j =paramStart; j <= paramEnd; j++) {
				int jindex=j*2+1;
				max =Math.max(max, data[i][jindex]+data[i][jindex+1]);
				min =Math.min(min, data[i][jindex]-data[i][jindex+1]);
			}
		}
		maxY =max;
		minY =min;
		maxX =data[data.length - 1][0];
		minX =data[0][0];
		this.startParameter=paramStart;
		this.endParameter=paramEnd;
	}

	protected void paintComponent(Graphics g) {
		// System.out.println("Paint "+getBounds());
		super.paintComponent(g);
		Graphics2D g2d =(Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//g2d.setRenderingHint(RenderingHints., g2d)
		Insets insets =getInsets();
		double width =getWidth() - insets.left - insets.right;
		double height =getHeight() - insets.bottom - insets.top;

		double xs =width / (maxX - minX);
		double ys =height / (maxY - minY);
		ArrayList<GeneralPath> plots=new ArrayList<GeneralPath>();
		for(int param=startParameter;param<=endParameter;param++){
			int vIndex=param*2+1;
			//first the std... ignore if one is NaN
			boolean stdDraw=true;
			GeneralPath stdPathTop=new GeneralPath();
			GeneralPath stdPathBottom=new GeneralPath();
			GeneralPath plotPath=new GeneralPath();
			for (int i =0; i < data.length; i++) {
				double dataX=data[i][0];
				double value=data[i][vIndex];
				double std=data[i][vIndex+1];
				if(Double.isNaN(std)){
					stdDraw=false;
				}
				double pixX=(dataX-minX)*xs+insets.left;
				double pixY=height-(value-minY)*ys+insets.top;
				double stdTop=pixY-std*ys;
				double stdBottom=pixY+std*ys;
				if(i==0){
					plotPath.moveTo(pixX, pixY);
					stdPathTop.moveTo(pixX, stdTop);
					stdPathBottom.moveTo(pixX, stdBottom);
				}else{
					plotPath.lineTo(pixX, pixY);
					stdPathTop.lineTo(pixX, stdTop);
					stdPathBottom.lineTo(pixX, stdBottom);
				}
			}
			Color color=COLORS[((vIndex-1)/2)%COLORS.length];
			if(stdDraw){
				
				//System.out.println(Integer.toHexString(color.getRGB()&0x7FFFFFFF));
				Color acolor=new Color(color.getRGB()&0x0FFFFFFF, true);
				g2d.setColor(acolor);
				//close both paths by filling to the bottom of the graph 
				double h=getHeight();
				stdPathTop.lineTo((maxX-minX)*xs+insets.left, h);//cheat with overdraw
				stdPathTop.lineTo(insets.left,h);
				stdPathTop.closePath();
				
				stdPathBottom.lineTo((maxX-minX)*xs+insets.left,h);
				stdPathBottom.lineTo(insets.left,h);
				stdPathBottom.closePath();
				
				Area area=new Area(stdPathTop);
				area.subtract(new Area(stdPathBottom));
				g2d.fill(area);
			}
			plots.add(plotPath);
		}
		
		//finish by plots the plots.
		for(int i=0;i<plots.size();i++){
			Color color=COLORS[(i+startParameter)%COLORS.length];
			g2d.setColor(color);
			g2d.draw(plots.get(i));
		}

		g2d.dispose();
	}

	@Override
	public Rectangle2D getAxisBounds() {

		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public String getXlabel() {

		return "Position";
	}

	@Override
	public double[] getXTicks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getYlabel() {
		return yLabel;
	}

	@Override
	public double[] getYTicks() {
		return null;
	}

	public String getyLabel() {
		return yLabel;
	}

	public void setyLabel(String yLabel) {
		this.yLabel =yLabel;
	}

	
	
	public Axis getAxis() {
		return axis;
	}

	public void setAxis(Axis axis) {
		this.axis =axis;
		setBorder(axis);
	}

	public static void main(String[] args) {
		JFrame frame=new JFrame();
		ThetaPlot hist=new ThetaPlot();
		hist.setBorder(new Axis(hist));
		frame.add(hist);
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		System.out.println(Desktop.isDesktopSupported());
		try {
			Desktop.getDesktop().browse(new URI("http://www.mabs.at/"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
