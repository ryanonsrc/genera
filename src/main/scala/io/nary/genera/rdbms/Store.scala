package io.nary.genera.rdbms

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable

import io.nary.genera._
import io.nary.genera.rdbms.Things._

case class Auth(dbPath: String, dbDriver: String, dbUser: String, dbPassword: String)

/**
 * Contains RDBMS implementations of the Genera store operations.
 * Defines all first and second order implementations for Genera.
 */
trait TransactionCapable {
  implicit def sessionToOption(session: Session) : Option[Session] = Some(session)

  implicit class WithTables(context: SlickContext) {
    def apply[S[_], T, V <: ResultValue[T, S], R <: Result[SlickContext, V]](f: SlickContext => R) : R = {
      if (MTable.getTables("things").list(context.session).isEmpty) {
        things.ddl.create(context.session)
      }
      f(context)
    }
  }

  def transaction[S[_], T, V <: ResultValue[T, S], R <: Result[SlickContext, V]](f: SlickContext => R)(implicit auth: Auth) : Result[SlickContext, V] =
    Database.forURL(auth.dbPath, driver = auth.dbDriver, user = auth.dbUser, password = auth.dbPassword) withTransaction { implicit session =>
      (new SlickContext())(f)
    }

  protected[rdbms] def joinOrCreateTransaction[S[_], T, V <: ResultValue[T, S], R <: Result[SlickContext, V]](f: SlickContext => R)(implicit context: Option[SlickContext], auth: Auth) : Result[SlickContext, V] =
    context match {
      case None => transaction(f)
      case Some(c) => c(f)
    }
}

object Store {

  implicit object RdbmsStore extends GeneraStore[SlickContext, Thing] with TransactionCapable {
    /**
     * Add root-level content to the store
     */
    def put(content: GeneraContent, context: Option[SlickContext] = None): Result[SlickContext, StoredSingleton[SlickContext, Thing]] =
      joinOrCreateTransaction { context =>
        implicit val session = context.session
        context withStoredSingleton Singleton(context.makeThing(content, None))
      }

    /**
     * Get a root-level thing from the store
     */
    def get(content: GeneraContent, context: Option[SlickContext] = None): Result[SlickContext, StoredValues[Thing, List]] =
      joinOrCreateTransaction { context =>
        implicit val session = context.session
        context withStoredValues things.filter(_.rooted).filter(_ === content).list
      }

    /**
     * Nukes all tables and recreates them.
     */
    protected[rdbms] def nukeAndCreate(): Result[SlickContext, ComputedValue[SlickContext, Unit]] = transaction { implicit context =>
      things.ddl.drop
      things.ddl.create
      context withUnit
    }
  }
}

trait ThingOperations extends GeneraThing[SlickContext, Thing] with TransactionCapable { self : Thing =>
  /**
   * Get all children of this thing that matches the content by performing a join across the meta relation id
   * so that we can check that the meta-relation content is the group child marker.
   **/
  def childrenWith (content: GeneraContent)(implicit context: Option[SlickContext] = None): Result[SlickContext, StoredValues[Thing, List]] = joinOrCreateTransaction { context =>
    implicit val session = context.session
    import context._
    context withStoredValues things.filter(_.relationId === self.id)
      .innerJoin(things).on(_.metaRelationId === _.id).filter { case (child, meta) =>
      meta.string === groupChildMarkerString && child === content
    }.map(_._1).list
  }

  /**
   * Get all children of this thing
   TODO: May want to remove this
  def all(implicit context: Option[SlickContext] = None): Seq[Thing] = joinOrCreateTransaction { context =>
    implicit val session = context.session
    things.filter(_.relationId === self.id)
      .innerJoin(things).on(_.metaRelationId === _.id).filter { case (child, meta) =>
      meta.string === groupChildMarkerString
    }.map(_._1).list
  }
   **/


  /**
   * Adds a child to this thing:
   *
   * child -> parent -> ...
   *       \    \____ meta -> ...
   *       \____ meta -> group-child-marker
   */
  def addChild (content: GeneraContent)(implicit context: Option[SlickContext] = None) : Result[SlickContext, StoredSingleton[SlickContext, Thing]] = joinOrCreateTransaction { context =>
    implicit val session = context.session
    import context._
    context withStoredSingleton Singleton[Thing](makeThing(
      content, relation = Some(Relation(self, Some(groupChildMarker)))
    ))
  }

  /**
   * Puts a key-value pair:
   *
   * key -> value -> parent -> ...
   *     \    \       \____ meta -> ...
   *     \    \____ meta -> marker-group-child-dict-value
   *     \____ meta -> marker-dict-key
   */
  def update(key: GeneraContent, value: GeneraContent)(implicit context: Option[SlickContext] = None) : Result[SlickContext, StoredSingleton[SlickContext, Thing]] =
    joinOrCreateTransaction { context =>
      implicit val session = context.session
      import context._
      self(key).value.item match {
        case Some(valueThing) => valueThing <= value
        case None => context withStoredSingleton Singleton[Thing](makeThing(key,
          Some(Relation(makeThing(value, Some(Relation(self, Some(groupChildDictionaryValueMarker)))),
            Some(dictionaryKeyMarker))))
        )
      }
    }

  /**
   * Get the value for a provided key within this thing
   */
  def apply(key: GeneraContent)(implicit context: Option[SlickContext] = None) : Result[SlickContext, StoredValues[Thing, Option]] =
    joinOrCreateTransaction { context =>
      implicit val session = context.session
      import context._
      context withStoredValues things.filter(_.metaRelationId === dictionaryKeyMarker.id).innerJoin(things).on(_.relationId === _.id)
        .filter { case (keyThing, valueThing) =>
          keyThing === key &&
          valueThing.relationId === self.id &&
          valueThing.metaRelationId === groupChildDictionaryValueMarker.id
      }.map(_._2).firstOption
    }

  /**
   * Performs a content-comparison
   */
  def :== (content: GeneraContent)(implicit context: Option[SlickContext] = None): Result[SlickContext, ComputedValue[SlickContext, Boolean]] =
    joinOrCreateTransaction { context =>
      implicit val session = context.session
      context withComputed Singleton(self.string == content.string && self.int == content.int &&
        self.double == content.double && self.boolean == content.boolean)
    }

  /**
   * When performing a delete: all children groups and KV pairs are deleted.  Note: markers are not deleted.
   */
  def delete()(implicit context: Option[SlickContext] = None) : Unit = joinOrCreateTransaction { context =>
    implicit val session = context.session

    def deleteRelatives(origin: Thing) : Unit = things.filter(other =>
      (other.relationId === origin.id) || (other.metaRelationId === origin.id)).list foreach { relative =>
      deleteRelatives(relative)
      things.filter(thing => thing.id === relative.id && thing.isMarker === false).delete
    }

    deleteRelatives(self)
    things.filter(_.id === self.id).delete
  }

  /**
   * Replace content (this will not work on Markers).
   */
  def setContent (content: GeneraContent)(implicit context: Option[SlickContext] = None) : Unit =
    joinOrCreateTransaction { context =>
      implicit val session = context.session
      import context._

      // If this is a key, then we need to treat this like a KV rehash
      if(self.metaRelationId == Some(dictionaryKeyMarker.id))
        self(self.getContent) = content
      else
        (for{ t <- things if t.id === self.id && !t.isMarker }
        yield (t.string, t.int, t.double, t.boolean)) update (content.string,
          content.int, content.double, content.boolean)
    }

  protected[rdbms] def getContent = GeneraContent(self.string, self.int, self.double, self.boolean)
}