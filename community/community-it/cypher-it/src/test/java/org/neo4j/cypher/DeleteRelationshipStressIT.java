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
package org.neo4j.cypher;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.rule.ImpermanentDatabaseRule;

import static org.neo4j.graphdb.Label.label;

public class DeleteRelationshipStressIT
{
    private final ExecutorService executorService = Executors.newFixedThreadPool( 10 );

    @Rule
    public ImpermanentDatabaseRule db = new ImpermanentDatabaseRule();

    @Before
    public void setup()
    {
        for ( int i = 0; i < 100; i++ )
        {
            try ( Transaction tx = db.beginTx() )
            {

                Node prev = null;
                for ( int j = 0; j < 100; j++ )
                {
                    Node node = db.createNode( label( "L" ) );

                    if ( prev != null )
                    {
                        Relationship rel = prev.createRelationshipTo( node, RelationshipType.withName( "T" ) );
                        rel.setProperty( "prop", i + j );
                    }
                    prev = node;
                }
                tx.success();
            }
        }
    }

    @After
    public void tearDown()
    {
        executorService.shutdown();
    }

    @Test
    public void shouldBeAbleToReturnRelsWhileDeletingRelationship() throws InterruptedException, ExecutionException
    {
        // Given
        Future query1 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) OPTIONAL MATCH (:L)-[:T {prop:1337}]-(:L) WITH r MATCH ()-[r]-() return r" );
        Future query2 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) DELETE r" );

        // When
        query1.get();
        query2.get();
    }

    @Test
    public void shouldBeAbleToGetPropertyWhileDeletingRelationship() throws InterruptedException, ExecutionException
    {
        // Given
        Future query1 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) OPTIONAL MATCH (:L)-[:T {prop:1337}]-(:L) WITH r MATCH ()-[r]-() return r.prop" );
        Future query2 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) DELETE r" );

        // When
        query1.get();
        query2.get();
    }

    @Test
    public void shouldBeAbleToCheckPropertiesWhileDeletingRelationship() throws InterruptedException, ExecutionException
    {
        // Given
        Future query1 =
                executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) OPTIONAL MATCH (:L)-[:T {prop:1337}]-(:L) WITH r MATCH ()-[r]-() return exists(r.prop)" );
        Future query2 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) DELETE r" );

        query1.get();
        query2.get();
    }

    @Test
    public void shouldBeAbleToRemovePropertiesWhileDeletingRelationship() throws InterruptedException, ExecutionException
    {
        // Given
        Future query1 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) OPTIONAL MATCH (:L)-[:T {prop:1337}]-(:L) WITH r MATCH ()-[r]-() REMOVE r.prop" );
        Future query2 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) DELETE r" );

        // When
        query1.get();
        query2.get();
    }

    @Test
    public void shouldBeAbleToSetPropertiesWhileDeletingRelationship() throws InterruptedException, ExecutionException
    {
        // Given
        Future query1 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) OPTIONAL MATCH (:L)-[:T {prop:1337}]-(:L) WITH r MATCH ()-[r]-() SET r.foo = 'bar'" );
        Future query2 = executeInThread( "MATCH (:L)-[r:T {prop:42}]-(:L) DELETE r" );

        // When
        query1.get();
        query2.get();
    }

    private Future executeInThread( final String query )
    {
        return executorService.submit( () -> db.execute( query ).resultAsString() );
    }
}
