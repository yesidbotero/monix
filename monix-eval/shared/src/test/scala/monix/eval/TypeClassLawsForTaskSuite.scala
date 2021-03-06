/*
 * Copyright (c) 2014-2017 by The Monix Project Developers.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.eval

import cats.Applicative
import cats.effect.Effect
import cats.effect.laws.discipline.{AsyncTests, EffectTests}
import cats.laws.discipline.CoflatMapTests
import monix.eval.instances.{CatsAsyncInstances, CatsEffectInstances}
import monix.execution.schedulers.TestScheduler

object TypeClassLawsForTaskSuite extends BaseLawsSuite {
  test("picked the deterministic Async instance") {
    val inst = implicitly[Applicative[Task]]
    assert(!inst.isInstanceOf[CatsAsyncInstances.ForParallelTask])
    assert(!inst.isInstanceOf[CatsEffectInstances.ForTask])
  }

  test("picked the deterministic Effect instance") {
    implicit val ec = TestScheduler()
    val inst = implicitly[Effect[Task]]
    assert(!inst.isInstanceOf[CatsEffectInstances.ForParallelTask])
  }

  checkAllAsync("CoflatMap[Task]") { implicit ec =>
    CoflatMapTests[Task].coflatMap[Int,Int,Int]
  }

  checkAllAsync("Async[Task]") { implicit ec =>
    AsyncTests[Task].async[Int,Int,Int]
  }

  checkAllAsync("Effect[Task]") { implicit ec =>
    EffectTests[Task].effect[Int,Int,Int]
  }
}
