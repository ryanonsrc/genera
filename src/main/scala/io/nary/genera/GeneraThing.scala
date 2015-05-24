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
 * All operations on a Genera "thing" that can be read, modified and deleted.
 */
trait GeneraThing[+C <: GeneraContext[C], T <: GeneraThing[C, T]] {
  /**
   * Retrieves all children from the thing that matches the provided content.
   */
  def childrenWith(content: GeneraContent)(implicit context: Option[C] = None): Result[C, StoredValues[T, List]]
  def /(content: GeneraContent)(implicit context: Option[C] = None) = childrenWith(content)

  /**
   * Allows child access via a "wrapper" (which makes it possible to chain child access operations)
   * TODO: dsabled for now
  def wrappedChildrenWith(content: GeneraContent)(implicit context: Option[C] = None): WrappedChildren[C, T] =
    new WrappedChildren[C, T](childrenWith(content).value)
  def /+(content: GeneraContent)(implicit context: Option[C] = None) = wrappedChildrenWith(content)
   **/

  /**
   * Retrieve all children from the thing.
   TODO: May want to remove this
  def all(implicit context: Option[C] = None) : Seq[T]
   **/

  /**
   * Add a new child to this thing containing the provided content
   */
  def addChild(content: GeneraContent)(implicit context: Option[C] = None) : Result[C, StoredSingleton[C, T]]
  def +=(content: GeneraContent)(implicit context: Option[C] = None) = addChild(content)

  /**
   * Put a new key-value pair as a child of this thing
   */
  def update(key: GeneraContent, value: GeneraContent)(implicit context: Option[C] = None) : Result[C, StoredSingleton[C, T]]

  /**
   * Retrieve the value via the provided key within this group.
   */
  def apply(key: GeneraContent)(implicit context: Option[C] = None) : Result[C, StoredValues[T, Option]]

  /**
   * perform a comparison of the content within this thing and the provided content
   */
  def :== (content: GeneraContent)(implicit context: Option[C] = None): Result[C, ComputedValue[C, Boolean]]

  /**
   * Delete this thing and all child grops and key-value pairs
   */
  def delete()(implicit context: Option[C] = None) : Result[C, ComputedValue[C, Unit]]

  /**
   * Replace the content in this thing with that which is provided
   */
  def setContent(content: GeneraContent)(implicit context: Option[C] = None) : Result[C, StoredSingleton[C, T]]
  def <=(content: GeneraContent)(implicit context: Option[C] = None) = setContent(content)

  /**
   * GeneraContent values
   */
  val string : Option[String]
  val int : Option[Int]
  val double : Option[Double]
  val boolean : Option[Boolean]
}

/**
 * A wrapper for children of things that allows child-access operations to be chained
 * and then resolved at the end with either a toSeq() or headOption()
 *
 * TODO: disabled for now

class WrappedChildren[C <: GeneraContext, T <: GeneraThing[C, T]](children: StoredValues[T, List]) {

  def wrappedChildrenWith(content: GeneraContent)(implicit context: Option[C] = None): WrappedChildren[C, T] =
    new WrappedChildren[C, T](children.item.map(_ wrappedChildrenWith content).flatten)

  def /+(content: GeneraContent)(implicit context: Option[C] = None) = wrappedChildrenWith(content)

  def toSeq : StoredValues[T, List] = children
  def headOption : StoredValues[T, Option] = StoredValues[T, Option](children.item.headOption)
}
 **/

/**
 * Holds all values for a thing.  Note that a think can contain zero or more values.
 */
case class GeneraContent(string: Option[String] = None, int: Option[Int] = None,
                   double: Option[Double] = None, boolean: Option[Boolean] = None)

/**
 * implicit conversions from a single value to an entity values collection.
 */
object GeneraContentConversions {
  implicit def stringToThingContent(str: String) : GeneraContent = GeneraContent(string = Some(str))
  implicit def intToThingContent(int: Int) : GeneraContent = GeneraContent(int = Some(int))
  implicit def doubleToThingContent(double: Double) : GeneraContent = GeneraContent(double = Some(double))
  implicit def booleanToThingContent(boolean: Boolean) : GeneraContent = GeneraContent(boolean = Some(boolean))
  implicit def noneToEmptyThingContent(none: None.type) : GeneraContent = GeneraContent()
}

/**
 * A DSL for more cleanly building Genera content.
 */
object GeneraContentBuilderKit {
  implicit class GeneraContentInt(val int: Int) extends AnyVal {
    def ~(string: String) = GeneraContent(int = Some(int), string = Some(string))
    def ~(double: Double) = GeneraContent(int = Some(int), double = Some(double))
    def ~(boolean: Boolean) = GeneraContent(int = Some(int), boolean = Some(boolean))
  }

  implicit class GeneraContentString(val string: String) extends AnyVal {
    def ~(int: Int) = GeneraContent(string = Some(string), int = Some(int))
    def ~(double: Double) = GeneraContent(string = Some(string), double = Some(double))
    def ~(boolean: Boolean) = GeneraContent(string = Some(string), boolean = Some(boolean))
  }

  implicit class GeneraContentDouble(val double: Double) extends AnyVal {
    def ~(string: String) = GeneraContent(double = Some(double), string = Some(string))
    def ~(int: Int) = GeneraContent(double = Some(double), int = Some(int))
    def ~(boolean: Boolean) = GeneraContent(double = Some(double), boolean = Some(boolean))
  }

  implicit class GeneraContentBoolean(val boolean: Boolean) extends AnyVal {
    def ~(string: String) = GeneraContent(boolean = Some(boolean), string = Some(string))
    def ~(int: Int) = GeneraContent(boolean = Some(boolean), int = Some(int))
    def ~(double: Double) = GeneraContent(boolean = Some(boolean), double = Some(double))
  }

  implicit class GeneraContentAugmentation(content: GeneraContent) {
    def ~(string: String) = content.copy(string = Some(string))
    def ~(int: Int) = content.copy(int = Some(int))
    def ~(double: Double) = content.copy(double = Some(double))
    def ~(boolean: Boolean) = content.copy(boolean = Some(boolean))
  }
}