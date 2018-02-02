package it.almawave.linkeddata.kb.catalog.meta

import java.net.URL
import it.almawave.linkeddata.kb.parsers.meta.OntologyMetadataExtractor
import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.utils.JSONHelper

object MainOntologyMetadataExtractor extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")
  println(s"\n\nextracting informations from\n${url}\n...")

  //  val repo = new RDFFileRepository(url)
  val results = OntologyMetadataExtractor(url)
  println(results)

  val json = JSONHelper.writeToString(results.informations().meta)
  println(json)

}