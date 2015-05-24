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


package io.nary.genera.rdbms

import io.nary.genera.{GeneraStore, GeneraContent}
import io.nary.genera.GeneraContentBuilderKit._
import io.nary.genera.GeneraContentConversions._
import org.scalatest.{OptionValues, Matchers, FlatSpec}

/**
 * Test RDBMS implementation of Genera
 */
class TablesSpec extends FlatSpec with Matchers with OptionValues {

  implicit val auth = Auth("jdbc:postgresql://localhost:5432/postgres", "org.postgresql.Driver", "postgres", "rikeromega3")

  import GeneraStore._
  import Store.RdbmsStore._

  "RDBMS Tables" should "create a few groups, some content, edit content and delete() the parent group" in {
    nukeAndCreate()  // IMPORTANT: disable in PROD!

    val movies = put[SlickContext, Thing]("movies")
    val tvShows = put[SlickContext, Thing]("tvShows")

    val officeSpace = movies.value.item.singleton addChild GeneraContent(string = Some("officeSpace"), int = Some(1))
    movies.value.item.singleton addChild GeneraContent(string = Some("raidersOfTheLostArk"), boolean = Some(true))
    tvShows.value.item.singleton addChild GeneraContent(string = Some("houseOfCards"), double = Some(2.0))

    (movies.value.item.singleton childrenWith GeneraContent(Some("officeSpace"), int = Some(1))).value.item.head("officeSpaceKey") =
      GeneraContent(int = Some(3), boolean = Some(false))

    get("movies").value.item.head == movies should be(true)
    get("tvShows").value.item.head == tvShows should be(true)

    (movies.value.item.singleton childrenWith GeneraContent(Some("raidersOfTheLostArk"), boolean = Some(true))).value.item.head.boolean.value should equal (true)

    val officeSpaceDictValue = (movies.value.item.singleton childrenWith GeneraContent(Some("officeSpace"),
      int = Some(1))).value.item.head("officeSpaceKey").value

    officeSpaceDictValue.item.value.int.value should equal (3)

    officeSpaceDictValue.item.value.boolean.value should equal (false)

    officeSpace.value.item.singleton setContent GeneraContent(string = Some("officeSpace"), int = Some(1))

    val officeSpaceRetrieved = (movies.value.item.singleton childrenWith GeneraContent(Some("officeSpace"), int = Some(1))).value.item.head
    officeSpaceRetrieved.int.value should equal(1)
    officeSpaceRetrieved.double should equal(None)

    movies.value.item.singleton.delete()
    tvShows.value.item.singleton.delete()

    get("movies") should be ('empty)
    get("Bar") should be ('empty)
  }

  it should "create a few groups, some content, edit content and delete() the parent group (using content builder kit)" in {
    nukeAndCreate()  // IMPORTANT: disable in PROD!

    val movies = put[SlickContext, Thing]("movies")
    val tvShows = put[SlickContext, Thing]("tvShows")

    val officeSpace = movies.value.item.singleton += "officeSpace" ~ 1
    movies.value.item.singleton += "raidersOfTheLostArk" ~ true
    tvShows.value.item.singleton += "houseOfCards" ~ 2.0

    (movies.value.item.singleton / ("officeSpace" ~ 1)).value.item head "officeSpaceKey" = 3 ~ false

    get("movies").value.item.head == movies should be(true)
    get("tvShows").value.item.head == tvShows should be(true)

    (movies.value.item.singleton / ("raidersOfTheLostArk" ~ true)).value.item.head.boolean.value should equal (true)

    val officeSpaceDictValue = (movies.value.item.singleton / ("officeSpace" ~ 1)).value.item.head("officeSpaceKey").value

    (officeSpaceDictValue.item.value :== (false ~ 3)) should equal(true)

    officeSpace.value.item.singleton <= "officeSpace" ~ 1

    val officeSpaceRetrieved = (movies.value.item.singleton / ("officeSpace" ~ 1)).value.item.head

    (officeSpaceRetrieved :== ("officeSpace" ~ 1)) should equal(true)

    movies.value.item.singleton.delete()
    tvShows.value.item.singleton.delete()

    get("movies") should be ('empty)
    get("Bar") should be ('empty)
  }

  it should "create a thing, add some key-value pairs and change them" in {
    nukeAndCreate()

    val composers = put[SlickContext, Thing]("composers")

    val beethoven = composers.value.item.singleton += "beethoven"

    beethoven.value.item.singleton("number of symphonies") = 9

    beethoven.value.item.singleton("number of symphonies").value.item.exists(t => (t :== 9).value.item.singleton) should equal(true)

    beethoven.value.item.singleton("number of symphonies") = 10

    beethoven.value.item.singleton("number of symphonies").value.item.exists(t => (t :== 10).value.item.singleton) should equal(true)

    beethoven.value.item.singleton("number of symphonies").value.item.foreach(_ <= 9)

    beethoven.value.item.singleton("number of symphonies" ).value.item.exists( t => (t :== 9).value.item.singleton) should equal(true)

    composers.value.item.singleton.delete()

    get("composers") should be('empty)
  }
}
