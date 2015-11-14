/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;

import java.util.concurrent.ExecutionException;

/**
 * An object with an operational state, plus asynchronous {@link #start()} and
 * {@link #stop()} lifecycle methods to transfer into and out of this state.
 * Example services include webservers, RPC servers and timers. The normal
 * lifecycle of a service is:
 * <ul>
 *   <li>{@link State#NEW} -&gt;</li>
 *   <li>{@link State#STARTING} -&gt;</li>
 *   <li>{@link State#RUNNING} -&gt;</li>
 *   <li>{@link State#STOPPING} -&gt;</li>
 *   <li>{@link State#TERMINATED}</li>
 * </ul>
 *
 * If the service fails while starting, running or stopping, its state will be
 * {@link State#FAILED}, and its behavior is undefined. Such a service cannot be
 * started nor stopped.
 *
 * <p>Implementors of this interface are strongly encouraged to extend one of 
 * the abstract classes in this package which implement this interface and 
 * make the threading and state management easier.
 *
 * @author Jesse Wilson
 * @since 9.0 (in 1.0 as
 *     {@code com.google.common.base.Service})
 */
@Beta // TODO(kevinb): make abstract class?
public interface Service {
  /**
   * If the service state is {@link State#NEW}, this initiates service startup
   * and returns immediately. If the service has already been started, this
   * method returns immediately without taking action. A stopped service may not
   * be restarted.
   *
   * @return a future for the startup result, regardless of whether this call
   *     initiated startup. Calling {@link ListenableFuture#get} will block
   *     until the service has finished starting, and returns one of {@link
   *     State#RUNNING}, {@link State#STOPPING} or {@link State#TERMINATED}. If
   *     the service fails to start, {@link ListenableFuture#get} will throw an
   *     {@link ExecutionException}, and the service's state will be {@link
   *     State#FAILED}. If it has already finished starting, {@link
   *     ListenableFuture#get} returns immediately. Cancelling this future has
   *     no effect on the service.
   */
  ListenableFuture<State> start();

  /**
   * Initiates service startup (if necessary), returning once the service has
   * finished starting. Unlike calling {@code start().get()}, this method throws
   * no checked exceptions, and it cannot be {@linkplain Thread#interrupt
   * interrupted}.
   *
   * @throws UncheckedExecutionException if startup failed
   * @return the state of the service when startup finished.
   */
  State startAndWait();

  /**
   * Returns {@code true} if this service is {@linkplain State#RUNNING running}.
   */
  boolean isRunning();

  /**
   * Returns the lifecycle state of the service.
   */
  State state();

  /**
   * If the service is {@linkplain State#STARTING starting} or {@linkplain
   * State#RUNNING running}, this initiates service shutdown and returns
   * immediately. If the service is {@linkplain State#NEW new}, it is
   * {@linkplain State#TERMINATED terminated} without having been started nor
   * stopped.  If the service has already been stopped, this method returns
   * immediately without taking action.
   *
   * @return a future for the shutdown result, regardless of whether this call
   *     initiated shutdown. Calling {@link ListenableFuture#get} will block
   *     until the service has finished shutting down, and either returns
   *     {@link State#TERMINATED} or throws an {@link ExecutionException}. If
   *     it has already finished stopping, {@link ListenableFuture#get} returns
   *     immediately.  Cancelling this future has no effect on the service.
   */
  ListenableFuture<State> stop();

  /**
   * Initiates service shutdown (if necessary), returning once the service has
   * finished stopping. If this is {@link State#STARTING}, startup will be
   * cancelled. If this is {@link State#NEW}, it is {@link State#TERMINATED
   * terminated} without having been started nor stopped. Unlike calling {@code
   * stop().get()}, this method throws no checked exceptions.
   *
   * @throws UncheckedExecutionException if shutdown failed
   * @return the state of the service when shutdown finished.
   */
  State stopAndWait();

  /**
   * The lifecycle states of a service.
   *
   * @since 9.0 (in 1.0 as
   *     {@code com.google.common.base.Service.State})
   */
  @Beta // should come out of Beta when Service does
  enum State {
    /**
     * A service in this state is inactive. It does minimal work and consumes
     * minimal resources.
     */
    NEW,

    /**
     * A service in this state is transitioning to {@link #RUNNING}.
     */
    STARTING,

    /**
     * A service in this state is operational.
     */
    RUNNING,

    /**
     * A service in this state is transitioning to {@link #TERMINATED}.
     */
    STOPPING,

    /**
     * A service in this state has completed execution normally. It does minimal
     * work and consumes minimal resources.
     */
    TERMINATED,

    /**
     * A service in this state has encountered a problem and may not be
     * operational. It cannot be started nor stopped.
     */
    FAILED
  }
}
