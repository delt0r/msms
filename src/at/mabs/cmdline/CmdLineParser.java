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
package at.mabs.cmdline;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import at.mabs.config.CommandLineMarshal;

/**
 * parser based on reflection and using the CmdLine annotation.
 * <p>
 * Note that types are based on method argument counts. Not types. So if you
 * have two different -S options bound to 2 different methods, then you must
 * have different numbers of arguments for each.
 * 
 * @author bob
 * 
 */
public class CmdLineParser<T> {
	public T instance;
	public HashMap<String, List<CmdLineMethod>> switches = new HashMap<String, List<CmdLineMethod>>();

	public HashSet<String> required = new HashSet<String>();
	public int wrapWidth = 80;

	public CmdLineParser(T instance) throws CmdLineBuildException {
		init(instance);
	}

	private void init(T instance) throws CmdLineBuildException {
		this.instance = instance;

		required.clear();
		switches.clear();

		Class objectType = instance.getClass();
		Method[] methods = objectType.getMethods();
		for (Method m : methods) {
			CLNames cmdData = (CLNames) m.getAnnotation(CLNames.class);
			if (cmdData == null)
				continue;
			String[] names = cmdData.names();
			for (String name : names) {
				CmdLineMethod clm = new CmdLineMethod(cmdData, m);
				if (clm.cmdData.required()) {
					required.add(clm.cmdData.names()[0]);
				}
				List<CmdLineMethod> list = switches.get(name);
				if (list == null) {
					list = new ArrayList<CmdLineMethod>();
					switches.put(name, list);
				}
				list.add(clm);
			}
		}

		// now sort and check
		for (List<CmdLineMethod> methodList : switches.values()) {
			ArgLengthCompare compare = new ArgLengthCompare();
			Collections.sort(methodList, compare);
			// System.out.println(methodList);
			if (compare.isError) {
				System.out.println(switches);
				throw new CmdLineBuildException("Two methods annotated with the same name with the same number of arguments:\n\t" + compare.methodA + "\n\t"
						+ compare.methodB);
			}

			CLDescription des = null;
			String[] names = null;
			for (CmdLineMethod clm : methodList) {
				if (des != null && clm.description != null && !des.value().equals(clm.description.value())) {
					throw new CmdLineBuildException("Description for same switches should be equal " + clm);
				}
				if (names != null && !Arrays.equals(names, clm.cmdData.names())) {
					throw new CmdLineBuildException("Names/Alisas for same switches should be equal " + clm);
				}
				if (names == null)
					names = clm.cmdData.names();
				if (des == null)
					des = clm.description;
			}
		}
	}

	public T processArguments(String[] args) throws CmdLineParseException {
		if(instance instanceof InitFinishParserObject){
			((InitFinishParserObject)instance).init();
		}
		// first we break up the args at switches.
		List<DelimitedArgs> delimited = new ArrayList<DelimitedArgs>();
		List<String> current = new ArrayList<String>();
		HashSet<String> requiredTest = new HashSet<String>(required);
		String name = "";
		for (int i = 0; i < args.length; i++) {
			// System.out.println("Arg:"+args[i]+"\t"+i+"\t"+current+"\t"+name);
			if (switches.containsKey(args[i])) {
				if (name.length() != 0 || !current.isEmpty()) {
					delimited.add(new DelimitedArgs(name, current));
					current = new ArrayList<String>();
				}
				name = args[i];
			} else {
				current.add(args[i]);
			}
			// System.out.println("ArgE:"+args[i]+"\t"+i+"\t"+current+"\t"+name);
		}
		//if (!current.isEmpty())
			delimited.add(new DelimitedArgs(name, current));
		Collections.sort(delimited, new RankCompare());// make sure everything
														// gets invoked in the
		// right order
		for (DelimitedArgs da : delimited) {
			// System.out.println("Invoke:"+da.method);
			requiredTest.remove(da.method.cmdData.names()[0]);
			da.invoke();
		}
		if (!requiredTest.isEmpty()) {
			throw new CmdLineParseException("Required Options missing:" + requiredTest);
		}
		if(instance instanceof InitFinishParserObject){
			((InitFinishParserObject)instance).finished();
		}
		return instance;
	}

	public String longUsage() {
		StringBuilder sb = new StringBuilder(shortUsage());
		sb.append("\n\tDetailed Option Descriptions:\n");
		//System.out.println(switches);
		HashSet<String> done = new HashSet<String>();
		List<String> sortedSwitches = new ArrayList<String>(switches.keySet());
		Collections.sort(sortedSwitches);
		for (String s : sortedSwitches) {
			List<CmdLineMethod> list = switches.get(s);
			CLNames clnames = list.get(0).cmdData;
			String[] names = clnames.names();
			if (done.contains(names[0]))
				continue;
			done.add(names[0]);
			longUsage(s, sb);
		}

		return sb.toString();
	}

	private void longUsage(String name, StringBuilder sb) {
		List<CmdLineMethod> list = switches.get(name);
		CLNames clnames = list.get(0).cmdData;
		String[] names = clnames.names();
		appendShortUsage(list, sb, false);
		sb.append('\n');
		if (names.length > 1) {
			sb.append("\tAlias:");
		}
		for (int i = 1; i < names.length; i++) {
			sb.append(names[i] + "  ");
		}
		sb.setLength(sb.length() - 1);
		if (clnames.required()) {
			sb.append("\n\tRequired");
		}

		for (CmdLineMethod clm : list) {
			if (clm.description != null) {
				sb.append("\n");
				sb.append(wrap(clm.description.value(), "\t", wrapWidth));
				sb.append("\n");
				break;
			}
		}
		sb.append("\n");
	}

	public String longUsage(String name) {
		if (name == null)
			return "";
		StringBuilder sb = new StringBuilder();
		longUsage(name, sb);
		return sb.toString();
	}

	/*
	 * wrap when over 80 chars wide...
	 */
	private String wrap(String s, String linePrefix, int wrap) {
		String wraped = "";
		int p = 0;
		while (p < s.length() - 1) {
			int pinc = Math.min(s.length() - 1, p + wrap);
			int space = s.lastIndexOf(' ', pinc);
			if (space > p && pinc == p + wrap)
				pinc = space;
			wraped += '\n' + linePrefix + s.substring(p, pinc + 1).trim();
			p = pinc;
		}
		if(wraped.length()==0)
			return "";
		return wraped.substring(1);// remove the leading \n

	}

	public int getWrapWidth() {
		return wrapWidth;
	}

	public void setWrapWidth(int wrapWidth) {
		this.wrapWidth = wrapWidth;
	}

	public String shortUsage() {
		HashSet<String> done = new HashSet<String>();
		StringBuilder sb = new StringBuilder();
		List<String> sortedSwitches = new ArrayList<String>(switches.keySet());
		Collections.sort(sortedSwitches);
		int lastlinebrake = 0;
		for (String s : sortedSwitches) {
			List<CmdLineMethod> list = switches.get(s);
			CLNames clnames = list.get(0).cmdData;
			String[] names = clnames.names();
			if (done.contains(names[0]))
				continue;
			done.add(names[0]);
			int index = sb.length();
			appendShortUsage(list, sb, true);
			if (sb.length() - lastlinebrake > wrapWidth) {
				sb.insert(index, "\n\t");
				lastlinebrake = index;
			}
		}
		// sb.setLength(sb.length()-3);
		return sb.toString();
	}

	/*
	 * lots of state machine logic. We do a bit of removing things after the
	 * fact.
	 */
	private void appendShortUsage(List<CmdLineMethod> list, StringBuilder sb, boolean brakets) {
		CLNames clnames = list.get(0).cmdData;
		String[] names = clnames.names();
		if (!clnames.required() && brakets) {
			sb.append('[');
		}
		sb.append(names[0]).append('|');

		sb.deleteCharAt(sb.length() - 1);
		sb.append(' ');
		int bIndex = -1;
		if (list.size() > 1) {
			sb.append('{');
			bIndex = sb.length() - 1;
		}
		int renderCount = 0;

		for (CmdLineMethod clm : list) {
			if (clm.usage != null && clm.usage.value().length() == 0)
				continue;// supress usage
			renderCount++;
			if (clm.usage != null) {
				sb.append(clm.usage.value()).append("}|{");
			} else {
				sb.append(arrayToString(clm.args)).append("}|{");
			}
		}
		if (renderCount == 0) {

		} else if (list.size() > 1 && renderCount == 1) {
			// remove brackets..
			sb.deleteCharAt(bIndex);
			sb.setLength(sb.length() - 3);
		} else if (list.size() > 1) {
			sb.setLength(sb.length() - 2);
		} else {
			sb.setLength(sb.length() - 3);
		}

		if (!clnames.required() && brakets) {
			sb.append(']');
		}
		sb.append(" ");
	}

	private String arrayToString(Class[] array) {
		if (array.length == 0)
			return "";
		String s = "";
		for (Class o : array) {
			if (o.isArray()) {
				s += o.getComponentType().getSimpleName() + "... ";
			} else {
				s += o.getSimpleName() + " ";
			}
		}
		return s.substring(0, s.length() - 1);
	}

	class CmdLineMethod {
		final CLNames cmdData;
		final Method method;
		final int argCount;// Integer.MAX_VALUE if there is an array quantity
		final Class[] args;
		final int arrayIndex;
		final CLDescription description;
		final CLUsage usage;

		public CmdLineMethod(CLNames cmdData, Method method) throws CmdLineBuildException {
			this.cmdData = cmdData;
			this.method = method;

			description = method.getAnnotation(CLDescription.class);
			usage = method.getAnnotation(CLUsage.class);

			args = method.getParameterTypes();
			int count = 0;
			int index = -1;
			// now check for an array.
			for (int i = 0; i < args.length; i++) {
				if (args[i].isArray() && count == 0) {
					count = -1;
					index = i;
				} else if (args[i].isArray() && count != 0) {
					throw new CmdLineBuildException("Can only have one array parameter per annotated method:" + method);
				}
			}
			if (count == -1) {
				argCount = Integer.MAX_VALUE;// args.length;
			} else {
				argCount = args.length;
			}
			arrayIndex = index;
		}

		@Override
		public String toString() {

			return "Switch " + cmdData.names()[0] + " " + Arrays.toString(args);
		}

	}

	class DelimitedArgs {
		final String name;
		final List<String> args;
		final CmdLineMethod method;

		public DelimitedArgs(String n, List<String> a) throws CmdLineParseException {
			this.name = n;
			this.args = a;
			List<CmdLineMethod> list = switches.get(name);
			if (list == null) {
				throw new CmdLineParseException("invalid switch:" + name, name, a);
			}
			assert list != null;
			CmdLineMethod clm = null;
			for (CmdLineMethod cm : list) {
				if (cm.argCount == a.size() || cm.argCount == Integer.MAX_VALUE) {
					clm = cm;
					break;
				}
			}
			// System.out.println(a);
			if (clm == null) {
				throw new CmdLineParseException(name + " has an incorrect number of arguments.", name, a);
			}
			method = clm;
		}

		public void invoke() throws CmdLineParseException {
			// we need to see if there is an array in there somewhere...
			int arrayRank = method.arrayIndex;
			int otherArgCount = method.args.length;
			if (arrayRank >= 0)
				otherArgCount--;

			int arrayArgs = args.size() - otherArgCount;

			Class[] types = method.args;
			Object[] invokeArgs = new Object[types.length];
			int argIndex = 0;
			int typeIndex = 0;
			while (argIndex < args.size()) {
				Class c = types[typeIndex];
				Object o = null;
				if (c.isArray()) {
					Class atype = c.getComponentType();
					o = Array.newInstance(atype, arrayArgs);
					for (int a = 0; a < arrayArgs; a++) {
						Object ad = null;
						try {
							ad = parseObject(atype, args.get(argIndex));
						} catch (Exception e) {
							throw new CmdLineParseException("Error parsing " + name + " at argument " + (argIndex + 1), name, args, e);
						}
						Array.set(o, a, ad);
						argIndex++;
					}
				} else {
					try {
						o = parseObject(c, args.get(argIndex));
					} catch (Exception e) {
						throw new CmdLineParseException("Error parsing " + name + " at argument " + (argIndex + 1), name, args, e);
					}
					argIndex++;
				}
				invokeArgs[typeIndex] = o;
				typeIndex++;
			}
			try {
				method.method.invoke(instance, invokeArgs);
			} catch (InvocationTargetException ite) {
				Throwable t = ite.getTargetException();
				throw new CmdLineParseException(t.getMessage(), name, args, t);
			} catch (Exception e) {
				throw new CmdLineParseException("Error invoking " + name, name, args, e);
			}
		}
	}

	private static Object parseObject(Class type, String value) throws Exception {
		if (type.equals(String.class)) {
			return value;
		}
		if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
			return new Integer(value);
		}
		if (type.equals(Long.class) || type.equals(Long.TYPE)) {
			return new Long(value);
		}
		if (type.equals(Float.class) || type.equals(Float.TYPE)) {
			return new Float(value);
		}
		if (type.equals(Double.class) || type.equals(Double.TYPE)) {
			return new Double(value);
		}
		if (type.equals(Short.class) || type.equals(Short.TYPE)) {
			return new Short(value);
		}
		Constructor constructor = type.getConstructor(String.class);
		return constructor.newInstance(value);
	}

	class ArgLengthCompare implements Comparator<CmdLineMethod> {
		boolean isError;
		CmdLineMethod methodA;
		CmdLineMethod methodB;

		@Override
		public int compare(CmdLineMethod o1, CmdLineMethod o2) {
			if (o1.argCount > o2.argCount) {
				return 1;
			}
			if (o1.argCount < o2.argCount) {
				return -1;
			}
			// should be an error condtion.
			isError = true;
			methodA = o1;
			methodB = o2;
			return 0;
		}
	}

	class RankCompare implements Comparator<DelimitedArgs> {
		@Override
		public int compare(DelimitedArgs o1, DelimitedArgs o2) {

			return o1.method.cmdData.rank() - o2.method.cmdData.rank();
		}
	}

	public T getObjectInstance() {

		return this.instance;
	}
}
