/**
 * Copyright(C) 2015 Ryan Delucchi

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */


package io.nary.genera

/**
 * Genera is intended as a simple yet fairly general-purpose persistence layer.  It operates on three implicit
 * layers of abstraction:
 *
 * first-order (DB Schema):
 *    Definition of a "thing": A record in the "things" table
 *    Columns: content, relation to another "thing", meta-relation to another "thing"
 *
 * second-order (Data Organization):
 *    Definition of a "thing":  Can contain content, and optionally be a node within a group hierarchy or KV Pair
 *    child -> parent relations are supported (if the meta-relation is the group-child marker)
 *    key -> value relations are supported (if the meta-relation is the dictionary key marker)
 *
 * third-order (Application Logic):
 *   Utilization of Data organization functionality per the application's implementation.
 */
trait GeneraStore[C <: GeneraContext[C], T <: GeneraThing[C, T]] {
  /**
   * Put content in the store and get a "thing" back.
   */
  protected[genera] def put(content: GeneraContent, context: Option[C]) : Result[C, T, Singleton, StoredSingleton[C, T]]

  /**
   * Get a sequence of "things" such that all of the content matches
   */
  protected[genera] def get(content: GeneraContent, context: Option[C]) : Result[C, T, List, StoredValues[T, List]]
}

/**
 * Represents all session information for a particular store type
 */
trait GeneraContext[+C <: GeneraContext[C]] { self : C =>
  def withStoredValues[T <: GeneraThing[C, T], S[_]](item: S[T]) = Result(self, StoredValues[T, S](item))
  def withStoredSingleton[T <: GeneraThing[C, T]](item: Singleton[T]) = Result(self, StoredSingleton[C, T](item))
  def withComputed[T](item: Singleton[T]) = Result(self, ComputedValue(item))
  def withUnit = Result[C, Unit, Singleton, ComputedValue[C, Unit]](self, ComputedValue(Singleton(())))
}

case class Singleton[+T](singleton: T)

trait ResultValue[+T, +S[+_]] {
  val item : S[T]
}

case class ComputedValue[+C <: GeneraContext[_], +T](override val item: Singleton[T]) extends ResultValue[T, Singleton]
case class StoredSingleton[+C <: GeneraContext[_], +T <: GeneraThing[_, _]](item : Singleton[T]) extends ResultValue[T, Singleton]
case class StoredValues[+T <: GeneraThing[_, _], +S[+_]](override val item: S[T]) extends ResultValue[T, S]

case class Result[+C <: GeneraContext[C], +T, +S[+_], +V <: ResultValue[T, S]](context: C, value: V)

/**
 * Provides access to GeneraStore operations given an implicit context and store.
 */
object GeneraStore {
  def put[C <: GeneraContext, T <: GeneraThing[C, T]](content: GeneraContent)(implicit store: GeneraStore[C, T]) : Result[C, T, Singleton[T], StoredSingleton[C, T]] = {
    store.put(content, None)
  }

  def get[C <: GeneraContext, T <: GeneraThing[C, T]](content: GeneraContent)(implicit store: GeneraStore[C, T]) : Result[C, T, List, StoredValues[T, List]] = {
    store.get(content, None)
  }

  def putJoin[C <: GeneraContext, T <: GeneraThing[C, T]](content: GeneraContent)(implicit context: C, store: GeneraStore[C, T]) = store.put(content, Some(context))
  def getJoin[C <: GeneraContext, T <: GeneraThing[C, T]](content: GeneraContent)(implicit context: C, store: GeneraStore[C, T]) = store get(content, Some(context))
}
