package refactoring.triplestore

import java.nio.file.Paths

/**
 * this example should be extended to expose a minimal, simple SPARQL endpoint
 */
object MainMockRepo extends App {

  val rdf_url = Paths.get("src/test/resources/catalog-data/VocabolariControllati/territorial-classifications/regions/regions.ttl")
    .toAbsolutePath().normalize()
    .toUri().toURL()

  println("rdf_url: " + rdf_url)

  val mock = new MockRepo()
  mock.start()

  mock.endpoint.update(s"""
    LOAD <${rdf_url.toURI()}>
    INTO GRAPH <test://regions.ttl>
  """)

  val _query2 = """
    CONSTRUCT { ?s ?p ?o } 
    WHERE {
      ?s a ?concept .
      ?s ?p ?o .
    }  
  """

  val _query = """
    SELECT ?concept (COUNT(?s) AS ?triples) ?graph 
    WHERE {
      GRAPH ?graph {
        ?s a ?concept .
        ?s ?p ?o .
      }
    }  
    GROUP BY ?graph ?concept 
  """

  //  mock.check_query(_query)

  mock.endpoint.query(_query, "csv")(System.out)

  mock.stop()

}