/*
 * Copyright (c) 2002-2018 "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 *   This file is part of ONgDB.
 *
 *   ONgDB is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.neo4j.internal.batchimport;

import org.neo4j.configuration.Config;
import org.neo4j.internal.batchimport.ImportLogic.Monitor;
import org.neo4j.internal.batchimport.input.Collector;
import org.neo4j.internal.batchimport.staging.ExecutionMonitor;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.kernel.impl.store.format.RecordFormats;
import org.neo4j.logging.internal.LogService;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.storageengine.api.LogFilesInitializer;

/**
 *
 */
public class EnterpriseBatchImporterFactory extends BatchImporterFactory
{

    public static final int PRIORITY = 10;
    public static final String FACTORY_NAME = "enterprise-importer";

    public EnterpriseBatchImporterFactory()
    {
        super( PRIORITY );
    }

    @Override
    public String getName()
    {
        return FACTORY_NAME;
    }

    /**
     * @param databaseLayout
     * @param fileSystem
     * @param externalPageCache
     * @param pageCacheTracer
     * @param config
     * @param logService
     * @param executionMonitor
     * @param additionalInitialIds
     * @param dbConfig
     * @param recordFormats
     * @param monitor
     * @param jobScheduler
     * @param badCollector
     * @param logFilesInitializer
     * @param memoryTracker
     * @return
     */
    public BatchImporter instantiate( DatabaseLayout databaseLayout, FileSystemAbstraction fileSystem, PageCache externalPageCache,
                                      PageCacheTracer pageCacheTracer, Configuration config, LogService logService, ExecutionMonitor executionMonitor,
                                      AdditionalInitialIds additionalInitialIds, Config dbConfig, RecordFormats recordFormats, Monitor monitor,
                                      JobScheduler jobScheduler, Collector badCollector, LogFilesInitializer logFilesInitializer, MemoryTracker memoryTracker )
    {
        return new EnterpriseParallelBatchImporter( databaseLayout, fileSystem, externalPageCache, pageCacheTracer, config, logService, executionMonitor,
                                                    additionalInitialIds, dbConfig, recordFormats, monitor, jobScheduler, badCollector, logFilesInitializer,
                                                    memoryTracker );
    }
}
