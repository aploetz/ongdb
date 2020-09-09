package org.neo4j.kernel.api.impl.fulltext;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;

import java.io.IOException;

import org.neo4j.kernel.impl.util.FulltextSortType;

public class FulltextUtils
{
    public static Sort buildSort( String sortFieldString, String sortType )
    {
        return buildSort( sortFieldString, sortType, false );
    }

    public static Sort buildSort( String sortFieldString, String sortType, boolean reverseSortOrder )
    {

        SortField sortField;
        FulltextSortType sortTypeEnum = FulltextSortType.valueOfIgnoreCase( sortType );

        if ( sortTypeEnum == null )
        {
            throw new RuntimeException( "Unable to determine sortField type '" + sortType + "'." );
        }

        return buildSort( sortFieldString, sortTypeEnum, reverseSortOrder );
    }

    public static Sort buildSort( String sortFieldString, FulltextSortType sortTypeEnum, boolean reverseSortOrder )
    {
        SortField sortField;
        switch ( sortTypeEnum )
        {
        case LONG:
            sortField = new SortedNumericSortField( sortFieldString, SortField.Type.LONG, reverseSortOrder );
            if ( !reverseSortOrder )
            {
                sortField.setMissingValue( Long.MAX_VALUE );
            }
            break;
        case DOUBLE:
            sortField = new SortedNumericSortField( sortFieldString, SortField.Type.DOUBLE, reverseSortOrder );
            if ( !reverseSortOrder )
            {
                sortField.setMissingValue( Double.MAX_VALUE );
            }
            break;
        case STRING:
            sortField = new SortField( sortFieldString, SortField.Type.STRING, reverseSortOrder );
            if ( !reverseSortOrder )
            {
                sortField.setMissingValue( SortField.STRING_LAST );
            }
            break;
        default:
            throw new RuntimeException( "Missing sortTypeEnum case in FulltextUtils." );
        }
        return new Sort( sortField );
    }
}
