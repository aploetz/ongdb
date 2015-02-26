/**
 * Copyright (c) 2002-2015 "Neo Technology,"
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
package org.neo4j.cypher.internal.compiler.v2_2.planner.logical

import org.neo4j.cypher.internal.compiler.v2_2.planner.QueryGraph
import org.neo4j.cypher.internal.compiler.v2_2.planner.logical.plans.LogicalPlan

object expandTableSolver extends ExhaustiveTableSolver {

  import org.neo4j.cypher.internal.compiler.v2_2.planner.logical.ExhaustiveQueryGraphSolver.planSinglePatternSide

  override def apply(qg: QueryGraph, goal: Set[Solvable], table: (Set[Solvable]) => Option[LogicalPlan]): Iterator[LogicalPlan] = {
    val result =
      for(
        solvable <- goal.iterator;
        pattern <- Solvable.relationship(solvable);
        solved = goal - solvable;
        plan <- table(solved) // if !plan.solved.graph.patternRelationships(pattern)
      ) yield {
          Iterator(
            planSinglePatternSide(qg, pattern, plan, pattern.left),
            planSinglePatternSide(qg, pattern, plan, pattern.right)
          ).flatten
      }
      result.flatten
  }
}
