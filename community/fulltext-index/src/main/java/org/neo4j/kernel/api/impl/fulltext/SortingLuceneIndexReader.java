package org.neo4j.kernel.api.impl.fulltext;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.SortingLeafReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.util.Sorter;
import org.eclipse.collections.api.set.primitive.MutableLongSet;

import org.neo4j.io.IOUtils;
import org.neo4j.values.storable.Value;

import static java.util.Arrays.asList;
import static org.neo4j.kernel.api.impl.fulltext.ScoreEntityIterator.mergeIterators;

public class SortingLuceneIndexReader extends FulltextIndexReader
{
    private final FulltextIndexReader baseReader;
    private final FulltextIndexReader nearRealTimeReader;
    private final MutableLongSet modifiedEntityIdsInThisTransaction;

    SortingLuceneIndexReader( FulltextIndexReader baseReader, FulltextIndexReader nearRealTimeReader,
                                         MutableLongSet modifiedEntityIdsInThisTransaction )
    {

        this.baseReader = baseReader;
        this.nearRealTimeReader = nearRealTimeReader;
        this.modifiedEntityIdsInThisTransaction = modifiedEntityIdsInThisTransaction;

//        Sorter
//        LeafReader sortingReader = SortingLeafReader.wrap( SlowCompositeReaderWrapper.wrap( LuceneIndexReaer), sort);
    }

    @Override
    public ScoreEntityIterator query( String query ) throws ParseException
    {
        ScoreEntityIterator iterator = baseReader.query( query );
        iterator = iterator.filter( entry -> !modifiedEntityIdsInThisTransaction.contains( entry.entityId() ) );
        iterator = mergeIterators( asList( iterator, nearRealTimeReader.query( query ) ) );
        return iterator;
    }

    @Override
    public ScoreEntityIterator queryWithSort( String query, String sortField, String sortDirection ) throws ParseException
    {
        ScoreEntityIterator iterator = baseReader.queryWithSort( query, sortField, sortDirection );
        iterator = iterator.filter( entry -> !modifiedEntityIdsInThisTransaction.contains( entry.entityId() ) );
        iterator = mergeIterators( asList( iterator, nearRealTimeReader.queryWithSort( query, sortField, sortDirection ) ) );
        return iterator;
    }

    /**
     * Used to determine the count when the queried documents have been changed within the transaction.
     *
     * Currently relies on actually querying (searching) Lucene, retrieving a ScoreEntityIterator, and then counting the results.
     * Although this solution does work, could this be improved? Avoid querying for documents and traversing an iterator?
     *
     * Seems like there is no good way to determine the updated count of documents without searching the changed documents.
     * Is it not possible determine updated count from the baseRead and nearRealTimeReader counts alone? Necessary to search Lucene?
     */
    @Override
    public CountResult queryForCount( String query ) throws ParseException
    {
        long count = query( query ).stream().count();
        return new CountResult( count );
    }

    @Override
    public long countIndexedNodes( long nodeId, int[] propertyKeyIds, Value... propertyValues )
    {
        // This is only used in the Consistency Checker. We don't need to worry about this here.
        return 0;
    }

    @Override
    public void close()
    {
        // The 'baseReader' is managed by the kernel, so we don't need to close it here.
        IOUtils.closeAllUnchecked( nearRealTimeReader );
    }
}
