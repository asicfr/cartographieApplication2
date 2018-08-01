package com.carto.applicarto.utils2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

public class JDIEventMonitor {

	private String packageFilter;
	// exclude events generated for these classes
	private final String[] excludes = { "java.*", "javax.*", "sun.*", "com.sun.*", "jdk.internal.*",
			// "com.mysql.*",
			// "org.apache.tomcat.*"
	};
	
	// >>> Event com.sun.tools.jdi.EventSetImpl$ThreadStartEventImpl
	// Tomcat JDBC Pool Cleaner[997608398:1533134536445] thread started
	// setStepping on Tomcat JDBC Pool Cleaner[997608398:1533134536445]
	
	private VirtualMachine vm;
	
	public JDIEventMonitor(VirtualMachine vm, String packageFilter) {
		super();
		this.vm = vm;
		this.packageFilter = packageFilter;
	}

	public void handleEvent(Event event)
	// process a JDI event
	{

		// method events
		if (event instanceof MethodEntryEvent) {
			methodEntryEvent((MethodEntryEvent) event);
		} else if (event instanceof MethodExitEvent) {
			methodExitEvent((MethodExitEvent) event);
		}

		// class events
		else if (event instanceof ClassPrepareEvent) {
			classPrepareEvent((ClassPrepareEvent) event);
		}

		// thread events
		else if (event instanceof ThreadStartEvent) {
			System.out.println(">>> Event " + event.getClass().getName());
			threadStartEvent((ThreadStartEvent) event);
		} else if (event instanceof ThreadDeathEvent) {
			System.out.println(">>> Event " + event.getClass().getName());
			threadDeathEvent((ThreadDeathEvent) event);
		}

		// step event -- a line of code is about to be executed
		else if (event instanceof StepEvent) {
			stepEvent((StepEvent) event);
		}

		// VM events

		else
			throw new Error("Unexpected event type");
	} // end of handleEvent()

	private void methodEntryEvent(MethodEntryEvent event)
	// entered a method but no code executed yet
	{
		Method meth = event.method();
		String className = meth.declaringType().name();

		if (className.indexOf(packageFilter) >= 0) {

			if (meth.isConstructor() && className.indexOf(packageFilter) >= 0)
				System.out.println("\nentered " + className + " constructor");
			else
				System.out.println("\nentered " + className + "." + meth.name() + "()");
		} // end of methodEntryEvent()
	}

	private void methodExitEvent(MethodExitEvent event)
	// all code in the method has been executed, and we are about to return
	{
		Method meth = event.method();
		String className = meth.declaringType().name();

		if (className.indexOf(packageFilter) >= 0) {
			if (meth.isConstructor())
				System.out.println("exiting " + className + " constructor\n");
			else
				System.out.println("exiting " + className + "." + meth.name() + "()\n");
		}

	} // end of methodExitEvent()

	// -------------------- class event handling ---------------

	private void classPrepareEvent(ClassPrepareEvent event)
	// a new class has been loaded
	{
		ReferenceType ref = event.referenceType();

		if (ref.name().indexOf(packageFilter) >= 0) {

			// String content = new String(Files.readAllBytes(Paths.get("duke.java")));
			System.out.println(">>> ref name file " + ref.name());

			// List<Field> fields = ref.fields();
			// List<Method> methods = ref.methods();
			//
			// String fnm;
			// try {
			// fnm = ref.sourceName(); // get filename of the class
			// showCode.add(fnm, ref.name());
			// }
			// catch (AbsentInformationException e)
			// { e.printStackTrace();
			// fnm = "??"; }

			// TODO : a quoi ca sert ?
			// setFieldsWatch(fields);

		} // end of classPrepareEvent()
	}

	private void threadStartEvent(ThreadStartEvent event)
	// a new thread has started running -- switch on single stepping
	{
		ThreadReference thr = event.thread();

		/*
		 * if (thr.name().equals("Signal Dispatcher") ||
		 * thr.name().equals("DestroyJavaVM") || thr.name().startsWith("AWT-") ) // AWT
		 * threads return;
		 * 
		 * if (thr.threadGroup().name().equals("system")) // ignore system threads
		 * return;
		 */
		System.out.println(thr.name() + " thread started");

		setStepping(thr);
	} // end of threadStartEvent()

	private void setStepping(ThreadReference thr)
	// start single stepping through the new thread
	{
		System.out.println("setStepping on " + thr.name());
		EventRequestManager mgr = vm.eventRequestManager();

		StepRequest sr = mgr.createStepRequest(thr, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
		sr.setSuspendPolicy(EventRequest.SUSPEND_NONE); // SUSPEND_EVENT_THREAD
		String regex = packageFilter + ".*";
		System.out.println(">>> packageFilter regex " + regex);
		sr.addClassFilter(regex);
		
		for (int i = 0; i < excludes.length; ++i)
			sr.addClassExclusionFilter(excludes[i]);
		sr.enable();
	} // end of setStepping()

	private void threadDeathEvent(ThreadDeathEvent event)
	// the thread is about to terminate
	{
		ThreadReference thr = event.thread();
		/*
		 * 
		 * if (thr.name().equals("DestroyJavaVM") || thr.name().startsWith("AWT-") )
		 * return;
		 * 
		 * if (thr.threadGroup().name().equals("system")) // ignore system threads
		 * return;
		 */
		System.out.println(thr.name() + " thread about to die");
	} // end of threadDeathEvent()

	// -------------------- step event handling ---------------
	private static final Pattern ENCLOSING_CLASS_REGEX = Pattern.compile("^([^\\$]*)");

	public static String parseEnclosingType(String fullyQualifiedName) {
		if (fullyQualifiedName == null) {
			return null;
		}
		Matcher matcher = ENCLOSING_CLASS_REGEX.matcher(fullyQualifiedName);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	private void stepEvent(StepEvent event)
	/*
	 * Print the line that's about to be executed. If this is the first line in a
	 * method then also print the local variables and the object's fields.
	 */
	{
		// TODO 4 - il n'y a pas de filtre sur le package lors du traitement des
		// stepevent
		// recuperer depuis l'event la classe concernée et donc son package
		// si on n'a pas d'info sur cette classe **ABSENT_BASE_SOURCE_NAME** , alors on
		// passe sans thrower d'exception

		List<LocalVariable> liste = new ArrayList<LocalVariable>();

		try {
			liste = event.location().method().variables();
		} catch (AbsentInformationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (LocalVariable l : liste) {
			String signature = l.signature();
			signature = signature.replace(";", "");

			if (signature != null && !signature.isEmpty()) {
				while (signature.indexOf("/") >= 0) {
					signature = signature.substring(signature.indexOf("/") + 1);
				}
			}

			System.out.println(signature + " " + l.name());
		}

		Location loc = event.location();
		String fullyQualifiedName = "Classe inconnue"; // Valeur par défaut quand le nom de la classe ne peut être
														// récupéré
		try {
			fullyQualifiedName = loc.sourceName();
		} catch (AbsentInformationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.out.println(">>>>>>> " + fullyQualifiedName + "  lineNumber : " + loc.lineNumber());

		// String enclosingType = parseEnclosingType(fullyQualifiedName);
		// String fnm = enclosingType.substring(enclosingType.lastIndexOf('.') + 1) +
		// ".java";
		// if(fnm.indexOf(packageFilter)>=0) {
		//
		// System.out.println(">>>>>>> " + fnm + " lineNumber : " + loc.lineNumber() );
		// }

		// try { // print the line
		// String fnm = loc.sourceName(); // get filename of code
		//
		//
		// // String showOuput = showCode.show(fnm, loc.lineNumber());
		// // if (showOuput != null) System.out.println(fnm + " à la ligne " +
		// loc.lineNumber() + ": " + showOuput );
		// }
		// catch (AbsentInformationException e) {
		// String enclosingType = parseEnclosingType(fullyQualifiedName);
		// String fnm = enclosingType.substring(enclosingType.lastIndexOf('.') + 1) +
		// ".java";
		// // String showOuput = showCode.show(fnm, loc.lineNumber());
		// // if (showOuput != null) {
		// // System.out.println(fnm + ": " + showOuput );
		// // } else {
		// // System.out.println(fnm + " à la ligne " + loc.lineNumber() );
		// // }
		//
		//
		// // relativeSourcePath = enclosingType.replace('.', File.separatorChar) +
		// ".java";
		// }
		//
		//
		// // returns -1 if the information is not available
		// // System.out.println(">>>>>>> lineNumber : " + loc.lineNumber() );

		// if (loc.codeIndex() == 0) // at the start of a method
		// printInitialState( event.thread() );
	} // end of stepEvent()

	private void printInitialState(ThreadReference thr) // TODO 1 - reactiver cette methode en se basant sur simpletrace
														// d'origine
	/*
	 * called to print the locals this object's fields when a method is first called
	 */
	{
		// get top-most current stack frame
		StackFrame currFrame = null;
		try {
			currFrame = thr.frame(0);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		printLocals(currFrame);

		// print fields info for the 'this' object
		ObjectReference objRef = currFrame.thisObject(); // get 'this' object
		if (objRef != null) {
			System.out.println("  object: " + objRef.toString());
			printFields(objRef);
		}
	} // end of printInitialState()

	private void printLocals(StackFrame currFrame)
	/*
	 * Print local variables that are currently visible in the method being
	 * executed. Since we only call printLocals() when a method is first entered,
	 * the only visible locals will be the parameters of the method.
	 */
	{
		List<LocalVariable> locals = null;
		try {
			locals = currFrame.visibleVariables();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (locals.size() == 0) // no local vars in the list
			return;

		System.out.println("  locals: ");
		for (LocalVariable l : locals)
			System.out.println("    | " + l.name() + " = " + currFrame.getValue(l));
	} // end of printLocals()

	private void printFields(ObjectReference objRef)
	// print the fields in the object
	{
		ReferenceType ref = objRef.referenceType(); // get type (class) of object
		List<Field> fields = null;
		try {
			fields = ref.fields(); // only this object's fields
			// could use allFields() to include inherited fields
		} catch (ClassNotPreparedException e) {
			e.printStackTrace();
			return;
		}

		System.out.println("  fields: ");
		for (Field f : fields)
			System.out.println("    | " + f.name() + " = " + objRef.getValue(f));
	} // end of printFields()


} // end of JDIEventMonitor class
