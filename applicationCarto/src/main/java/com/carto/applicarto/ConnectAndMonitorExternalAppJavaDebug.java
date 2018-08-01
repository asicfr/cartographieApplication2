package com.carto.applicarto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.carto.applicarto.utils2.JDIEventMonitor;
import com.microsoft.java.debug.core.DebugSession;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

public class ConnectAndMonitorExternalAppJavaDebug {

	public ConnectAndMonitorExternalAppJavaDebug(String host, String port, String packageFilter) {
		VirtualMachine vm = null;
		try {
			vm = connect(host, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JDIEventMonitor jdiEventMonitor = new JDIEventMonitor(vm, packageFilter);
		DebugSession debugSession = new DebugSession(vm);
		debugSession.getEventHub().events().subscribe(debugEvent  -> {
			jdiEventMonitor.handleEvent(debugEvent.event);
		});
		debugSession.start();
	}

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
			
			new ConnectAndMonitorExternalAppJavaDebug(host, port, packageFilter);
		}
	}

	
	
	
} 
