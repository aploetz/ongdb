/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.cluster.protocol.atomicbroadcast.multipaxos.context;

import org.junit.Test;

import org.neo4j.cluster.protocol.cluster.ClusterConfiguration;
import org.neo4j.cluster.protocol.heartbeat.HeartbeatContext;

import static org.neo4j.cluster.protocol.atomicbroadcast.multipaxos.ClusterProtocolAtomicbroadcastTestUtil.ids;
import static org.neo4j.cluster.protocol.atomicbroadcast.multipaxos.ClusterProtocolAtomicbroadcastTestUtil.members;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AtomicBroadcastContextImplTest
{
    @Test
    public void shouldHasQuorumWhenTwoMachinesAliveInAClusterWithThreeMachines()
    {
        //Given
        HeartbeatContext heartbeatContext = mock( HeartbeatContext.class );
        CommonContextState commonState = mock( CommonContextState.class );
        ClusterConfiguration configuration = mock(ClusterConfiguration.class);

        when( heartbeatContext.getAlive() ).thenReturn( ids( 2 ) );
        when( commonState.configuration() ).thenReturn( configuration );
        when( configuration.getMembers() ).thenReturn( members( 3 ) );

        AtomicBroadcastContextImpl context = new AtomicBroadcastContextImpl( null, commonState, null, null, null,
                heartbeatContext ); // we do not care about other args
        //When
        boolean hasQuorum = context.hasQuorum();
        //Then
        assertTrue( hasQuorum );
    }

    @Test
    public void shouldHasNoQuorumWhenOneMachineAliveInAClusterWithThreeMachines()
    {
        //Given
        HeartbeatContext heartbeatContext = mock( HeartbeatContext.class );
        CommonContextState commonState = mock( CommonContextState.class );
        ClusterConfiguration configuration = mock( ClusterConfiguration.class );

        when( heartbeatContext.getAlive() ).thenReturn( ids( 1 ) );
        when( commonState.configuration() ).thenReturn( configuration );
        when( configuration.getMembers() ).thenReturn( members( 3 ) );

        AtomicBroadcastContextImpl context = new AtomicBroadcastContextImpl( null, commonState, null, null, null,
                heartbeatContext ); // we do not care about other args
        //When
        boolean hasQuorum = context.hasQuorum();
        //Then
        assertFalse( hasQuorum );
    }

    @Test
    public void shouldHasQuorumWhenOneMachineAliveInAClusterWithOneMachine()
    {
        //Given
        HeartbeatContext heartbeatContext = mock( HeartbeatContext.class );
        CommonContextState commonState = mock( CommonContextState.class );
        ClusterConfiguration configuration = mock( ClusterConfiguration.class );

        when( heartbeatContext.getAlive() ).thenReturn( ids( 1 ) );
        when( commonState.configuration() ).thenReturn( configuration );
        when( configuration.getMembers() ).thenReturn( members( 1 ) );

        AtomicBroadcastContextImpl context = new AtomicBroadcastContextImpl( null, commonState, null, null, null,
                heartbeatContext ); // we do not care about other args
        //When
        boolean hasQuorum = context.hasQuorum();
        //Then
        assertTrue( hasQuorum );
    }
}
