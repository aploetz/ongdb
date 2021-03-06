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
package org.neo4j.causalclustering.core.consensus;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.neo4j.causalclustering.messaging.Inbound;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;

import static java.lang.String.format;

@ChannelHandler.Sharable
public class RaftMessageNettyHandler extends SimpleChannelInboundHandler<RaftMessages.ReceivedInstantClusterIdAwareMessage<?>>
        implements Inbound<RaftMessages.ReceivedInstantClusterIdAwareMessage<?>>
{
    private Inbound.MessageHandler<RaftMessages.ReceivedInstantClusterIdAwareMessage<?>> actual;
    private Log log;

    public RaftMessageNettyHandler( LogProvider logProvider )
    {
        this.log = logProvider.getLog( getClass() );
    }

    @Override
    public void registerHandler( Inbound.MessageHandler<RaftMessages.ReceivedInstantClusterIdAwareMessage<?>> actual )
    {
        this.actual = actual;
    }

    @Override
    protected void channelRead0( ChannelHandlerContext channelHandlerContext, RaftMessages.ReceivedInstantClusterIdAwareMessage<?> incomingMessage )
    {
        try
        {
            actual.handle( incomingMessage );
        }
        catch ( Exception e )
        {
            log.error( format( "Failed to process message %s", incomingMessage ), e );
        }
    }
}
