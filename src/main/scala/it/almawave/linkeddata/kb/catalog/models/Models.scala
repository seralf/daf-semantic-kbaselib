package it.almawave.linkeddata.kb.catalog.models

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import java.net.URL

import org.eclipse.rdf4j.model.Resource
import java.net.URI

import scala.collection.mutable.ListBuffer

// CHECK: ai fini della navigazione bisogna capire se usare solo titles/descriptions it

case class OntologyInformation(meta: OntologyMeta, data: RDFData)

case class VocabularyInformation(meta: VocabularyMeta, data: RDFData)

case class ItemByLanguage(lang: String, value: String)

case class URIWithLabel(value: String, uri: String, lang: String) {

  def this(value: String, uri: URI) = this(value, uri.toString(), "ita")

  def this(uri: String) = this(uri.toString().replaceAll("^.*[#/](.*?)$", "$1"), uri, "ita")

}

case class URIWithValue(value: String, uri: String)

case class Version(
  number:   String,
  date:     String,
  lang:     String,
  comment:  String,
  uri:      String)

// TODO: add a regex / case class extractor for semantic versioning
// TODO: add a proper date format

case class LANG(code: String) // TODO

case class DateInfo(value: String)

case class OntologyMeta(

  id:           String,
  source:       URL,
  url:          URL,
  prefix:       String,
  namespace:    String,
  concepts:     Set[String],
  imports:      Seq[URIWithLabel],

  titles:       Seq[ItemByLanguage],
  descriptions: Seq[ItemByLanguage],

  versions:     Seq[Version],
  creators:     Seq[URIWithLabel],

  // CHECK with provenance
  publishedBy:  Seq[URIWithLabel],
  owners:       Seq[URIWithLabel],
  langs:        Seq[String], // CHECK: LANG
  creationDate: String,
  lastEditDate: String,
  licenses:     Seq[URIWithLabel],

  tags:         Seq[URIWithLabel],
  themes:       Seq[URIWithLabel],
  subthemes:    Seq[URIWithLabel],
  provenance:   Seq[Map[String, Any]],
  hasContributor:       String,
  hasFormalityLevel:    String,
  hasOntologyLanguage:  String,
  hasSemanticAssetDistributions: Seq[URIWithValue],
  hasTasks: Seq[String]) {

  def withID(id_new: String) = {

    OntologyMeta(
      id_new,
      source, url,
      prefix, namespace,
      concepts, imports,
      titles, descriptions,
      versions, creators, publishedBy, owners, langs, creationDate, lastEditDate, licenses,
      tags, themes, subthemes, provenance, hasContributor, hasFormalityLevel,
      hasOntologyLanguage, hasSemanticAssetDistributions, hasTasks)

  }

  def withURL(url_new: URL) = {

    OntologyMeta(
      id,
      source, url_new,
      prefix, namespace,
      concepts, imports,
      titles, descriptions,
      versions, creators, publishedBy, owners, langs, creationDate, lastEditDate, licenses,
      tags, themes, subthemes, provenance, hasContributor, hasFormalityLevel,
      hasOntologyLanguage, hasSemanticAssetDistributions, hasTasks)

  }

}

case class OntologyExtendedMeta(

  id:           String,
  source:       URL,
  url:          URL,
  prefix:       String,
  namespace:    String,
  concepts:     Set[String],
  imports:      Seq[URIWithLabel],

  titles:       Seq[ItemByLanguage],
  descriptions: Seq[ItemByLanguage],

  versions:     Seq[Version],
  creators:     Seq[URIWithLabel],

  // CHECK with provenance
  publishedBy:  Seq[URIWithLabel],
  owners:       Seq[URIWithLabel],
  langs:        Seq[String], // CHECK: LANG
  creationDate: String,
  lastEditDate: String,
  licenses:     Seq[URIWithLabel],

  tags:         Seq[URIWithLabel],
  themes:       Seq[URIWithLabel],
  subthemes:    Seq[URIWithLabel],
  provenance:   Seq[Map[String, Any]],
  hasContributor:       String,
  hasFormalityLevel:    String,
  hasOntologyLanguage:  String,
  hasSemanticAssetDistributions: Seq[URIWithValue],
  hasTasks: Seq[String],
  owlClasses: Seq[OwlClass],
  owlDatatypeProperties: Seq[OwlProperty],
  owlObjectProperties: Seq[OwlProperty]
                               ) {

  def withID(id_new: String) = {

    OntologyExtendedMeta(
      id_new,
      source, url,
      prefix, namespace,
      concepts, imports,
      titles, descriptions,
      versions, creators, publishedBy, owners, langs, creationDate, lastEditDate, licenses,
      tags, themes, subthemes, provenance, hasContributor, hasFormalityLevel,
      hasOntologyLanguage, hasSemanticAssetDistributions, hasTasks, owlClasses, owlDatatypeProperties, owlObjectProperties)

  }

  def withURL(url_new: URL) = {

    OntologyExtendedMeta(
      id,
      source, url_new,
      prefix, namespace,
      concepts, imports,
      titles, descriptions,
      versions, creators, publishedBy, owners, langs, creationDate, lastEditDate, licenses,
      tags, themes, subthemes, provenance, hasContributor, hasFormalityLevel,
      hasOntologyLanguage, hasSemanticAssetDistributions, hasTasks, owlClasses, owlDatatypeProperties, owlObjectProperties)

  }
}

case class OwlClass( isDefinedBy: String, equivalentClass: String, subClassOf: String )

case class OwlProperty( isDefinedBy: String, equivalentProperty: String, subPropertyOf: String )

// TODO: aggiornare i modelli
case class VocabularyMeta(

  id:             String,
  url:            URL,
  source:         URL,
  instances:      Set[String],

  titles:         Seq[ItemByLanguage],
  descriptions:   Seq[ItemByLanguage],

  publishedBy:    Seq[URIWithLabel],
  owners:         Seq[URIWithLabel],
  creators:       Seq[URIWithLabel],

  langs:          Seq[String], // CHECK: LANG
  licenses:       Seq[URIWithLabel],

  versions:       Seq[Version],

  creationDate:   String,
  lastEditDate:   String,
  tags:           Seq[URIWithLabel],
  themes:         Seq[URIWithLabel],
  subthemes:      Seq[URIWithLabel],
  dependencies:   Seq[String], // ontologies from which the vocabulary depends on
  hierarchy:      ListBuffer[Hierarchy],
  distributions:  Seq[Distribution]
)

case class RDFData(
  subjects:   Seq[Resource],
  properties: Seq[IRI],
  objects:    Seq[Value],
  contexts:   Seq[Resource])

// TODO
case class AssetType(assetType: String, representationTechnique: String)

case class Hierarchy (
  codice: String,
  label: String,
  uri: String,
  parent_uri: String,
  children: ListBuffer[Hierarchy]//offspring
)

case class Distribution (
  format:       URIWithValue,
  license:      String,
  downloadUrl:  String,
  accessUrl:    String,
  titles:  Seq[ItemByLanguage],
  descriptions: Seq[ItemByLanguage]
)
  
