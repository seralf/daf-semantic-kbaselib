package it.almawave.linkeddata.kb.catalog

import com.typesafe.config.Config

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import scala.collection.mutable.ListBuffer
import java.net.URI
import java.nio.file.Paths
import java.net.URL
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer
import org.eclipse.rdf4j.common.iteration.Iterations
import org.eclipse.rdf4j.sail.federation.Federation
import org.eclipse.rdf4j.sail.Sail
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FileUtils
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.eclipse.jgit.merge.MergeStrategy

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

/*
 * TODO: consider using inferences
 */
class CatalogBox(config: Config) extends RDFBox {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  val federation = new Federation()
  //    new ForwardChainingRDFSInferencer(new DedupingInferencer(federation))
  override val repo: Repository = new SailRepository(federation)

  override val conf = config.resolve()

  private val _ontologies = new ListBuffer[OntologyBox]
  private val _vocabularies = new ListBuffer[VocabularyBox]
  private val _remotes = new ListBuffer[RemoteOntologyBox]

  def ontologies = _ontologies.toList
  def vocabularies = _vocabularies.toList

  // REVIEW HERE ............................................................................................

  val store = {
    val root = Paths.get(conf.getString("ontologies.path_local")).normalize().toAbsolutePath().toFile()
    new RDFFilesStore(root)
  }

  val git = GitHandler(conf.getConfig("git"))

  // REVIEW HERE ............................................................................................

  override def start() {

    // synchronize with remote git repository
    // CHECK: future -> Await.ready(git.synchronize(), Duration.Inf)
    if (conf.getBoolean("git.synchronize")) git.synchronize()

    if (!repo.isInitialized()) repo.initialize()

    // load ontologies!
    this.load_ontologies

    // load vocabularies!
    this.load_vocabularies

    // starts the different kbboxes
    (_ontologies ++ _vocabularies) foreach (_.start())

    // adding triples to global federated repository
    _ontologies.foreach { x => federation.addMember(x.repo) }
    _vocabularies.foreach { x => federation.addMember(x.repo) }
    _remotes.foreach { x => federation.addMember(x.repo) }

  }

  override def stop() {

    // starts the different kbboxes
    (_ontologies ++ _vocabularies) foreach (_.stop())

    if (repo.isInitialized()) repo.shutDown()
  }

  override def triples = {
    _ontologies.foldLeft(0)((a, b) => a + b.triples) + _vocabularies.foldLeft(0)((a, b) => a + b.triples)
  }

  def getVocabularyByID(vocabularyID: String) = Try {
    this._vocabularies.toStream.filter(_.id.equals(vocabularyID)).head
  }

  private def load_ontologies = {

    val base_path: URI = if (conf.getBoolean("ontologies.use_cache"))
      Paths.get(conf.getString("ontologies.path_local")).normalize().toAbsolutePath().toUri()
    else
      new URI(conf.getString("ontologies.path_remote"))

    if (conf.hasPath("ontologies.data")) {

      logger.info(s"using selected ontologies")

      conf.getConfigList("ontologies.data")
        .toStream
        .foreach { onto_conf =>
          val source_path = onto_conf.getString("path")
          val source_url = new URI(base_path + source_path).normalize().toURL()
          val box = OntologyBox.parse(source_url)
          box.start()
          box.stop()
          _ontologies += box
        }

    } else {

      logger.info(s"using all available ontologies")

      store.ontologies().foreach { f =>
        val box = OntologyBox.parse(f.toURI().toURL())
        box.start()
        box.stop()
        _ontologies += box
      }

    }

    _ontologies.toStream

  }

  // TODO: same for vocabulary?
  private def getOntologyBoxByContext(context: String) = Try {
    _ontologies.toStream.filter(_.context.equals(context)).head
  }

  private def load_vocabularies = {

    val base_path: URI = if (conf.getBoolean("vocabularies.use_cache"))
      Paths.get(conf.getString("vocabularies.path_local")).normalize().toAbsolutePath().toUri()
    else
      new URI(conf.getString("vocabularies.path_remote"))

    if (conf.hasPath("vocabularies.data")) {

      logger.info(s"using selected vocabularies")

      conf.getConfigList("vocabularies.data")
        .toStream
        .foreach { voc_conf =>

          val source_path = voc_conf.getString("path")
          val source_url = new URI(base_path + source_path).normalize().toURL()
          val box = VocabularyBox.parse(source_url)
          box.start()
          box.stop()
          _vocabularies += box
        }

    } else {

      logger.info(s"using all available vocabularies")

      store.vocabularies().foreach { f =>
        val box = VocabularyBox.parse(f.toURI().toURL())
        box.start()
        box.stop()
        _vocabularies += box
      }

    }

    _vocabularies.toStream

  }

  // CHECK: repository <all>

  def vocabulariesWithDependencies(): Seq[VocabularyBox] = {

    this.vocabularies.map { vbox =>

      // getting the vocabulary id
      val vocID = vbox.id

      // find vocabulary by id
      var voc_box = this.getVocabularyByID(vocID).get

      val triples_no_deps = voc_box.triples

      // resolve internal dependencies
      val ontos = this.resolve_dependencies(voc_box)

      // federation with repositories
      voc_box = voc_box.federateWith(ontos)

      val triples_deps = voc_box.triples

      voc_box
    }

  }

  // resolve only internal dependencies
  private def internal_dependencies(voc_box: VocabularyBox): Stream[String] = {

    val onto_baseURI = this.conf.getString("ontologies.baseURI").trim()
    voc_box.meta.dependencies.toStream.filter { d => d.startsWith(onto_baseURI) }

    // TODO
    //    if (conf.getValue("ontologies.baseURI").isInstanceOf[String]) {
    //
    //      val onto_baseURI = this.conf.getString("ontologies.baseURI").trim()
    //      voc_box.meta.dependencies.toStream.filter { d => d.startsWith(onto_baseURI) }
    //
    //    } else {
    //
    //      //    HACK: aggiunta multipli base URI per consentire sviluppo nuovi vocabolari / ontologie (pre-rifattorizzazione)
    //      //    val onto_baseURIs = this.conf.getStringList("ontologies.baseURI")
    //      //    voc_box.meta.dependencies.toStream.filterNot { d => onto_baseURIs.filter(x => d.startsWith(x)) }
    //
    //      val onto_baseURI = this.conf.getStringList("ontologies.baseURI")(0).trim()
    //      voc_box.meta.dependencies.toStream.filter { d => d.startsWith(onto_baseURI) }
    //
    //    }

  }

  private def resolve_dependencies(voc_box: VocabularyBox): Seq[OntologyBox] = {

    // list of dependencies
    val deps = this.internal_dependencies(voc_box).map(new URL(_))

    // boxes found
    deps.flatMap { dep_ctx =>
      this.ontologies.toList
        .filter(_.context.trim().equals(dep_ctx.toString().trim()))
    }

  }

}

