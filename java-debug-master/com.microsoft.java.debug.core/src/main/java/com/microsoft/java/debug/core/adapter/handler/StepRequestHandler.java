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

package com.microsoft.java.debug.core.adapter.handler;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;

import com.microsoft.java.debug.core.Configuration;
import com.microsoft.java.debug.core.DebugEvent;
import com.microsoft.java.debug.core.DebugUtility;
import com.microsoft.java.debug.core.IDebugSession;
import com.microsoft.java.debug.core.adapter.AdapterUtils;
import com.microsoft.java.debug.core.adapter.ErrorCode;
import com.microsoft.java.debug.core.adapter.IDebugAdapterContext;
import com.microsoft.java.debug.core.adapter.IDebugRequestHandler;
import com.microsoft.java.debug.core.protocol.Events;
import com.microsoft.java.debug.core.protocol.Messages.Response;
import com.microsoft.java.debug.core.protocol.Requests.Arguments;
import com.microsoft.java.debug.core.protocol.Requests.Command;
import com.microsoft.java.debug.core.protocol.Requests.StepArguments;
import com.microsoft.java.debug.core.protocol.Requests.StepFilters;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.StepRequest;

import io.reactivex.disposables.Disposable;

public class StepRequestHandler implements IDebugRequestHandler {
    private static final Logger logger = Logger.getLogger(Configuration.LOGGER_NAME);

    @Override
    public List<Command> getTargetCommands() {
        return Arrays.asList(Command.STEPIN, Command.STEPOUT, Command.NEXT);
    }

    @Override
    public CompletableFuture<Response> handle(Command command, Arguments arguments, Response response,
            IDebugAdapterContext context) {
        if (context.getDebugSession() == null) {
            return AdapterUtils.createAsyncErrorResponse(response, ErrorCode.EMPTY_DEBUG_SESSION, "Debug Session doesn't exist.");
        }

        long threadId = ((StepArguments) arguments).threadId;
        ThreadReference thread = DebugUtility.getThread(context.getDebugSession(), threadId);
        if (thread != null) {
            try {
                ThreadState threadState = new ThreadState();
                threadState.threadId = threadId;
                threadState.pendingStepType = command;
                threadState.stackDepth = thread.frameCount();
                threadState.stepLocation = getTopFrame(thread).location();
                threadState.eventSubscription = context.getDebugSession().getEventHub().events()
                    .filter(debugEvent -> (debugEvent.event instanceof StepEvent && debugEvent.event.request().equals(threadState.pendingStepRequest))
                            || debugEvent.event instanceof BreakpointEvent)
                    .subscribe(debugEvent -> {
                        handleDebugEvent(debugEvent, context.getDebugSession(), context, threadState);
                    });

                if (command == Command.STEPIN) {
                    threadState.pendingStepRequest = DebugUtility.createStepIntoRequest(thread,
                            context.getStepFilters().classNameFilters);
                } else if (command == Command.STEPOUT) {
                    threadState.pendingStepRequest = DebugUtility.createStepOutRequest(thread,
                            context.getStepFilters().classNameFilters);
                } else {
                    threadState.pendingStepRequest = DebugUtility.createStepOverRequest(thread,
                            context.getStepFilters().classNameFilters);
                }
                threadState.pendingStepRequest.enable();
                DebugUtility.resumeThread(thread);

                ThreadsRequestHandler.checkThreadRunningAndRecycleIds(thread, context);
            } catch (IncompatibleThreadStateException ex) {
                final String failureMessage = String.format("Failed to step because the thread '%s' is not suspended in the target VM.", thread.name());
                logger.log(Level.SEVERE, failureMessage);
                return AdapterUtils.createAsyncErrorResponse(response, ErrorCode.STEP_FAILURE, failureMessage);
            } catch (IndexOutOfBoundsException ex) {
                final String failureMessage = String.format("Failed to step because the thread '%s' doesn't contain any stack frame", thread.name());
                logger.log(Level.SEVERE, failureMessage);
                return AdapterUtils.createAsyncErrorResponse(response, ErrorCode.STEP_FAILURE, failureMessage);
            }
        }

        return CompletableFuture.completedFuture(response);
    }

    private void handleDebugEvent(DebugEvent debugEvent, IDebugSession debugSession, IDebugAdapterContext context,
            ThreadState threadState) {
        Event event = debugEvent.event;

        // When a breakpoint occurs, abort any pending step requests from the same thread.
        if (event instanceof BreakpointEvent) {
            long threadId = ((BreakpointEvent) event).thread().uniqueID();
            if (threadId == threadState.threadId && threadState.pendingStepRequest != null) {
                DebugUtility.deleteEventRequestSafely(debugSession.getVM().eventRequestManager(), threadState.pendingStepRequest);
                threadState.pendingStepRequest = null;
                if (threadState.eventSubscription != null) {
                    threadState.eventSubscription.dispose();
                }
            }
        } else if (event instanceof StepEvent) {
            ThreadReference thread = ((StepEvent) event).thread();
            DebugUtility.deleteEventRequestSafely(thread.virtualMachine().eventRequestManager(), threadState.pendingStepRequest);
            threadState.pendingStepRequest = null;
            if (isStepFiltersConfigured(context.getStepFilters())) {
                try {
                    if (threadState.pendingStepType == Command.STEPIN) {
                        int currentStackDepth = thread.frameCount();
                        Location currentStepLocation = getTopFrame(thread).location();
                        // Check if the step into operation stepped through the filtered code and stopped at an un-filtered location.
                        if (threadState.stackDepth + 1 < thread.frameCount()) {
                            // Create another stepOut request to return back where we started the step into.
                            threadState.pendingStepRequest = DebugUtility.createStepOutRequest(thread,
                                    context.getStepFilters().classNameFilters);
                            threadState.pendingStepRequest.enable();
                            debugEvent.shouldResume = true;
                            return;
                        }
                        // If the ending step location is filtered, or same as the original location where the step into operation is originated,
                        // do another step of the same kind.
                        if (shouldFilterLocation(threadState.stepLocation, currentStepLocation, context)
                                || shouldDoExtraStepInto(threadState.stackDepth, threadState.stepLocation, currentStackDepth, currentStepLocation)) {
                            threadState.pendingStepRequest = DebugUtility.createStepIntoRequest(thread,
                                    context.getStepFilters().classNameFilters);
                            threadState.pendingStepRequest.enable();
                            debugEvent.shouldResume = true;
                            return;
                        }
                    }
                } catch (IncompatibleThreadStateException | IndexOutOfBoundsException ex) {
                    // ignore.
                }
            }
            if (threadState.eventSubscription != null) {
                threadState.eventSubscription.dispose();
            }
            context.getProtocolServer().sendEvent(new Events.StoppedEvent("step", thread.uniqueID()));
            debugEvent.shouldResume = false;
        }
    }

    private boolean isStepFiltersConfigured(StepFilters filters) {
        if (filters == null) {
            return false;
        }
        return ArrayUtils.isNotEmpty(filters.classNameFilters) || filters.skipConstructors
               || filters.skipStaticInitializers || filters.skipSynthetics;
    }

    /**
     * Return true if the StepEvent's location is a Method that the user has indicated to filter.
     *
     * @throws IncompatibleThreadStateException
     *                      if the thread is not suspended in the target VM.
     */
    private boolean shouldFilterLocation(Location originalLocation, Location currentLocation, IDebugAdapterContext context)
            throws IncompatibleThreadStateException {
        if (originalLocation == null || currentLocation == null) {
            return false;
        }
        return !shouldFilterMethod(originalLocation.method(), context) && shouldFilterMethod(currentLocation.method(), context);
    }

    private boolean shouldFilterMethod(Method method, IDebugAdapterContext context) {
        return (context.getStepFilters().skipStaticInitializers && method.isStaticInitializer())
                || (context.getStepFilters().skipSynthetics && method.isSynthetic())
                || (context.getStepFilters().skipConstructors && method.isConstructor());
    }

    /**
     * Check if the current top stack is same as the original top stack.
     *
     * @throws IncompatibleThreadStateException
     *                      if the thread is not suspended in the target VM.
     */
    private boolean shouldDoExtraStepInto(int originalStackDepth, Location originalLocation, int currentStackDepth, Location currentLocation)
            throws IncompatibleThreadStateException {
        if (originalStackDepth != currentStackDepth) {
            return false;
        }
        if (originalLocation == null) {
            return false;
        }
        Method originalMethod = originalLocation.method();
        Method currentMethod = currentLocation.method();
        if (!originalMethod.equals(currentMethod)) {
            return false;
        }
        if (originalLocation.lineNumber() != currentLocation.lineNumber()) {
            return false;
        }
        return true;
    }

    /**
     * Return the top stack frame of the target thread.
     *
     * @param thread
     *              the target thread.
     * @return the top frame.
     * @throws IncompatibleThreadStateException
     *                      if the thread is not suspended in the target VM.
     * @throws IndexOutOfBoundsException
     *                      if the thread doesn't contain any stack frame.
     */
    private StackFrame getTopFrame(ThreadReference thread) throws IncompatibleThreadStateException {
        return thread.frame(0);
    }

    class ThreadState {
        long threadId = -1;
        Command pendingStepType;
        StepRequest pendingStepRequest = null;
        int stackDepth = -1;
        Location stepLocation = null;
        Disposable eventSubscription = null;
    }
}
