/*
 * Copyright (c) 2002-2016 "Neo Technology,"
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
package org.neo4j.cypher.docgen

import org.junit.Test
import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.graphdb.Label
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.procedure.example.IndexingProcedure

class CallTest extends DocumentingTestBase with QueryStatisticsTestSupport with HardReset {

  var nodeId:Long = -1

  override def section = "Call"

  override def hardReset() = {
    super.hardReset()
    db.getDependencyResolver.resolveDependency(classOf[Procedures]).registerProcedure( classOf[IndexingProcedure] )
    db.inTx {
      val node = db.createNode(Label.label("User"), Label.label("Administrator"))
      node.setProperty("name", "Adrian")
      nodeId = node.getId
    }
  }

  @Test def call_a_procedure() {
    testQuery(
      title = "Call a procedure",
      text = "This calls the built-in procedure `db.labels`, which lists all labels used in the database.",
      queryText = "CALL db.labels",
      optionalResultExplanation = "",
      assertions = (p) => assert(p.nonEmpty) )
  }

  @Test def call_a_procedure_name_quoting() {
    testQuery(
      title = "Call a procedure using a quoted namespace and name",
      text = "This calls the built-in procedure `db.labels`, which lists all labels used in the database.",
      queryText = "CALL `db`.`labels`",
      optionalResultExplanation = "",
      assertions = (p) => assert(p.nonEmpty) )
  }

  @Test def call_a_procedure_within_a_complex_query() {
    testQuery(
      title = "Call a procedure within a complex query",
      text = "This calls the built-in procedure `db.labels` to count all labels used in the database.",
      planners = Seq(""),
      queryText = "CALL db.labels() YIELD label RETURN count(label) AS numLabels",
      optionalResultExplanation =
        "Since the procedure call is part of a larger query, all outputs must be named explicitly.",
      assertions = (p) => assert(p.nonEmpty) )
  }

  @Test def call_a_procedure_and_filter_its_results() {
    testQuery(
      title = "Call a procedure and filter its results",
      text = "This calls the built-in procedure `db.labels` to count all in-use labels in the database that contain the word 'User'",
      planners = Seq(""),
      queryText = "CALL db.labels() YIELD label WHERE label CONTAINS 'User' RETURN count(label) AS numLabels",
      optionalResultExplanation =
        "Since the procedure call is part of a larger query, all outputs must be named explicitly.",
      assertions = (p) => assert(p.nonEmpty) )
  }

  @Test def call_a_procedure_within_a_complex_query_and_rename_outputs() {
    testQuery(
      title = "Call a procedure within a complex query and rename its outputs",
      text =
        "This calls the built-in procedure `db.propertyKeys` as part of counting " +
        "the number of nodes per property key that is currently used in the database.",
      planners = Seq(""),
      queryText = "CALL db.propertyKeys() YIELD propertyKey AS prop " +
                  "MATCH (n) WHERE n[prop] IS NOT NULL " +
                  "RETURN prop, count(n) AS numNodes",
      optionalResultExplanation =
        "Since the procedure call is part of a larger query, all outputs must be named explicitly.",
      assertions = (p) => assert(p.nonEmpty) )
  }

  @Test def call_a_procedure_with_literal_arguments() {
    testQuery(
      title = "Call a procedure with literal arguments",
      text = "This calls the example procedure `org.neo4j.procedure.example.addNodeToIndex` using literal arguments, i.e. arguments that are written out directly in the statement text.",
      queryText = "CALL org.neo4j.procedure.example.addNodeToIndex('users', "+nodeId+", 'name')",
      optionalResultExplanation = "Since our example procedure does not return any result, the result is empty.",
      assertions = (p) => assert(p.isEmpty) )
  }

  @Test def call_a_procedure_with_parameter_arguments() {
    testQuery(
      title = "Call a procedure with parameter arguments",
      text = "This calls the example procedure `org.neo4j.procedure.example.addNodeToIndex` using parameters as arguments. Each procedure argument is taken to be the value of a corresponding statement parameters with the same name (or null if no such parameter has been given).",
      queryText = "CALL org.neo4j.procedure.example.addNodeToIndex",
      parameters = Map("indexName"->"users", "node"->nodeId, "propKey"-> "name"),
      optionalResultExplanation = "Since our example procedure does not return any result, the result is empty.",
      assertions = (p) => assert(p.isEmpty) )
  }

  @Test def call_a_procedure_with_mixed_arguments() {
    testQuery(
      title = "Call a procedure with mixed literal and parameter arguments",
      text = "This calls the example procedure `org.neo4j.procedure.example.addNodeToIndex` using both literal and parameter arguments.",
      queryText = "CALL org.neo4j.procedure.example.addNodeToIndex('users', $node, 'name')",
      parameters = Map("node"->nodeId),
      optionalResultExplanation = "Since our example procedure does not return any result, the result is empty.",
      assertions = (p) => assert(p.isEmpty) )
  }

  @Test def call_a_procedure_with_literal_and_default_arguments() {
    testQuery(
      title = "Call a procedure with literal and default arguments",
      text = "This calls the example procedure `org.neo4j.procedure.example.addNodeToIndex` using literal arguments, i.e. arguments that are written out directly in the statement text, and a trailing default argument that is provided by the procedure itself.",
      queryText = "CALL org.neo4j.procedure.example.addNodeToIndex('users', "+nodeId+")",
      optionalResultExplanation = "Since our example procedure does not return any result, the result is empty.",
      assertions = (p) => assert(p.isEmpty) )
  }
}
