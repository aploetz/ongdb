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
package org.neo4j.procedure.builtin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

<<<<<<< HEAD
import org.neo4j.storageengine.api.StoreId;
=======
import org.neo4j.storageengine.api.StoreIdProvider;
>>>>>>> neo4j/4.1
import org.neo4j.string.HexString;

import static java.lang.String.format;

<<<<<<< HEAD
public class StoreIdDecodeUtils
=======
class StoreIdDecodeUtils
>>>>>>> neo4j/4.1
{
    private static final String DEFAULT_ALGORITHM = "SHA-256";

    private StoreIdDecodeUtils()
    {
    }

<<<<<<< HEAD
    public static String decodeId( StoreId storeId ) throws NoSuchAlgorithmException
    {
        var messageDigest = MessageDigest.getInstance( DEFAULT_ALGORITHM );
        var storeIdString = format( "%d%d%d", storeId.getCreationTime(), storeId.getRandomId(), storeId.getStoreVersion() );
=======
    static String decodeId( StoreIdProvider storeIdProvider ) throws NoSuchAlgorithmException
    {
        var externalStoreId = storeIdProvider.getExternalStoreId();
        var storeId = storeIdProvider.getStoreId();
        var storeIdString = externalStoreId.isPresent() ? externalStoreId.get().toString()
                                        : format( "%d%d%d", storeId.getCreationTime(), storeId.getRandomId(), storeId.getStoreVersion() );
        var messageDigest = MessageDigest.getInstance( DEFAULT_ALGORITHM );
>>>>>>> neo4j/4.1
        messageDigest.update( storeIdString.getBytes() );
        return HexString.encodeHexString( messageDigest.digest() );
    }
}
