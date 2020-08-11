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
package org.neo4j.server.rest;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.neo4j.configuration.Config;
import org.neo4j.server.configuration.ServerSettings;
import org.neo4j.server.modules.ServerModule;
import org.neo4j.server.web.WebServer;

public class DatabaseRoleInfoServerModule implements ServerModule
{
    private final WebServer server;
    private final Config config;

    public DatabaseRoleInfoServerModule( WebServer server, Config config )
    {
        this.server = server;
        this.config = config;
    }

    private static List<Class<?>> jaxRsClasses()
    {
        return Collections.emptyList();
        //return List.of( CausalClusteringService.class );
    }

    public void start()
    {
        String mountPoint = this.mountPoint();
        this.server.addJAXRSClasses( jaxRsClasses(), mountPoint, null );
    }

    public void stop()
    {
        this.server.removeJAXRSClasses( jaxRsClasses(), this.mountPoint() );
    }

    private String mountPoint()
    {
        return ((URI) this.config.get( ServerSettings.db_api_path )).toString();
    }
}
