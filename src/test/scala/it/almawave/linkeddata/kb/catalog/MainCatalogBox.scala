package it.almawave.linkeddata.kb.catalog

import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import java.io.File

object MainCatalogBox extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/test/resources/conf/catalog.conf").normalize().toFile())

  val catalog = new CatalogBox(conf)
  catalog.start()

  println("\n#### ontologies")
  catalog.ontologies
    .foreach { onto =>
      if (onto.triples == 0) System.err.println(s"warning: no triples were loaded for ontology: ${onto.id}")
      println(onto)
    }

  println("\n#### vocabularies")
  catalog.vocabularies
    .foreach { voc =>
      if (voc.extract_assetType._1.trim().equals("")) System.err.println(s"warning: no asset type detected for vocabulary: ${voc.id}!")
      if (voc.triples == 0) System.err.println(s"warning: no triples were loaded for vocabulary: ${voc.id}!")
      println(voc)
    }

  println(s"""
    
    #### CATALOG SUMMARY
    
    n° triples in catalog:       ${catalog.triples}
    
    n° ontologies loaded:        ${catalog.ontologies.size}
    n° triples in ontologies:    ${catalog.ontologies.foldLeft(0)(_ + _.triples)}
    
    n° vocabularies loaded:      ${catalog.vocabularies.size}
    n° triples in vocabularies:  ${catalog.vocabularies.foldLeft(0)(_ + _.triples)}
             with dependencies:  ${catalog.vocabulariesWithDependencies().foldLeft(0)(_ + _.triples)}
  
  """)

  //  // DEVELOPMENT ONLY: remove
  //  if (catalog.vocabularies.foldLeft(0)(_ + _.triples) == catalog.vocabulariesWithDependencies().foldLeft(0)(_ + _.triples))
  //    System.err.println("WARNING: no dependencies resolved for vocabularies!")

  catalog.stop()

}
