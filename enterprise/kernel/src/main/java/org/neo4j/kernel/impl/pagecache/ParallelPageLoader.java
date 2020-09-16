/*
 * Copyright (c) 2002-2018 "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.kernel.impl.pagecache;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.io.pagecache.tracing.cursor.PageCursorTracer;

import static org.neo4j.io.pagecache.PagedFile.PF_SHARED_READ_LOCK;

class ParallelPageLoader implements PageLoader
{

    private final PagedFile file;
    private final Executor executor;
    private final PageCacheTracer pageCacheTracer;
    private final AtomicLong received;
    private final AtomicLong processed;

    ParallelPageLoader( PagedFile file, Executor executor, PageCacheTracer pageCacheTracer )
    {
        this.file = file;
        this.executor = executor;
        this.pageCacheTracer = pageCacheTracer;
        received = new AtomicLong();
        processed = new AtomicLong();
    }

    @Override
    public void load( long pageId )
    {
        PageCursorTracer pageCursorTracer = this.pageCacheTracer.createPageCursorTracer( PAGE_CACHE_PROFILE_LOADER );
        received.getAndIncrement();
        executor.execute( () ->
                          {
                              try
                              {
                                  try ( PageCursor cursor = file.io( pageId, PF_SHARED_READ_LOCK, pageCursorTracer ) )
                                  {
                                      cursor.next();
                                  }
                                  catch ( IOException ignore )
                                  {
                                  }
                              }
                              finally
                              {

                                  processed.getAndIncrement();
                              }
                          } );
    }

    @Override
    public void close()
    {
        while ( processed.get() < received.get() )
        {
            Thread.yield();
        }
    }
}
