package com.carto.applicarto;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

import com.carto.applicarto.utils.JDIEventMonitor;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.connect.AttachingConnector;

public class ConnectAndMonitorExternalApp {

	public ConnectAndMonitorExternalApp(String host, String port, String packageFilter) {
//		System.out.println("classpathStr = " + classpathStr);
//		System.out.println("args = " + args);
//		System.out.println("vmArgs = " + vmArgs.toString());
//		for (String arg : args) {
//			System.out.println(arg);
//		}
		VirtualMachine vm = null;
		try {
			vm = connect(host, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		monitorJVM(vm, packageFilter);
	} // end of SimpleTrace()

	private VirtualMachine connect(String host, String port) throws IOException {
		VirtualMachineManager manager = Bootstrap.virtualMachineManager();

		// Find appropiate connector
		List<AttachingConnector> connectors = manager.attachingConnectors();
		AttachingConnector chosenConnector = null;
		for (AttachingConnector c : connectors) {
			if (c.transport().name().equals("dt_socket")) {
				chosenConnector = c;
				break;
			}
		}
		if (chosenConnector == null) {
			throw new IllegalStateException("Could not find socket connector");
		}

		// Set port argument
		AttachingConnector connector = chosenConnector;
		Map<String, Argument> defaults = connector.defaultArguments();
		Argument arg = defaults.get("port");
		if (arg == null) {
			throw new IllegalStateException("Could not find port argument");
		}
		arg.setValue(port);
		
		((Connector.Argument) defaults.get("hostname")).setValue(host);
		
		// Attach
		try {
			System.out.println("Connector arguments: " + defaults);
			return connector.attach(defaults);
		} catch (IllegalConnectorArgumentsException e) {
			throw new IllegalArgumentException("Illegal connector arguments", e);
		}
	}

	private LaunchingConnector getCommandLineConnector()
	// find a command line launch connector
	{
		List<Connector> conns = Bootstrap.virtualMachineManager().allConnectors();

		for (Connector conn : conns) {
			if (conn.name().equals("com.sun.jdi.CommandLineLaunch")) // "com.sun.jdi.ProcessAttach"
				return (LaunchingConnector) conn; // com.sun.jdi.connect.AttachingConnector
		}
		throw new Error("No launching connector found");
	} // end of getCommandLineConnector()

	private Map<String, Connector.Argument> setMainArgs(String classpathStr, LaunchingConnector conn, String[] args,
			List<String> vmArgs)
	// make the tracer's input arguments the program's main() arguments
	{
		// get the connector argument for the program's main() method
		Map<String, Connector.Argument> connArgs = conn.defaultArguments();

		// ((com.sun.jdi.connect.Connector.BooleanArgument)
		// connArgs.get("suspend")).setValue(false);

		Connector.Argument mArgs = (Connector.Argument) connArgs.get("main");
		if (mArgs == null)
			throw new Error("Bad launching connector");

		// TODO pour springboot, comment changer la valeur de suspend à no ???

		// concatenate all the tracer's input arguments into a single string
		StringBuffer sb = new StringBuffer();

		sb.append(" -cp " + classpathStr + " ");

		for (int i = 0; i < args.length; i++) {
			sb.append(args[i] + " ");
		}

		for (String string : vmArgs) {
			sb.append(string + " ");
		}

		System.out.println(">>> jvm args : " + sb.toString());

		mArgs.setValue(sb.toString()); // assign input args to application's main()
		return connArgs;
	} // end of setMainArgs()

	private void monitorJVM(VirtualMachine vm, String packageFilter)
	// monitor the JVM running the application
	{
		// start JDI event handler which displays trace info
		JDIEventMonitor watcher = new JDIEventMonitor(vm, packageFilter);
		watcher.start();

		// vm.resume(); // start the application

		try {
			watcher.join(); // Wait. Shutdown begins when the JDI watcher terminates
			// errRedirect.join(); // make sure all the stream outputs have been forwarded
			// before we exit
			// outRedirect.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	} // end of monitorJVM()

	// ------------------------------------------------

	public static void main(String[] args) {
		// TODO : 
		// arg[0] -> host (localhost)
		// arg[1] -> port (8001)
		// arg[2] -> package à filtrer (com.carto.applitemoin)
		// arg[3] -> sources (c:\temp\sources\projet\temoin)
		
		if (args.length == 0)
			System.err.println("Usage: runTrace <program>");
		else {
			// String classpathStr = System.getProperty("java.class.path");
			// System.out.print("> classpath : " + classpathStr);

			for (String string : args) {
				System.out.println(">> launch args : " + string);
			}
			
			// RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
			// List<String> arguments = runtimeMxBean.getInputArguments();

			//for (String arg : arguments) {
			//	System.out.println(">> vm args : " + arg);
			//}
			
			String host = args[0];
			String port = args[1];
			String packageFilter = args[2];
			
			new ConnectAndMonitorExternalApp(host, port, packageFilter);
		}
	}

} 
