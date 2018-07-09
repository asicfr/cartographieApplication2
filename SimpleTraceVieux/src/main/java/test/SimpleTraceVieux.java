package test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;

public class SimpleTraceVieux
{

  public SimpleTraceVieux(String classpathStr, String[] args, List<String> vmArgs)
  {
	  System.out.println("classpathStr = "+classpathStr);
	  System.out.println("args = "+args);
	  System.out.println("vmArgs = "+vmArgs.toString());
    VirtualMachine vm = launchConnect(classpathStr, args, vmArgs);
    monitorJVM(vm);
  }  // end of SimpleTrace()


  private VirtualMachine launchConnect(String classpathStr, String[] args, List<String> vmArgs)
  // Set up a launching connection to the JVM
  {
    VirtualMachine vm = null;
    LaunchingConnector conn = getCommandLineConnector();
    Map<String,Connector.Argument> connArgs = setMainArgs(classpathStr, conn, args, vmArgs);
    
    try {
      vm = conn.launch(connArgs);   // launch the JVM and connect to it
    }
    catch (IOException e) {
    	e.printStackTrace();
      throw new Error("Unable to launch JVM: " + e);
    }
    catch (IllegalConnectorArgumentsException e) {
    	e.printStackTrace();
      throw new Error("Internal error: " + e);
    }
    catch (VMStartException e) {
    	e.printStackTrace();
      throw new Error("JVM failed to start: " + e.getMessage());
    }

    return vm;
  }  // end of launchConnect()



  private LaunchingConnector getCommandLineConnector()
  // find a command line launch connector
  {
    List<Connector> conns = Bootstrap.virtualMachineManager().allConnectors();

    for (Connector conn: conns) {
      if (conn.name().equals("com.sun.jdi.CommandLineLaunch"))
        return (LaunchingConnector) conn;
    }
    throw new Error("No launching connector found");
  } // end of getCommandLineConnector()



  private Map<String,Connector.Argument> setMainArgs(String classpathStr, 
                                 LaunchingConnector conn, String[] args, List<String> vmArgs)
  // make the tracer's input arguments the program's main() arguments
  {
    // get the connector argument for the program's main() method
    Map<String,Connector.Argument> connArgs = conn.defaultArguments();
    Connector.Argument mArgs = (Connector.Argument) connArgs.get("main");
    if (mArgs == null)
      throw new Error("Bad launching connector");

    // concatenate all the tracer's input arguments into a single string
    StringBuffer sb = new StringBuffer();
    
	sb.append(" -cp " + classpathStr + " ");
    
    for (int i=0; i < args.length; i++) {
    	sb.append(args[i] + " ");
    }
    
    for (String string : vmArgs) {
    	sb.append(string + " ");
	}
    
    System.out.println(">>> jvm args : " + sb.toString());
    
    mArgs.setValue(sb.toString());   // assign input args to application's main()
    return connArgs;
  }  // end of setMainArgs()



  private void monitorJVM(VirtualMachine vm)
  // monitor the JVM running the application
  {
    // start JDI event handler which displays trace info
    JDIEventMonitor watcher = new JDIEventMonitor(vm); 
    watcher.start();   

    /* redirect VM's output and error streams
       to the system output and error streams */
    Process process = vm.process();
    Thread errRedirect = new StreamRedirecter("error reader",
                                   process.getErrorStream(), System.err);
    Thread outRedirect = new StreamRedirecter("output reader",
                                   process.getInputStream(), System.out);
    errRedirect.start();
    outRedirect.start();

    vm.resume();           // start the application

    try {
      watcher.join();      // Wait. Shutdown begins when the JDI watcher terminates
      errRedirect.join();  // make sure all the stream outputs have been forwarded before we exit
      outRedirect.join();  
    }
    catch (InterruptedException e) {e.printStackTrace();}
  }  // end of monitorJVM()



  // ------------------------------------------------

  public static void main(String[] args)
  { 
    if (args.length == 0)
      System.err.println("Usage: runTrace <program>");
    else {
    	String classpathStr = System.getProperty("java.class.path");
		System.out.print("> classpath : " + classpathStr);
    	
    	
    	for (String string : args) {
    		System.out.println(">> launch args : " + string);
		}
    	RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    	List<String> arguments = runtimeMxBean.getInputArguments();

    	for (String arg : arguments) {
    		System.out.println(">> vm args : " + arg);
		}
    	new SimpleTraceVieux(classpathStr, args, arguments);  
    }
  } 

}  // end of SimpleTrace class
