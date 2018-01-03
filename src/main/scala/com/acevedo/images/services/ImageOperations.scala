package com.acevedo.images.services
import java.io.ByteArrayInputStream

import com.acevedo.images.Loggable
import com.acevedo.images.repositories.ImageFile
import com.sksamuel.scrimage._
import com.sksamuel.scrimage.nio.{GifWriter, ImageWriter, JpegWriter, PngWriter}

import scala.language.{higherKinds, implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}

protected[services] trait ImageOperations extends Loggable with ImageWriters {
  private lazy val config: Map[String, Thumbnail] = Map("thumbnail" -> Thumbnail())
  protected def resize(imageRequest: ImageRequest): ImageFile => Try[ImageFile] = { imageFile =>
    for {
      image          <- Try(Image.fromStream(imageFile.data))
      resizedImage   <- Try(image.cover(imageRequest.imageType.width, imageRequest.imageType.height))
      imageResponse  <- Try(ImageFile(new ByteArrayInputStream(resizedImage.bytes(getWriter(imageRequest.imageType.format)(imageRequest.imageType.quality)))))
    } yield imageResponse
  }

  private def findResolution(imageType: String): Try[Resolution] =
    config.get(imageType).map(Success(_)).getOrElse({
      logger.info(s"imageType properties not exist $imageType")
      Failure(new NoSuchElementException("Resolution not found"))
    })
}

trait ImageWriters {
  def getWriter(format: Format): Int => ImageWriter = { quality =>
    format match {
      case JPEG => JpegWriter(quality, progressive = false)
      case PNG  => PngWriter(quality)
      case GIF  => GifWriter(progressive = false)
    }
  }
}