
Pour que ca fonctionne : 
- il faut ajouter tools.jar dans le classpath du projet (proprietes du projet > java build path > 
	librairies > selectionner jdk > ajouter external > dans le jdl trouver tools.jar)
- il faut le projet JavaAgentCartoTest à coté, il a été ajouté dans les entries du classpath pour lancer SimpleTrace (voir SimpleTrace.launch)


RAF : 
- avoir accès aux sources du code
- faire un accès à une JVM remote
	http://kingsfleet.blogspot.fr/2013/10/write-auto-debugger-to-catch-exceptions.html
		vmm = com.sun.jdi.Bootstrap.virtualMachineManager();
		vmm.attachingConnectors().each{ if("dt_socket".equalsIgnoreCase(it.transport().name())) { atconn = it; } }
		prm = atconn.defaultArguments();
		prm.get("port").setValue(7896prm.getprm.get("hostname").setValue("127.0.0.1.attach(prm);


https://fivedots.coe.psu.ac.th/~ad/jg/javaArt3/
https://fivedots.coe.psu.ac.th/~ad/jg/javaArt3/SimpleTrace.zip
https://fivedots.coe.psu.ac.th/~ad/jg/javaArt3/traceJPDA.pdf

Similaire : http://cs.fit.edu/~ryan/java/programs/jdi/trace/

Autres : 
https://stackify.com/java-remote-debugging/
http://bridgei2i.com/blog/an-introduction-to-the-java-debugger/
https://github.com/gmu-swe/phosphor
http://www.jonbell.net/2016/11/debugging-java-bytecode-instrumentation/

================================================================================================================

JavaArt Chapter 3. Tracing with JPDA

From the website:

  Killer Game Programming in Java
  http://fivedots.coe.psu.ac.th/~ad/jg

  Dr. Andrew Davison
  Dept. of Computer Engineering
  Prince of Songkla University
  Hat yai, Songkhla 90112, Thailand
  E-mail: ad@fivedots.coe.psu.ac.th


If you use this code, please mention my name, and include a link
to the website.

Thanks,
  Andrew

============================

The tracer application consists of 5 Java files:

  * SimpleTrace.java, StreamRedirecter.java, JDIEventMonitor.java
    ShowCode.java, ShowLines.java


There are 2 test applications for the tracer:

  * Comparison.java
  * TestStack.java and Stack.java
       - TestStack uses Stack


There are 2 batch files:

  * compile.bat
  * runTrace.bat
     - they both assume that tools.jar is in
           c:\Program Files\Java\jdk1.6.0_10\lib\


----------------------------
Compilation of the tracer:

  > compile *.java

Note: the compilation of the test applications can use javac:

  > javac Comparison.java
  > javac TestStack.java
  > javac Stack.java


----------------------------
Execution of the tracer:

  > runTrace <java-file-to-be-traced>
e.g.
  > runTrace Comparison
  > runTrace TestStack


Note: you can also run the test applications directly:

  > java Comparison
  > java TestStack

---------
Last updated: 30th March 2009
