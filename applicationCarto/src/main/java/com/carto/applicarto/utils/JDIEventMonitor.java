package com.carto.applicarto.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.java.debug.core.adapter.variables.VariableUtils;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ClassUnloadRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;

public class JDIEventMonitor extends Thread
{
  // exclude events generated for these classes
  private final String[] excludes = { "java.*", "javax.*", "sun.*", "com.sun.*", 
		  "jdk.internal.*",	
		  };

  private final VirtualMachine vm;   // the JVM
  private final String packageFilter;	// the package
  private boolean connected = true;  // connected to VM?
  private boolean vmDied;            // has VM death occurred?

  private ShowCode showCode;


  public JDIEventMonitor(VirtualMachine jvm, String newPackageFilter)
  {
    super("JDIEventMonitor");
    vm = jvm;
    packageFilter = newPackageFilter;
    showCode = new ShowCode();

    setEventRequests(vm.allThreads());
  }  // end of JDIEventMonitor()



  private void setEventRequests(List<ThreadReference> threads)
  /* Create and enable the event requests for the events
     we want to monitor in the running program. */
  {
    EventRequestManager mgr = vm.eventRequestManager();

    MethodEntryRequest menr = mgr.createMethodEntryRequest(); // report method entries
    for (int i = 0; i < excludes.length; ++i)
      menr.addClassExclusionFilter(excludes[i]);
    menr.setSuspendPolicy(EventRequest.SUSPEND_NONE); // TODO a voir
    menr.enable();
	
    MethodExitRequest mexr = mgr.createMethodExitRequest();   // report method exits
    for (int i = 0; i < excludes.length; ++i) // TODO pourquoi faire plusieurs fois la boucle
      mexr.addClassExclusionFilter(excludes[i]);
    mexr.setSuspendPolicy(EventRequest.SUSPEND_NONE); // SUSPEND_EVENT_THREAD
    mexr.enable();

    ClassPrepareRequest cpr = mgr.createClassPrepareRequest(); // report class loads
    for (int i = 0; i < excludes.length; ++i)
      cpr.addClassExclusionFilter(excludes[i]);
    // cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
    cpr.enable();

    ClassUnloadRequest cur = mgr.createClassUnloadRequest();  // report class unloads
    for (int i = 0; i < excludes.length; ++i)
      cur.addClassExclusionFilter(excludes[i]);
    // cur.setSuspendPolicy(EventRequest.SUSPEND_ALL);
    cur.enable();
    
    ThreadStartRequest tsr = mgr.createThreadStartRequest();  // report thread starts
    tsr.enable();

    ThreadDeathRequest tdr = mgr.createThreadDeathRequest();  // report thread deaths
    tdr.enable();
    
    
    threads.forEach(thread -> setStepping(thread));
  }  // end of setEventRequests()



  public void run()
  // process JDI events as they arrive on the event queue
  { 
    EventQueue queue = vm.eventQueue();
    while (connected) {
      try {
        EventSet eventSet = queue.remove();
        for(Event event : eventSet)
          handleEvent(event);
        eventSet.resume();
      }
      catch (InterruptedException e) {e.printStackTrace();}  // Ignore
      catch (VMDisconnectedException discExc) {
    	  discExc.printStackTrace();
        handleDisconnectedException();
        break;
      }
    }
  }  // end of run()



  private void handleEvent(Event event)
  // process a JDI event
  {
	  
    // method events
	int toto = 1;
    if (event instanceof MethodEntryEvent) {
        methodEntryEvent((MethodEntryEvent) event);
    }
    else if (event instanceof MethodExitEvent) {
    	methodExitEvent((MethodExitEvent) event);
    }

    // class events
    else if (event instanceof ClassPrepareEvent) {
    	classPrepareEvent((ClassPrepareEvent) event);
    }
    else if (event instanceof ClassUnloadEvent) {
    	classUnloadEvent((ClassUnloadEvent) event);
    }

    // thread events
    else if (event instanceof ThreadStartEvent) {
    	System.out.println(">>> Event " + event.getClass().getName());
    	threadStartEvent((ThreadStartEvent) event);
    }
    else if (event instanceof ThreadDeathEvent) {
    	System.out.println(">>> Event " + event.getClass().getName());
    	threadDeathEvent((ThreadDeathEvent) event);
    }

    // step event -- a line of code is about to be executed
    else if (event instanceof StepEvent) {
    	stepEvent((StepEvent) event);
    }
//
//    // modified field event  -- a field is about to be changed
//    else if (event instanceof ModificationWatchpointEvent) {
//    	fieldWatchEvent((ModificationWatchpointEvent) event);
//    }

    // VM events
    else if (event instanceof VMStartEvent) {
    	vmStartEvent((VMStartEvent) event);
    }
    else if (event instanceof VMDeathEvent) {
    	vmDeathEvent((VMDeathEvent) event);
    }
    else if (event instanceof VMDisconnectEvent) {
    	vmDisconnectEvent((VMDisconnectEvent) event);
    }

    else
      throw new Error("Unexpected event type");
  }  // end of handleEvent()


  private synchronized void handleDisconnectedException()
  /* A VMDisconnectedException has occurred while dealing with
     another event. Flush the event queue, dealing only
     with exit events (VMDeath, VMDisconnect) so that things 
     terminate correctly. */
  {
    EventQueue queue = vm.eventQueue();
    while (connected) {
      try {
        EventSet eventSet = queue.remove();
        for(Event event : eventSet) {
          if (event instanceof VMDeathEvent)
            vmDeathEvent((VMDeathEvent) event);
          else if (event instanceof VMDisconnectEvent)
            vmDisconnectEvent((VMDisconnectEvent) event);
        }
        eventSet.resume(); // resume the VM
      }
      catch (InterruptedException e) {e.printStackTrace();}  // ignore
    }
  }  // end of handleDisconnectedException()



  // -------------------- method event handling  ---------------



  private void methodEntryEvent(MethodEntryEvent event)
  // entered a method but no code executed yet
  { 
    Method meth = event.method();
    String className = meth.declaringType().name();
    
    if(className.indexOf(packageFilter)>=0 
    		&& className.contains("<generated>") == false
			&& className.contains("$$FastClassBySpringCGLIB$$") == false
			&& className.contains("$$EnhancerBySpringCGLIB$$") == false) {
    	// entered com.carto.apptemoin.dao.impl.ClientDaoImpl$$EnhancerBySpringCGLIB$$79989aac.findClientById()

        if (meth.isConstructor())
	      System.out.println("\nentered " + className + " constructor");
	    else
	      System.out.println("\nentered " + className +  "." + meth.name() +"()");
	  }  // end of methodEntryEvent()
  }



  private void methodExitEvent(MethodExitEvent event)
  // all code in the method has been executed, and we are about to return
  {  
    Method meth = event.method();
    String className = meth.declaringType().name();

    if(className.indexOf(packageFilter)>=0
    		&& className.contains("<generated>") == false
    		&& className.contains("$$FastClassBySpringCGLIB$$") == false
			&& className.contains("$$EnhancerBySpringCGLIB$$") == false) {
	    if (meth.isConstructor())
	      System.out.println("exiting " + className + " constructor\n");
	    else
	      System.out.println("exiting " + className + "." + meth.name() + "()\n" );
    }

  }  // end of methodExitEvent()


  // -------------------- class event handling  ---------------


  private void classPrepareEvent(ClassPrepareEvent event)
  // a new class has been loaded  
  {
    ReferenceType ref = event.referenceType();
    
    if(ref.name().indexOf(packageFilter)>=0) {
	    
	    // String content = new String(Files.readAllBytes(Paths.get("duke.java")));
	    System.out.println(">>> ref name file " + ref.name());
    }  // end of classPrepareEvent()
  }


  private void classUnloadEvent(ClassUnloadEvent event)
  // a class has been unloaded  
  { 
    if (!vmDied)
      System.out.println("unloaded class: " + event.className());  
  }



  // ---------------------- modified field event handling ----------------------------------


//  private void fieldWatchEvent(ModificationWatchpointEvent event)
//  {
//     Field f = event.field();
//     Value value = event.valueToBe();   // value that _will_ be assigned
//     System.out.println("    > " + f.name() + " = " + value); // TODO 1 - exemple qui marche ou on affiche les valeurs des variables
//  }  // end of fieldWatchEvent()



  // -------------------- thread event handling  ---------------

  private void threadStartEvent(ThreadStartEvent event)
  // a new thread has started running -- switch on single stepping
  {
    ThreadReference thr = event.thread();
    
	  
    System.out.println(thr.name() + " thread started");

    setStepping(thr);
  } // end of threadStartEvent()



  private void setStepping(ThreadReference thr)
  // start single stepping through the new thread
  {
	  try {
    EventRequestManager mgr = vm.eventRequestManager();

    StepRequest sr = mgr.createStepRequest(thr, StepRequest.STEP_LINE,
                                                StepRequest.STEP_INTO);
    sr.setSuspendPolicy(EventRequest.SUSPEND_NONE); // SUSPEND_EVENT_THREAD

    for (int i = 0; i < excludes.length; ++i)
      sr.addClassExclusionFilter(excludes[i]);
    sr.enable();
	  } catch (com.sun.jdi.InternalException ex) {
		  System.err.println("exception on setStepping on " + thr.name());
	  }
  }  // end of setStepping()




  private void threadDeathEvent(ThreadDeathEvent event)
  // the thread is about to terminate
  {ThreadReference thr = event.thread();
    System.out.println(thr.name() + " thread about to die");
  }  // end of threadDeathEvent()


  // -------------------- step event handling  ---------------
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
  /* Print the line that's about to be executed.
     If this is the first line in a method then also print 
     the local variables and the object's fields.
  */
  { 
	// TODO 4 - il n'y a pas de filtre sur le package lors du traitement des stepevent
	  // recuperer depuis l'event la classe concernée et donc son package
	  // si on n'a pas d'info sur cette classe **ABSENT_BASE_SOURCE_NAME** , alors on passe sans thrower d'exception
	  
//	  //TODO on tente de récupérer les valeurs des variables
//	  ThreadReference thr = event.thread();
//	  StackFrame sf = null;
//	  try {
//		sf = thr.frame(0);
//	} catch (IncompatibleThreadStateException e2) {
//		// TODO Auto-generated catch block
//		e2.printStackTrace();
//	}
	  
	 Location loc = event.location();
	 String fullyQualifiedName = "Classe inconnue";	//Valeur par défaut quand le nom de la classe ne peut être récupéré
	try {
		fullyQualifiedName = loc.sourcePath().replace("\\", ".").replace(".java", "");
	} catch (AbsentInformationException e1) {
		// TODO Auto-generated catch block
		//e1.printStackTrace();
	}
	 
	if (fullyQualifiedName.startsWith(packageFilter) 
			&& fullyQualifiedName.contains("<generated>") == false
			&& fullyQualifiedName.contains("$$FastClassBySpringCGLIB$$") == false
			&& fullyQualifiedName.contains("$$EnhancerBySpringCGLIB$$") == false) {
		// <generated>  lineNumber : -1
		// exiting com.carto.apptemoin.dao.impl.ClientDaoImpl$$FastClassBySpringCGLIB$$e82ed8b6.invoke()
		System.out.println("work on " + fullyQualifiedName + " at lineNumber : " + loc.lineNumber() );
		
		
		// TODO a externaliser dans une méthode dédiée
		try {
			List<LocalVariable> liste = event.location().method().variables();
			for(LocalVariable l:liste) {
				String signature = l.signature();
				signature = signature.replace(";", "");
				
				if(signature != null && !signature.isEmpty()) {
					while(signature.indexOf("/")>=0) {
						signature = signature.substring(signature.indexOf("/")+1);
					}
				}
				
				System.out.println(" variable : " + l.name() + " of type " + l.typeName());
				
				// TODO pour avoir la valeur, il faudra utiliser java-debug et stackFrame.getValue(localVariable)
			}
		} catch (AbsentInformationException e) {
		}

		  
		
	}
	 
//	 String enclosingType = parseEnclosingType(fullyQualifiedName);
//	 String fnm = enclosingType.substring(enclosingType.lastIndexOf('.') + 1) + ".java";
//	 if(fnm.indexOf(packageFilter)>=0) {
//		 
//		 System.out.println(">>>>>>> " + fnm + "  lineNumber : " + loc.lineNumber() );
//	 }


	 
	 
//    try {   // print the line
//      String fnm = loc.sourceName();  // get filename of code
//      
//      
//      // String showOuput = showCode.show(fnm, loc.lineNumber());
//      // if (showOuput != null) System.out.println(fnm + " à la ligne " + loc.lineNumber() + ": " + showOuput );
//    }
//    catch (AbsentInformationException e) {
//    	String enclosingType = parseEnclosingType(fullyQualifiedName);
//    	String fnm = enclosingType.substring(enclosingType.lastIndexOf('.') + 1) + ".java";
//    	// String showOuput = showCode.show(fnm, loc.lineNumber());
//        // if (showOuput != null) { 
//        //	System.out.println(fnm + ": " + showOuput );
//        // } else {
//        // 	System.out.println(fnm + " à la ligne " + loc.lineNumber() );
//        // }
//        
//        
//        // relativeSourcePath = enclosingType.replace('.', File.separatorChar) + ".java";
//    }
//
//    
//    	// returns -1 if the information is not available
//      // System.out.println(">>>>>>>  lineNumber : " + loc.lineNumber() );

    

    //if (loc.codeIndex() == 0)   // at the start of a method
      //printInitialState( event.thread() );
  }  // end of stepEvent()


  private void printInitialState(ThreadReference thr) // TODO 1 - reactiver cette methode en se basant sur simpletrace d'origine
  /* called to print the locals this object's fields when a method
     is first called */
  {
    // get top-most current stack frame
	  thr.suspend();
    StackFrame currFrame = null;
    try {
      currFrame = thr.frame(0);
    }
    catch (Exception e) {
      e.printStackTrace();
      thr.resume();
      return;
    }

    printLocals(currFrame);
    thr.resume();

    // print fields info for the 'this' object
    ObjectReference objRef = currFrame.thisObject();   // get 'this' object
    if (objRef != null) {
      System.out.println("  object: " + objRef.toString());
      printFields(objRef);
    }
  }  // end of printInitialState()


  private void printLocals(StackFrame currFrame)
  /* Print local variables that are currently visible in the method 
     being executed. Since we only call printLocals() when a method
     is first entered, the only visible locals will be the
     parameters of the method. */
  {
    List<LocalVariable> locals = null;
    try {
      locals = currFrame.visibleVariables();
    }
    catch (Exception e) {
    	e.printStackTrace();
      return;
    }

    if (locals.size() == 0)   // no local vars in the list
      return;

    System.out.println("  locals: ");
    for(LocalVariable l : locals)
      System.out.println("    | " + l.name() + 
                               " = " + currFrame.getValue(l) );
  }  // end of printLocals()




  private void printFields(ObjectReference objRef)
  // print the fields in the object
  {
    ReferenceType ref = objRef.referenceType();  // get type (class) of object
    List<Field> fields = null;
    try {
      fields = ref.fields();      // only this object's fields
            // could use allFields() to include inherited fields
    }
    catch (ClassNotPreparedException e) {
    	e.printStackTrace();
      return;
    }

    System.out.println("  fields: ");
    for(Field f : fields)
      System.out.println("    | " + f.name() + " = " + objRef.getValue(f) );
  }  // end of printFields()


  // ---------------------- VM event handling ----------------------------------

  private void vmStartEvent(VMStartEvent event)
  /* Notification of initialization of a target VM. This event is received 
     before the main thread is started and before any application code has 
     been executed. */
  { vmDied = false;
    System.out.println("-- VM Started --"); 
  }


  private void vmDeathEvent(VMDeathEvent event)
  // Notification of VM termination
  { vmDied = true;
    System.out.println("-- The application has exited --");
  }


  private void vmDisconnectEvent(VMDisconnectEvent event)
  /* Notification of disconnection from the VM, either through normal termination 
     or because of an exception/error. */
  { connected = false;
    if (!vmDied)
      System.out.println("-- The application has been disconnected --");
  }

}  // end of JDIEventMonitor class
