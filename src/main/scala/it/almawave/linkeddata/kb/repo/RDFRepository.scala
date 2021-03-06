package it.almawave.linkeddata.kb.repo

import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.nio.file.Paths
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.slf4j.LoggerFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import it.almawave.linkeddata.kb.utils.RDF4JAdapters.StringContextAdapter
import it.almawave.linkeddata.kb.utils.TryHandlers.TryLog
import virtuoso.rdf4j.driver.VirtuosoRepository // TODO: check dependency
import it.almawave.linkeddata.kb.repo.managers.RDFFileManager
import it.almawave.linkeddata.kb.repo.managers.RDFStoreManager
import it.almawave.linkeddata.kb.repo.managers.PrefixesManager
import it.almawave.linkeddata.kb.repo.managers.SPARQLManager
import scala.concurrent.Future
import it.almawave.linkeddata.kb.utils.TryHandlers.FutureWithLog
import scala.util.Try
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer

// TODO: refactorization using trait!!
trait RDFRepository

object RDFRepository {

  val logger = LoggerFactory.getLogger(this.getClass)

  def remote(endpoint: String) = {
    new RDFRepositoryBase(new SPARQLRepository(endpoint, endpoint))
  }

  def memory(): RDFRepositoryBase = {

    //    val mem = new MemoryStore
    //    val repo: Repository = new SailRepository(mem)
    //    new RDFRepositoryBase(repo)

    memory(false)

  }

  /*
   *  in-memory repository, with "full RDFS reasoning"
   */
  def memory(rdfs_inference: Boolean): RDFRepositoryBase = {
    val mem = new MemoryStore
    val repo: SailRepository = if (rdfs_inference)
      new SailRepository(new ForwardChainingRDFSInferencer(new DedupingInferencer(mem)))
    else
      new SailRepository(mem)
    new RDFRepositoryBase(repo)
  }

  // TODO: config
  def memory(dir_cache: String): RDFRepositoryBase = {

    val dataDir = Paths.get(dir_cache).normalize().toAbsolutePath().toFile()
    if (!dataDir.exists())
      dataDir.mkdirs()
    val mem = new MemoryStore()
    mem.setDataDir(dataDir)
    mem.setSyncDelay(1000L)
    mem.setPersist(false)
    mem.setConnectionTimeOut(1000) // TODO: set a good timeout!

    // IDEA: see how to trace statements added by inferencing
    // CHECK val inferencer = new DedupingInferencer(new ForwardChainingRDFSInferencer(new DirectTypeHierarchyInferencer(mem)))
    // SEE CustomGraphQueryInferencer

    val repo = new SailRepository(mem)
    new RDFRepositoryBase(repo)
  }

  // VERIFY: virtuoso jar dependencies on maven central
  def virtuoso(): RDFRepositoryBase = {
    // TODO: externalize configurations
    // TODO: add a factory for switching between dev / prod
    val host = "localhost"
    val port = 1111
    val username = "dba"
    val password = "dba"

    val repo = new VirtuosoRepository(s"jdbc:virtuoso://${host}:${port}/charset=UTF-8/log_enable=2", username, password)
    new RDFRepositoryBase(repo)
  }

  /* DISABLED */
  def solr(): RDFRepositoryBase = {

    //    val index = new SolrIndex()
    //    val sailProperties = new Properties()
    //    sailProperties.put(SolrIndex.SERVER_KEY, "embedded:")
    //    index.initialize(sailProperties)
    //    val client = index.getClient()
    //
    //    val memoryStore = new MemoryStore()
    //    // enable lock tracking
    //    org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true)
    //    val lucenesail = new LuceneSail()
    //    lucenesail.setBaseSail(memoryStore)
    //    lucenesail.setLuceneIndex(index)
    //
    //    val repo = new SailRepository(lucenesail)

    throw new RuntimeException("this method is currently disabled")
  }

}
