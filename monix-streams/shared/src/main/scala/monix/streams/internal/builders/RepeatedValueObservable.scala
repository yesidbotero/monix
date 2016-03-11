/*
 * Copyright (c) 2014-2016 by its authors. Some rights reserved.
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

package monix.streams.internal.builders

import java.util.concurrent.TimeUnit
import monix.execution.cancelables.MultiAssignmentCancelable
import monix.execution.{Cancelable, Ack}
import monix.execution.Ack.{Cancel, Continue}
import monix.streams.Observable
import monix.streams.observers.Subscriber
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

private[streams] final
class RepeatedValueObservable[T](initialDelay: FiniteDuration, period: FiniteDuration, unit: T)
  extends Observable[T] {

  def unsafeSubscribeFn(subscriber: Subscriber[T]): Cancelable = {
    val task = MultiAssignmentCancelable()
    val r = runnable(subscriber, task)

    if (initialDelay.length <= 0)
      r.run()
    else {
      task := subscriber.scheduler
        .scheduleOnce(initialDelay.length, initialDelay.unit, r)
    }

    task
  }

  private[this] def runnable(subscriber: Subscriber[T], task: MultiAssignmentCancelable): Runnable =
    new Runnable { self =>
      private[this] implicit val s = subscriber.scheduler
      private[this] val periodMs = period.toMillis
      private[this] var startedAt = 0L

      def syncScheduleNext(): Unit = {
        val initialDelay = {
          val duration = s.currentTimeMillis() - startedAt
          val d = periodMs - duration
          if (d >= 0L) d else 0L
        }

        // No need to synchronize, since we have a happens-before
        // relationship between scheduleOnce invocations.
        task := s.scheduleOnce(initialDelay, TimeUnit.MILLISECONDS, self)
      }

      def asyncScheduleNext(r: Try[Ack]): Unit = r match {
        case Success(ack) =>
          if (ack == Continue) syncScheduleNext()
        case Failure(ex) =>
          s.reportFailure(ex)
      }

      def run(): Unit = {
        startedAt = s.currentTimeMillis()
        val ack = subscriber.onNext(unit)
        if (ack == Continue)
          syncScheduleNext()
        else if (ack != Cancel)
          ack.onComplete(asyncScheduleNext)
      }
    }
}