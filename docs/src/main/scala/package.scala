package object docs {
  import java.io.File
  import cats.effect.unsafe.IORuntime
  import doodle.image.Image
  import doodle.core.format._
  import doodle.java2d._

  implicit class ImageSaveSyntax(image: Image) {
    import doodle.image.syntax.all._
    def save(filename: String)(implicit r: IORuntime): Unit = {
      val dir = new File("docs/src/pages/")
      val file = new File(dir, filename)
      image.write[Png](file)
    }
  }

  implicit class PictureSaveSyntax(picture: Picture[Unit]) {
    import doodle.syntax.all._
    def save(filename: String)(implicit r: IORuntime): Unit = {
      val dir = new File("docs/src/pages/")
      val file = new File(dir, filename)
      picture.write[Png](file)
    }
  }
}
