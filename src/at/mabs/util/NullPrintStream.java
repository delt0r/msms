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
package at.mabs.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class NullPrintStream extends PrintStream {

	public NullPrintStream() {
		super(new OutputStream(){

			@Override
			public void write(int b) throws IOException {
				
			}
			
		});
		
	}

	@Override
	public PrintStream append(char c) {
		
		return this;
	}

	@Override
	public PrintStream append(CharSequence csq, int start, int end) {
		
		return this;
	}

	

	@Override
	public boolean checkError() {
		return false;
	}

	@Override
	protected void clearError() {
		
	}

	@Override
	public void close() {
		
	}

	@Override
	public void flush() {
		
	}

	@Override
	public void print(boolean b) {
		
	}

	@Override
	public void print(char c) {
		
	}

	@Override
	public void print(char[] s) {
		
	}

	@Override
	public void print(double d) {
		
	}

	@Override
	public void print(float f) {
		
	}

	@Override
	public void print(int i) {
		
	}

	@Override
	public void print(long l) {
		
	}

	@Override
	public void print(Object obj) {
		
	}

	@Override
	public void print(String s) {
		
	}

	
	@Override
	public void println() {
		
	}

	@Override
	public void println(boolean x) {
		
	}

	@Override
	public void println(char x) {
		
	}

	@Override
	public void println(char[] x) {
		
	}

	@Override
	public void println(double x) {
		
	}

	@Override
	public void println(float x) {
		
	}

	@Override
	public void println(int x) {
		
	}

	@Override
	public void println(long x) {
		
	}

	@Override
	public void println(Object x) {
		
	}

	@Override
	public void println(String x) {
		
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		
	}

	@Override
	public void write(int b) {
		
	}

	@Override
	public void write(byte[] b) throws IOException {
		
	}

	@Override
	public PrintStream append(CharSequence csq) {
		return this;
	}

	@Override
	public PrintStream format(Locale l, String format, Object... args) {
		return this;
	}

	@Override
	public PrintStream format(String format, Object... args) {
		return this;}

	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		return this;
	}

	@Override
	public PrintStream printf(String format, Object... args) {
		
		return this;
	}

	

}
