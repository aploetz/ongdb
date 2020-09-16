/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j"
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
package org.neo4j.kernel.enterprise.api.security;

import java.util.Map;

import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.kernel.api.security.AuthManager;
import org.neo4j.kernel.api.security.AuthToken;
import org.neo4j.kernel.api.security.exception.InvalidAuthTokenException;

public abstract class EnterpriseAuthManager extends AuthManager
{

    /**
     * Implementation that does no authentication.
     */
    public static EnterpriseAuthManager NO_AUTH = new EnterpriseAuthManager()
    {
        public EnterpriseLoginContext login( Map<String,Object> authToken )
        {
            AuthToken.clearCredentials( authToken );
            return EnterpriseLoginContext.AUTH_DISABLED;
        }

        @Override
        public void clearAuthCache()
        {
        }

        @Override
        public void log( String message, SecurityContext securityContext )
        {
        }
    };

    public abstract void clearAuthCache();

    @Override
    public abstract EnterpriseLoginContext login( Map<String,Object> authToken ) throws InvalidAuthTokenException;
}
