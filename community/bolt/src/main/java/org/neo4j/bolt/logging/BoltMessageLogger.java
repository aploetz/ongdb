/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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
package org.neo4j.bolt.logging;

import java.util.Map;
import java.util.function.Supplier;

import org.neo4j.kernel.api.exceptions.Status;

public interface BoltMessageLogger
{
    void clientEvent( String eventName );

    void clientEvent( String eventName, Supplier<String> detailsSupplier );

    void clientError( String eventName, String message, Supplier<String> detailsSupplier );

    void serverEvent( String eventName );

    void serverEvent( String eventName, Supplier<String> detailsSupplier );

    void serverError( String eventName, String message );

    void serverError( String eventName, Status status, String message );

    void init( String userAgent, Map<String,Object> authToken );

    void run( String statement, Map<String,Object> parameters );

    void pullAll();

    void discardAll();

    void ackFailure();

    void reset();

    void success( Object metadata );

    void failure( Status status, String message );

    void ignored();

    void record( Object arg1 );
}
