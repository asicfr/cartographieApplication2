
Pour que ca fonctionne : 
- il faut ajouter tools.jar dans le classpath du projet (proprietes du projet > java build path > 
	librairies > selectionner jdk > ajouter external > dans le jdl trouver tools.jar)
- il faut le projet JavaAgentCartoTest à coté, il a été ajouté dans les entries du classpath pour lancer SimpleTrace (voir SimpleTrace.launch)


RAF : 
TODO : 
- reporter les modifications qu'on a fait sur com.carto.applicarto.utils.JDIEventMonitor
			vers com.carto.applicarto.utils2.JDIEventMonitor (utilise java-debug)
		il faut qu'on ait le meme comportement entre ces deux classes
- on commit sur notre branche git
- cleaner le code com.carto.applicarto.utils2.JDIEventMonitor
		- supprimer commentaire code
		- supprimer les commentaires inutiles
		- on supprimer les méthodes inutiles ...
		- on reformatte le code (ctrl+shift+f)
		- on supprime les classe du package com.carto.applicarto.utils
		il faut qu'on ait le meme comportement qu'avant
- on commit sur notre branche git

- on va implementer quelques fonctionnalités - commiter à chaque fonctionnalité terminée :
	- afficher les variables dans stepEvent en utilisant java-debug et stackFrame.getValue(localVariable)
	- ajouter un petit message : "carto ready" une fois que les setStepping sont ok
	- rattacher le flux d'execution à un thead id -- à voir ensemble
	- +indentation sur les enters / -indentation sur les exits
	- centraliser les startsWith + contains("<generated>")
	- mettre en place un vrai logger (log4j / slf4j ...)
	- afficher les données en entrées de la méthode : event.location().method().arguments()
	- stocker le résultat du flux d'exécution





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
