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

import org.neo4j.io.IOUtils;
import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.io.pagecache.tracing.cursor.PageCursorTracer;

import static org.neo4j.io.pagecache.PagedFile.PF_SHARED_READ_LOCK;

class SingleCursorPageLoader implements PageLoader
{

    private final PageCursor cursor;
    private final PageCursorTracer pageCursorTracer;

    SingleCursorPageLoader( PagedFile file, PageCacheTracer pageCacheTracer ) throws IOException
    {
        this.pageCursorTracer = pageCacheTracer.createPageCursorTracer( PAGE_CACHE_PROFILE_LOADER );
        this.cursor = file.io( 0, PF_SHARED_READ_LOCK, pageCursorTracer );
    }

    @Override
    public void load( long pageId ) throws IOException
    {
        cursor.next( pageId );
    }

    @Override
    public void close()
    {
        IOUtils.closeAllUnchecked( this.cursor, this.pageCursorTracer );
    }
}
