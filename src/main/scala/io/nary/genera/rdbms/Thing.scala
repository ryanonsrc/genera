package io.nary.genera.rdbms

import scala.slick.driver.PostgresDriver.simple._

import io.nary.genera.{Singleton, StoredSingleton, GeneraContent, GeneraContext}
import io.nary.genera.GeneraContentConversions._
import io.nary.genera.rdbms.Things._

/**
 * Created by ryan on 6/1/14.
 */
class SlickContext(implicit val session: Session) extends GeneraContext[SlickContext] with TransactionCapable {
  /**
   * content string and lazily-constructed thing for all dictionary keys in the database
   */
  protected[rdbms] val dictionaryKeyMarkerString = "meta-marker: dict-key"

  /**
   * content string and lazily-constructed thing for all group children in the database
   */
  protected[rdbms] val groupChildMarkerString = "meta-marker: child->parent"


  /**
   * content string and lazily-constructed thing for kV values, that is also a group child in the database
   */
  protected[rdbms] val groupChildDictionaryValueMarkerString = "meta-marker: child->parent && dict-value"

  /**
   * Represents a relation and meta-relation pair.
   */
  protected[rdbms] case class Relation(thing: Thing, metaThing: Option[Thing] = None)


  /**
   * general method for building things
   */
  protected[rdbms] def makeThing(content: GeneraContent, relation: Option[Relation] = None,
                                 isMarker : Boolean = false) : Thing =
    (things returning things) += Thing(0, isMarker,
      content.string, content.int,
      content.double, content.boolean,
      relation.map(_.thing.id), relation.flatMap(_.metaThing).map(_.id))

  protected[rdbms] lazy val dictionaryKeyMarker : Thing = (transaction { context =>
    implicit val session = context.session
    StoredSingleton[SlickContext, Thing](
      Singleton[Thing](things.filter(_.isMarker).filter(_ === dictionaryKeyMarkerString)
        .firstOption getOrElse makeThing(dictionaryKeyMarkerString, isMarker = true))
    )
  })

  protected[rdbms]lazy val groupChildMarker : Thing = transaction { context =>
    implicit val session = context.session
    things.filter(_.isMarker).filter(_ === groupChildMarkerString)
      .firstOption getOrElse makeThing(groupChildMarkerString, isMarker = true)
  }

  protected[rdbms]lazy val groupChildDictionaryValueMarker : Thing = transaction { context =>
    implicit val session = context.session
    things.filter(_.isMarker).filter(_ === groupChildDictionaryValueMarkerString)
      .firstOption getOrElse makeThing(groupChildDictionaryValueMarkerString, isMarker = true)
  }
}

/**
 * Representation for a thing in the database.
 */
case class Thing(id: Int, isMarker: Boolean, override val string: Option[String], override val int: Option[Int],
                 override val double: Option[Double], override val boolean: Option[Boolean],
                 relationId: Option[Int], metaRelationId: Option[Int]) extends ThingOperations

class Things(tag: Tag) extends Table[Thing](tag, "things") {

  def id = column[Int]("thing_id", O.PrimaryKey, O.AutoInc)
  def isMarker = column[Boolean]("is_marker")

  // data stored within the "thing" of various types
  def string = column[Option[String]]("string", O.Nullable)
  def int = column[Option[Int]]("int", O.Nullable)
  def double = column[Option[Double]]("double", O.Nullable)
  def boolean = column[Option[Boolean]]("boolean", O.Nullable)

  // Relation with other things as well as a "meta" relation
  def relationId = column[Option[Int]]("relation_id")
  def metaRelationId = column[Option[Int]]("meta_relation_id")

  def * = (id, isMarker, string, int, double, boolean, relationId, metaRelationId) <> (Thing.tupled, Thing.unapply)

  def relatedThing = foreignKey("THINGS_RELATION_FK", relationId, things)(_.id)
  def metaRelatedThing = foreignKey("THINGS_META_RELATION_FK", metaRelationId, things)(_.id)

  /**
   * represents root things (but not markers)
   */
  def rooted : Column[Boolean] = !isMarker && relationId.isNull && metaRelationId.isNull

  /**
   * In-Query comparison of content
   */
  def === (content: GeneraContent): Column[Option[Boolean]] =
    ((string.isNull && !content.string.isDefined) || (string.isNotNull && (string === content.string))) &&
      ((int.isNull && !content.int.isDefined) || (int.isNotNull && (int === content.int))) &&
      ((double.isNull && !content.double.isDefined) || (double.isNotNull && (double === content.double))) &&
      ((boolean.isNull && !content.boolean.isDefined) || (boolean.isNotNull && (boolean === content.boolean)))
}

object Things {
  protected[rdbms] val things = TableQuery[Things]
}