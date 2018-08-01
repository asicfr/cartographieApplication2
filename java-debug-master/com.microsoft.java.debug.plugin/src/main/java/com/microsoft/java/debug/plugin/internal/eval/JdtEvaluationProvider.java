/*******************************************************************************
 * Copyright (c) 2017 Microsoft Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Microsoft Corporation - initial API and implementation
 *******************************************************************************/

package com.microsoft.java.debug.plugin.internal.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.eval.ICompiledExpression;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIStackFrame;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;
import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTEvaluationEngine;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;

import com.microsoft.java.debug.core.Configuration;
import com.microsoft.java.debug.core.IEvaluatableBreakpoint;
import com.microsoft.java.debug.core.adapter.Constants;
import com.microsoft.java.debug.core.adapter.IDebugAdapterContext;
import com.microsoft.java.debug.core.adapter.IEvaluationProvider;
import com.microsoft.java.debug.plugin.internal.JdtUtils;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class JdtEvaluationProvider implements IEvaluationProvider {
    private static final Logger logger = Logger.getLogger(Configuration.LOGGER_NAME);
    private IJavaProject project;
    private ILaunch launch;
    private JDIDebugTarget debugTarget;
    private Map<ThreadReference, JDIThread> threadMap = new HashMap<>();
    private HashMap<String, Object> options = new HashMap<>();
    private IDebugAdapterContext context;

    private List<IJavaProject> projectCandidates;

    private Set<String> visitedClassNames = new HashSet<>();

    public JdtEvaluationProvider() {
    }

    @Override
    public void initialize(IDebugAdapterContext context, Map<String, Object> props) {
        if (props == null) {
            throw new IllegalArgumentException("argument is null");
        }
        options.putAll(props);
        this.context = context;
    }

    @Override
    public CompletableFuture<Value> evaluateForBreakpoint(IEvaluatableBreakpoint breakpoint, ThreadReference thread) {
        if (breakpoint == null) {
            throw new IllegalArgumentException("The breakpoint is null.");
        }

        if (!breakpoint.containsEvaluatableExpression()) {
            throw new IllegalArgumentException("The breakpoint doesn't contain the evaluatable expression.");
        }

        if (StringUtils.isNotBlank(breakpoint.getLogMessage())) {
            return evaluate(logMessageToExpression(breakpoint.getLogMessage()), thread, 0, breakpoint);
        } else {
            return evaluate(breakpoint.getCondition(), thread, 0, breakpoint);
        }
    }

    @Override
    public CompletableFuture<Value> evaluate(String expression, ThreadReference thread, int depth) {
        return evaluate(expression, thread, depth, null);
    }

    private CompletableFuture<Value> evaluate(String expression, ThreadReference thread, int depth, IEvaluatableBreakpoint breakpoint) {
        CompletableFuture<Value> completableFuture = new CompletableFuture<>();
        try  {
            ensureDebugTarget(thread.virtualMachine(), thread, depth);
            JDIThread jdiThread = getMockJDIThread(thread);
            JDIStackFrame stackframe = createStackFrame(jdiThread, depth);
            if (stackframe == null) {
                logger.severe("Cannot evaluate because the stackframe is not available.");
                throw new IllegalStateException("Cannot evaluate because the stackframe is not available.");
            }

            ICompiledExpression compiledExpression = null;
            ASTEvaluationEngine engine = new ASTEvaluationEngine(project, debugTarget);
            if (breakpoint != null) {
                if (StringUtils.isNotBlank(breakpoint.getLogMessage())) {
                    compiledExpression = (ICompiledExpression) breakpoint.getCompiledLogpointExpression();
                    if (compiledExpression == null) {
                        compiledExpression = engine.getCompiledExpression(expression, stackframe);
                        breakpoint.setCompiledLogpointExpression(compiledExpression);
                    }
                } else {
                    compiledExpression = (ICompiledExpression) breakpoint.getCompiledConditionalExpression();
                    if (compiledExpression == null) {
                        compiledExpression = engine.getCompiledExpression(expression, stackframe);
                        breakpoint.setCompiledConditionalExpression(compiledExpression);
                    }
                }
            } else {
                compiledExpression = engine.getCompiledExpression(expression, stackframe);
            }

            internalEvaluate(engine, compiledExpression, stackframe, completableFuture);
            return completableFuture;
        } catch (Exception ex) {
            completableFuture.completeExceptionally(ex);
            return completableFuture;
        }
    }

    private String logMessageToExpression(String logMessage) {
        final String LOGMESSAGE_VARIABLE_REGEXP = "\\{(.*?)\\}";
        String format = logMessage.replaceAll(LOGMESSAGE_VARIABLE_REGEXP, "%s");

        Pattern pattern = Pattern.compile(LOGMESSAGE_VARIABLE_REGEXP);
        Matcher matcher = pattern.matcher(logMessage);
        List<String> arguments = new ArrayList<>();
        while (matcher.find()) {
            arguments.add("(" + matcher.group(1) + ")");
        }

        if (arguments.size() > 0) {
            return "System.out.println(String.format(\"" + format + "\"," + String.join(",", arguments) + "))";
        } else {
            return "System.out.println(\"" + format + "\")";
        }
    }

    /**
     * Prepare a list of java project candidates in workspace which contains the main class.
     *
     * @param mainclass the main class specified by launch.json for finding project candidates
     */
    private void initializeProjectCandidates(String mainclass) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        projectCandidates = Arrays.stream(root.getProjects()).map(JdtUtils::getJavaProject).filter(p -> {
            try {
                return p != null && p.hasBuildState();
            } catch (Exception e) {
                // ignore
            }
            return false;
        }).collect(Collectors.toList());


        if (StringUtils.isNotBlank(mainclass)) {
            filterProjectCandidatesByClass(mainclass);
        }
    }

    private void filterProjectCandidatesByClass(String className) {
        projectCandidates = visitedClassNames.contains(className) ? projectCandidates
                 : projectCandidates.stream().filter(p -> {
                     try {
                         return p.findType(className) != null;
                     } catch (Exception e) {
                         // ignore
                     }
                     return false;
                 }).collect(Collectors.toList());
        visitedClassNames.add(className);
    }

    private IJavaProject findJavaProjectByStackFrame(ThreadReference thread, int depth) {
        if (projectCandidates == null) {
            // initial candidate projects by main class (projects contains this main class)
            initializeProjectCandidates((String) options.get(Constants.MAIN_CLASS));
        }

        if (projectCandidates.size() == 0) {
            logger.severe("No project is available for evaluation.");
            throw new IllegalStateException("Cannot evaluate, please specify projectName in launch.json.");
        }


        try {
            StackFrame sf = thread.frame(depth);
            String typeName = sf.location().method().declaringType().name();
            // narrow down candidate projects by current class
            filterProjectCandidatesByClass(typeName);
        } catch (Exception ex) {
            logger.severe("Cannot evaluate when the project is not specified, due to exception: " + ex.getMessage());
            throw new IllegalStateException("Cannot evaluate, please specify projectName in launch.json.");
        }

        if (projectCandidates.size() == 1) {
            return projectCandidates.get(0);
        }

        if (projectCandidates.size() == 0) {
            logger.severe("No project is available for evaluation.");
            throw new IllegalStateException("Cannot evaluate, please specify projectName in launch.json.");
        } else {
            // narrow down projects
            logger.severe("Multiple projects are valid for evaluation.");
            throw new IllegalStateException("Cannot evaluate, please specify projectName in launch.json.");
        }

    }


    private JDIStackFrame createStackFrame(JDIThread thread, int depth) {
        try {
            IStackFrame[] jdiStackFrames = thread.getStackFrames();
            return jdiStackFrames.length > depth ? (JDIStackFrame) jdiStackFrames[depth] : null;
        } catch (DebugException e) {
            return null;
        }

    }

    private JDIThread getMockJDIThread(ThreadReference thread) {
        synchronized (threadMap) {
            return threadMap.computeIfAbsent(thread, threadKey -> new JDIThread(debugTarget, thread) {
                @Override
                protected synchronized void invokeComplete(int restoreTimeout) {
                    super.invokeComplete(restoreTimeout);
                    context.getStackFrameManager().reloadStackFrames(thread);
                }
            });
        }

    }

    private void internalEvaluate(ASTEvaluationEngine engine, ICompiledExpression compiledExpression,
            IJavaStackFrame stackframe, CompletableFuture<Value> completableFuture) {
        try  {
            engine.evaluateExpression(compiledExpression, stackframe, evaluateResult -> {
                if (evaluateResult == null || evaluateResult.hasErrors()) {
                    Exception ex = evaluateResult.getException() != null ? evaluateResult.getException()
                            : new RuntimeException(StringUtils.join(evaluateResult.getErrorMessages()));
                    completableFuture.completeExceptionally(ex);
                    return;
                }
                try {
                    // we need to read fValue from the result Value instance implements by JDT
                    Value value = (Value) FieldUtils.readField(evaluateResult.getValue(), "fValue", true);
                    completableFuture.complete(value);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    completableFuture.completeExceptionally(ex);
                }
            }, 0, false);
        } catch (Exception ex) {
            completableFuture.completeExceptionally(ex);
        }
    }

    @Override
    public boolean isInEvaluation(ThreadReference thread) {
        return debugTarget != null && getMockJDIThread(thread).isPerformingEvaluation();
    }

    @Override
    public void clearState(ThreadReference thread) {
        if (debugTarget != null) {
            synchronized (threadMap) {
                JDIThread jdiThread = threadMap.get(thread);
                if (jdiThread != null) {
                    try {
                        jdiThread.terminateEvaluation();
                    } catch (DebugException e) {
                        logger.warning(String.format("Error stopping evalutoin on thread %d: %s", thread.uniqueID(),
                                e.toString()));
                    }
                    threadMap.remove(thread);
                }
            }
        }
    }

    private void ensureDebugTarget(VirtualMachine vm, ThreadReference thread, int depth) {
        if (debugTarget == null) {
            if (project == null) {
                String projectName = (String) options.get(Constants.PROJECT_NAME);
                if (StringUtils.isBlank(projectName)) {
                    project = findJavaProjectByStackFrame(thread, depth);
                } else {
                    IJavaProject javaProject = JdtUtils.getJavaProject(projectName);
                    if (javaProject == null) {
                        throw new IllegalStateException(String.format("Project %s cannot be found.", projectName));
                    }
                    project = javaProject;
                }
            }

            if (launch == null) {
                launch = createILaunchMock(project);
            }

            debugTarget = new JDIDebugTarget(launch, vm, "", false, false, null, false) {
                @Override
                protected synchronized void initialize() {
                    // use empty initialize intentionally to avoid to register jdi event listener
                }
            };
        }
    }

    private static ILaunch createILaunchMock(IJavaProject project) {
        return new ILaunch() {
            private AbstractSourceLookupDirector locator;

            @Override
            public boolean canTerminate() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public void terminate() throws DebugException {
            }

            @Override
            public <T> T getAdapter(Class<T> arg0) {
                return null;
            }

            @Override
            public void addDebugTarget(IDebugTarget arg0) {
            }

            @Override
            public void addProcess(IProcess arg0) {
            }

            @Override
            public String getAttribute(String arg0) {
                return null;
            }

            @Override
            public Object[] getChildren() {
                return null;
            }

            @Override
            public IDebugTarget getDebugTarget() {
                return null;
            }

            @Override
            public IDebugTarget[] getDebugTargets() {
                return null;
            }

            @Override
            public ILaunchConfiguration getLaunchConfiguration() {
                return null;
            }

            @Override
            public String getLaunchMode() {
                return null;
            }

            @Override
            public IProcess[] getProcesses() {
                return null;
            }

            @Override
            public ISourceLocator getSourceLocator() {
                if (locator != null) {
                    return locator;
                }
                locator = new JavaSourceLookupDirector();

                try {
                    locator.setSourceContainers(
                            new ProjectSourceContainer(project.getProject(), true).getSourceContainers());
                } catch (CoreException e) {
                    logger.severe(String.format("Cannot initialize JavaSourceLookupDirector: %s", e.toString()));
                }
                locator.initializeParticipants();
                return locator;
            }

            @Override
            public boolean hasChildren() {
                return false;
            }

            @Override
            public void removeDebugTarget(IDebugTarget arg0) {
            }

            @Override
            public void removeProcess(IProcess arg0) {
            }

            @Override
            public void setAttribute(String arg0, String arg1) {
            }

            @Override
            public void setSourceLocator(ISourceLocator arg0) {
            }
        };
    }
}
