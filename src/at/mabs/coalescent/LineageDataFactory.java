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
package at.mabs.coalescent;

import at.mabs.util.PartialSumTreeElement;

/**
 * simple factory to seperate coalescent objects from segment objects... or something like that.
 * @author bob
 *
 */
public interface LineageDataFactory<T extends PartialSumTreeElement> {
	//if too many leafs are created we throw an exception. This says to start again. 
	public void reset();
	public T createLineageData(double time);
}
