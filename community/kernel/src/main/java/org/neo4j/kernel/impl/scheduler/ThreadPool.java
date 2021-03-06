/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.neo4j.scheduler.Group;
import org.neo4j.scheduler.JobHandle;

final class ThreadPool
{
    private final GroupedDaemonThreadFactory threadFactory;
    private final ExecutorService executor;
    private final ConcurrentHashMap<Object,Future<?>> registry;
    private InterruptedException shutdownInterrupted;

    ThreadPool( Group group, ThreadGroup parentThreadGroup )
    {
        threadFactory = new GroupedDaemonThreadFactory( group, parentThreadGroup );
        executor = group.buildExecutorService( threadFactory );
        registry = new ConcurrentHashMap<>();
    }

    public ThreadFactory getThreadFactory()
    {
        return threadFactory;
    }

    public JobHandle submit( Runnable job )
    {
        Object registryKey = new Object();
        CompletableFuture<Void> placeHolder = CompletableFuture.completedFuture( null );
        registry.put( registryKey, placeHolder );

        Runnable registeredJob = () ->
        {
            try
            {
                job.run();
            }
            finally
            {
                registry.remove( registryKey );
            }
        };

        Future<?> future = executor.submit( registeredJob );
        registry.replace( registryKey, placeHolder, future );
        return new PooledJobHandle( future, registryKey, registry );
    }

    int activeJobCount()
    {
        return registry.size();
    }

    void cancelAllJobs()
    {
        registry.values().removeIf( future ->
        {
            future.cancel( true );
            return true;
        } );
    }

    void shutDown()
    {
        executor.shutdown();
        try
        {
            executor.awaitTermination( 30, TimeUnit.SECONDS );
        }
        catch ( InterruptedException e )
        {
            shutdownInterrupted = e;
        }
    }

    InterruptedException getShutdownException()
    {
        return shutdownInterrupted;
    }
}
