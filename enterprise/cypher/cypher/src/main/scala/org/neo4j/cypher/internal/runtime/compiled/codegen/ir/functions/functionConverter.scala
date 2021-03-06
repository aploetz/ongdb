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
package org.neo4j.cypher.internal.runtime.compiled.codegen.ir.functions

import org.neo4j.cypher.internal.runtime.compiled.codegen.CodeGenContext
import org.neo4j.cypher.internal.runtime.compiled.codegen.ir.expressions._
import org.neo4j.cypher.internal.compiler.v3_6.planner.CantCompileQueryException
import org.neo4j.cypher.internal.v3_6.{expressions => ast}
import org.neo4j.cypher.internal.v3_6.expressions.{functions => astFunctions}

object functionConverter {

  def apply(fcn: ast.FunctionInvocation, callback: ast.Expression => CodeGenExpression)
           (implicit context: CodeGenContext): CodeGenExpression = fcn.function match {

    // id(n)
    case astFunctions.Id =>
      assert(fcn.args.size == 1)
      IdCodeGenFunction(callback(fcn.args(0)))

    // type(r)
    case astFunctions.Type =>
      assert(fcn.args.size == 1)
      TypeCodeGenFunction(callback(fcn.args(0)))

    case other => throw new CantCompileQueryException(s"Function $other not yet supported")
  }
}



