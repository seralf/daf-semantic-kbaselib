package refactoring.triplestore

import java.net.URL
import java.io.File
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter
import scala.util.Try
import java.io.OutputStream
import java.nio.file.Paths
import org.slf4j.LoggerFactory
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.parser.QueryParserUtil
import org.eclipse.rdf4j.query.resultio.QueryResultIO
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery
import scala.concurrent.Future
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat
import org.eclipse.rdf4j.query.resultio.QueryResultFormat
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat

class MockRepo(baseURI: String = "https://baseURI/") {

  import scala.concurrent.ExecutionContext.Implicits._

  // TODO: https://baseURI/ - .well-known

  val logger = LoggerFactory.getLogger(this.getClass)
  val repo = new SailRepository(new MemoryStore)

  def start() = {
    logger.info("#### RDF repository START")
    if (!repo.isInitialized()) repo.initialize()
  }

  def stop() = {
    logger.info("#### RDF repository STOP")
    if (repo.isInitialized()) repo.shutDown()
  }

  object endpoint {

    def update(query: String) = Try {

      val conn = repo.getConnection
      conn.begin()
      conn.prepareUpdate(QueryLanguage.SPARQL, query, baseURI).execute()
      conn.commit()
      conn.close()

    }

    def query(query: String, format: String = "csv")(out: OutputStream) = Try {

      val parsed = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, baseURI)

      val conn = repo.getConnection

      parsed match {

        case tuples: ParsedTupleQuery =>
          logger.debug("SPARQL> ParsedTupleQuery")

          val result_format: QueryResultFormat = QueryResultIO.getWriterFormatForFileName("DUMP." + format)
            .orElse(TupleQueryResultFormat.CSV)
          val writer = QueryResultIO.createTupleWriter(result_format, out)
          conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate(writer)

        case bool: ParsedBooleanQuery =>
          logger.debug("SPARQL> ParsedBooleanQuery")

          val result_format: QueryResultFormat = QueryResultIO.getBooleanWriterFormatForFileName("DUMP." + format)
            .orElse(BooleanQueryResultFormat.TEXT)
          val writer = QueryResultIO.createWriter(result_format, out)
          val result = conn.prepareBooleanQuery(QueryLanguage.SPARQL, query).evaluate()
          out.write(result.toString().getBytes)

        case graph: ParsedGraphQuery =>
          logger.debug("SPARQL> ParsedGraphQuery")

          val result_format: RDFFormat = Rio.getWriterFormatForFileName("DUMP." + format)
            .orElse(RDFFormat.NTRIPLES)
          val writer = Rio.createWriter(result_format, out)
          conn.prepareGraphQuery(QueryLanguage.SPARQL, query, baseURI).evaluate(writer)

        case _ =>
          logger.debug("SPARQL> error parsing query")
          throw new RuntimeException(s"can't parse query\n${query}\nwith format ${format}")

      }

      conn.close() // SEE: side-effects monad unsing(closable){...}

    }

  }

}
