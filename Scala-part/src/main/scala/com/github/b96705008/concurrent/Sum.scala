package com.github.b96705008.concurrent

import akka.actor.{Actor, ActorSystem, Props}
import akka.routing.RoundRobinPool

sealed trait SumTrait
case class Result(value: Int) extends SumTrait

class SumActor extends Actor {
  val RANGE = 10000

  def calculate(start: Int, end: Int, flag: String): Int = {
    var cal = 0

    for (i <- start to end) {
      for (j <- 1 to 3000000) {}
      cal += i
    }

    println(s"flag: $flag.")
    cal
  }

  def receive = {
    case value: Int =>
      val start = (RANGE / 4) * (value - 1) + 1
      val end = (RANGE / 4) * value
      sender ! Result(calculate(start, end, value.toString))
    case _ => println("Unknown in SumActor...")
  }
}

class PrintActor extends Actor {
  def receive = {
    case (sum: Int, startTime: Long) =>
      val duration = (System.nanoTime() - startTime) / 1000000000.0
      println("Sum is " + sum + "; It takes " + duration + " seconds.")
    case _ => println("Unknown in PrintActor...")
  }
}

class MasterActor extends Actor {
  var sum = 0
  var count = 0
  var startTime: Long = 0

  val sumActor = context.actorOf(Props[SumActor]
    .withRouter(RoundRobinPool(nrOfInstances = 4)), name = "sumActor")
  val printActor = context.actorOf(Props[PrintActor], name = "printActor")

  def receive = {
    case "calculate..." =>
      startTime = System.nanoTime()
      for (i <- 1 to 4) sumActor ! i
    case Result(value) =>
      sum += value
      count += 1
      if (count == 4) {
        printActor ! (sum, startTime)
        context.stop(self)
      }
    case _ => println("Unknown in MasterActor...")
  }
}

object Sum {
  def main(args: Array[String]): Unit = {
    var sum = 0

    val system = ActorSystem("MasterActorSystem")
    val masterActor = system.actorOf(Props[MasterActor], name = "masterActor")
    masterActor ! "calculate..."

    Thread.sleep(5000)
    system.terminate()
  }
}
