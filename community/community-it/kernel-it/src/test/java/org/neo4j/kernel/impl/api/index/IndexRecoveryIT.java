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
package org.neo4j.kernel.impl.api.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.internal.kernel.api.InternalIndexState;
import org.neo4j.internal.kernel.api.TokenNameLookup;
import org.neo4j.internal.kernel.api.exceptions.schema.MisconfiguredIndexException;
import org.neo4j.internal.kernel.api.schema.LabelSchemaDescriptor;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.index.IndexAccessor;
import org.neo4j.kernel.api.index.IndexEntryUpdate;
import org.neo4j.kernel.api.index.IndexPopulator;
import org.neo4j.kernel.api.index.IndexProvider;
import org.neo4j.kernel.api.index.IndexUpdater;
import org.neo4j.kernel.api.schema.SchemaDescriptorFactory;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.api.index.sampling.IndexSamplingConfig;
import org.neo4j.kernel.impl.api.index.updater.SwallowingIndexUpdater;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.impl.index.schema.CollectingIndexUpdater;
import org.neo4j.kernel.impl.storemigration.StoreMigrationParticipant;
import org.neo4j.kernel.impl.transaction.log.checkpoint.CheckPointer;
import org.neo4j.kernel.impl.transaction.log.checkpoint.SimpleTriggerInfo;
import org.neo4j.kernel.impl.transaction.log.rotation.LogRotation;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.storageengine.api.schema.IndexDescriptor;
import org.neo4j.storageengine.api.schema.IndexSample;
import org.neo4j.storageengine.api.schema.StoreIndexDescriptor;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.test.rule.fs.EphemeralFileSystemRule;
import org.neo4j.values.storable.Values;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.neo4j.graphdb.Label.label;
import static org.neo4j.kernel.impl.api.index.SchemaIndexTestHelper.singleInstanceIndexProviderFactory;
import static org.neo4j.kernel.impl.api.index.TestIndexProviderDescriptor.PROVIDER_DESCRIPTOR;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.getIndexes;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.hasSize;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.haveState;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.inTx;

public class IndexRecoveryIT
{
    @Test
    public void shouldBeAbleToRecoverInTheMiddleOfPopulatingAnIndex() throws Exception
    {
        // Given
        startDb();

        CountDownLatch latch = new CountDownLatch( 1 );
        when( mockedIndexProvider
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) ) )
                .thenReturn( indexPopulatorWithControlledCompletionTiming( latch ) );
        createIndex( myLabel );

        // And Given
        Future<Void> killFuture = killDbInSeparateThread();
        latch.countDown();
        killFuture.get();

        // When
        when( mockedIndexProvider.getInitialState( any( StoreIndexDescriptor.class ) ) )
                .thenReturn( InternalIndexState.POPULATING );
        latch = new CountDownLatch( 1 );
        when( mockedIndexProvider
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) ) )
                .thenReturn( indexPopulatorWithControlledCompletionTiming( latch ) );
        startDb();

        // Then
        assertThat( getIndexes( db, myLabel ), inTx( db, hasSize( 1 ) ) );
        assertThat( getIndexes( db, myLabel ), inTx( db, haveState( db, Schema.IndexState.POPULATING ) ) );
        verify( mockedIndexProvider, times( 2 ) )
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) );
        verify( mockedIndexProvider, never() )
                .getOnlineAccessor( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any( TokenNameLookup.class ) );
        latch.countDown();
    }

    @Test
    public void shouldBeAbleToRecoverInTheMiddleOfPopulatingAnIndexWhereLogHasRotated() throws Exception
    {
        // Given
        startDb();

        CountDownLatch latch = new CountDownLatch( 1 );
        when( mockedIndexProvider
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) ) )
                .thenReturn( indexPopulatorWithControlledCompletionTiming( latch ) );
        createIndex( myLabel );
        rotateLogsAndCheckPoint();

        // And Given
        Future<Void> killFuture = killDbInSeparateThread();
        latch.countDown();
        killFuture.get();
        latch = new CountDownLatch( 1 );
        when( mockedIndexProvider
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) ) )
                .thenReturn( indexPopulatorWithControlledCompletionTiming( latch ) );
        when( mockedIndexProvider.getInitialState( any( StoreIndexDescriptor.class ) ) )
                .thenReturn( InternalIndexState.POPULATING );

        // When
        startDb();

        // Then
        assertThat( getIndexes( db, myLabel ), inTx( db, hasSize( 1 ) ) );
        assertThat( getIndexes( db, myLabel ), inTx( db, haveState( db, Schema.IndexState.POPULATING ) ) );
        verify( mockedIndexProvider, times( 2 ) )
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) );
        verify( mockedIndexProvider, never() )
                .getOnlineAccessor( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any( TokenNameLookup.class ) );
        latch.countDown();
    }

    @Test
    public void shouldBeAbleToRecoverAndUpdateOnlineIndex() throws Exception
    {
        // Given
        startDb();

        IndexPopulator populator = mock( IndexPopulator.class );
        when( mockedIndexProvider
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) ) )
                .thenReturn( populator );
        when( populator.sampleResult() ).thenReturn( new IndexSample() );
        IndexAccessor mockedAccessor = mock( IndexAccessor.class );
        when( mockedAccessor.newUpdater( any( IndexUpdateMode.class ) ) ).thenReturn( SwallowingIndexUpdater.INSTANCE );
        when( mockedIndexProvider.getOnlineAccessor( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any( TokenNameLookup.class ) )
        ).thenReturn( mockedAccessor );
        createIndexAndAwaitPopulation( myLabel );
        // rotate logs
        rotateLogsAndCheckPoint();
        // make updates
        Set<IndexEntryUpdate<?>> expectedUpdates = createSomeBananas( myLabel );

        // And Given
        killDb();
        when( mockedIndexProvider.getInitialState( any( StoreIndexDescriptor.class )) )
                .thenReturn( InternalIndexState.ONLINE );
        GatheringIndexWriter writer = new GatheringIndexWriter();
        when( mockedIndexProvider.getOnlineAccessor( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any( TokenNameLookup.class ) )
        ).thenReturn( writer );

        // When
        startDb();

        // Then
        assertThat( getIndexes( db, myLabel ), inTx( db, hasSize( 1 ) ) );
        assertThat( getIndexes( db, myLabel ), inTx( db, haveState( db, Schema.IndexState.ONLINE ) ) );
        verify( mockedIndexProvider, times( 1 ) )
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) );
        int onlineAccessorInvocationCount = 2; // once when we create the index, and once when we restart the db
        verify( mockedIndexProvider, times( onlineAccessorInvocationCount ) )
                .getOnlineAccessor( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any( TokenNameLookup.class ) );
        assertEquals( expectedUpdates, writer.batchedUpdates );
    }

    @Test
    public void shouldKeepFailedIndexesAsFailedAfterRestart() throws Exception
    {
        // Given
        IndexPopulator indexPopulator = mock( IndexPopulator.class );
        when( mockedIndexProvider.getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) ) )
                .thenReturn( indexPopulator );
        IndexAccessor indexAccessor = mock( IndexAccessor.class );
        when( mockedIndexProvider.getOnlineAccessor( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any( TokenNameLookup.class ) ) )
                .thenReturn( indexAccessor );
        startDb();
        createIndex( myLabel );
        rotateLogsAndCheckPoint();

        // And Given
        killDb();
        when( mockedIndexProvider.getInitialState( any( StoreIndexDescriptor.class ) ) )
                .thenReturn( InternalIndexState.FAILED );

        // When
        startDb();

        // Then
        assertThat( getIndexes( db, myLabel ), inTx( db, hasSize( 1 ) ) );
        assertThat( getIndexes( db, myLabel ), inTx( db, haveState( db, Schema.IndexState.FAILED ) ) );
        verify( mockedIndexProvider, times( 2 ) )
                .getPopulator( any( StoreIndexDescriptor.class ), any( IndexSamplingConfig.class ), any(), any( TokenNameLookup.class ) );
    }

    private GraphDatabaseAPI db;
    @Rule
    public EphemeralFileSystemRule fs = new EphemeralFileSystemRule();
    private final IndexProvider mockedIndexProvider = mock( IndexProvider.class );
    private final KernelExtensionFactory<?> mockedIndexProviderFactory =
            singleInstanceIndexProviderFactory( PROVIDER_DESCRIPTOR.getKey(),
                    mockedIndexProvider );
    private final String key = "number_of_bananas_owned";
    private final Label myLabel = label( "MyLabel" );

    @Before
    public void setUp() throws MisconfiguredIndexException
    {
        when( mockedIndexProvider.getProviderDescriptor() ).thenReturn( PROVIDER_DESCRIPTOR );
        when( mockedIndexProvider.storeMigrationParticipant( any( FileSystemAbstraction.class ), any( PageCache.class ) ) )
                .thenReturn( StoreMigrationParticipant.NOT_PARTICIPATING );
        when( mockedIndexProvider.bless( any( IndexDescriptor.class ) ) ).thenCallRealMethod();
    }

    private void startDb()
    {
        if ( db != null )
        {
            db.shutdown();
        }

        TestGraphDatabaseFactory factory = new TestGraphDatabaseFactory();
        factory.setFileSystem( fs.get() );
        factory.setKernelExtensions( Collections.singletonList( mockedIndexProviderFactory ) );
        db = (GraphDatabaseAPI) factory.newImpermanentDatabaseBuilder()
                .setConfig( GraphDatabaseSettings.default_schema_provider, PROVIDER_DESCRIPTOR.name() ).newGraphDatabase();
    }

    private void killDb() throws Exception
    {
        if ( db != null )
        {
            fs.snapshot( () ->
            {
                db.shutdown();
                db = null;
            } );
        }
    }

    private Future<Void> killDbInSeparateThread()
    {
        ExecutorService executor = newSingleThreadExecutor();
        Future<Void> result = executor.submit( () ->
        {
            killDb();
            return null;
        } );
        executor.shutdown();
        return result;
    }

    @After
    public void after()
    {
        if ( db != null )
        {
            db.shutdown();
        }
    }

    private void rotateLogsAndCheckPoint() throws IOException
    {
        db.getDependencyResolver().resolveDependency( LogRotation.class ).rotateLogFile();
        db.getDependencyResolver().resolveDependency( CheckPointer.class ).forceCheckPoint(
                new SimpleTriggerInfo( "test" )
        );
    }

    private void createIndexAndAwaitPopulation( Label label )
    {
        IndexDefinition index = createIndex( label );
        try ( Transaction tx = db.beginTx() )
        {
            db.schema().awaitIndexOnline( index, 10, SECONDS );
            tx.success();
        }
    }

    private IndexDefinition createIndex( Label label )
    {
        try ( Transaction tx = db.beginTx() )
        {
            IndexDefinition index = db.schema().indexFor( label ).on( key ).create();
            tx.success();
            return index;
        }
    }

    private Set<IndexEntryUpdate<?>> createSomeBananas( Label label )
    {
        Set<IndexEntryUpdate<?>> updates = new HashSet<>();
        try ( Transaction tx = db.beginTx() )
        {
            ThreadToStatementContextBridge ctxSupplier = db.getDependencyResolver().resolveDependency(
                    ThreadToStatementContextBridge.class );
            KernelTransaction ktx =
                    ctxSupplier.getKernelTransactionBoundToThisThread( true );

            int labelId = ktx.tokenRead().nodeLabel( label.name() );
            int propertyKeyId = ktx.tokenRead().propertyKey( key );
            LabelSchemaDescriptor schemaDescriptor = SchemaDescriptorFactory.forLabel( labelId, propertyKeyId );
            for ( int number : new int[]{4, 10} )
            {
                Node node = db.createNode( label );
                node.setProperty( key, number );
                updates.add( IndexEntryUpdate.add( node.getId(), schemaDescriptor, Values.of( number ) ) );
            }
            tx.success();
            return updates;
        }
    }

    public static class GatheringIndexWriter extends IndexAccessor.Adapter
    {
        private final Set<IndexEntryUpdate<?>> regularUpdates = new HashSet<>();
        private final Set<IndexEntryUpdate<?>> batchedUpdates = new HashSet<>();

        @Override
        public IndexUpdater newUpdater( final IndexUpdateMode mode )
        {
            return new CollectingIndexUpdater( updates ->
            {
                switch ( mode )
                {
                    case ONLINE:
                        regularUpdates.addAll( updates );
                        break;

                    case RECOVERY:
                        batchedUpdates.addAll( updates );
                        break;

                    default:
                        throw new UnsupportedOperationException(  );
                }
            } );
        }
    }

    private static IndexPopulator indexPopulatorWithControlledCompletionTiming( final CountDownLatch latch )
    {
        return new IndexPopulator.Adapter()
        {
            @Override
            public void create()
            {
                try
                {
                    latch.await();
                }
                catch ( InterruptedException e )
                {
                    // fall through and return early
                }
                throw new RuntimeException( "this is expected" );
            }
        };
    }
}
