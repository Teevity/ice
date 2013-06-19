/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.ice.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class Poller {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    private Thread pollerThread;
    private CountDownLatch threadDoneSignal;

    private void doWork(int initialDelaySec, int delaySec, boolean fixedRate) {
        logger.info("poller starting...");
        boolean first = true;
        long sleepTime = delaySec * 1000L;
        while (true) {
            try {
                if (first) {
                    if (initialDelaySec > 0) {
                        Thread.sleep(initialDelaySec * 1000L);
                    }
                    first = false;
                }
                else if (sleepTime > 0)
                    Thread.sleep(sleepTime);

                long startMillis = System.currentTimeMillis();
                poll();
                sleepTime = fixedRate ? delaySec * 1000L - (System.currentTimeMillis() - startMillis) : delaySec * 1000L;
            }
            catch (InterruptedException e) {
                break;
            }
            catch (InterruptedIOException e) {
                break;
            }
            catch (ClosedByInterruptException e) {
                break;
            }
            catch (Exception e) {
                logger.error("Error polling", e);
            }
        }
        threadDoneSignal.countDown();
        logger.info("poller stopping.");
    }

    protected abstract void poll() throws Exception;

    protected String getThreadName() {
        return getClass().getName();
    }

    public void start() {
        start(0, 3600, false);
    }

    public void start(final int delaySec) {
       start(0, delaySec, false);
    }

    public void start(final int initialDelaySec, final int delaySec, final boolean fixedRate) {
        threadDoneSignal = new CountDownLatch(1);
        pollerThread = new Thread(new Runnable() {
            public void run() {
                doWork(initialDelaySec, delaySec, fixedRate);
            }
        }, getThreadName());
        pollerThread.start();
        logger.info("poller thread for " + getThreadName() + " started...");
    }

    public void shutdown() {
        logger.info("shutting down... trying to interrupt poller thread...");
        boolean done = false;
        int numTries = 0;
        while (!done) {
            pollerThread.interrupt();
            try {
                done = threadDoneSignal.await(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                // ingore InterruptedException here
            }
            if (!done) {
                numTries = numTries + 1;
                logger.warn("trying to interrupt write thread again " + numTries);
            }
            else {
                logger.info("shutted down successfully.");
            }
        }
    }
}
