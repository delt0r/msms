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

import java.awt.geom.Rectangle2D;

/**
 * 
 * @author bob
 *
 */
// I don't like lots of little classes all over the place for simple things...
// But i just don't have a better idea right now. 
public interface AxisModel {
	
	public Rectangle2D getAxisBounds();
	
	public double[] getXTicks();
	public double[] getYTicks();
	
	public String getXlabel();
	public String getYlabel();
	public String getTitle(); 

}
