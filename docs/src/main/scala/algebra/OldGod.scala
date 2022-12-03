package docs
package algebra

import cats.effect.unsafe.implicits.global
import cats.implicits._
import doodle.core._
import doodle.java2d._
import doodle.syntax.all._ // For Color

object OldGod {
  val redCircle = circle[Algebra](100).strokeColor(Color.red)
  val twoRedCircles = redCircle.beside(redCircle)
  val oldGod = read[Algebra]("docs/src/main/scala/algebra/old-god.png")

  twoRedCircles.above(oldGod).save("algebra/suns-old-god.png")

}
