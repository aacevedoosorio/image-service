package com.acevedo.images.services

import java.net.URL

import com.acevedo.images.repositories.{ImageFile, ImageRepository}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import scala.language.{higherKinds, implicitConversions, postfixOps}
import scala.util.Try

sealed trait Format
case object PNG extends Format
case object GIF extends Format
case object JPEG extends Format

sealed trait Resolution {
  def name: String
  def height: Int
  def width: Int
  def format: Format
  def quality: Int
}
case class  Thumbnail(override val name: String = "thumbnail",
                      override val height: Int = 320,
                      override val width: Int = 240,
                      override val format: Format = JPEG,
                      override val quality: Int = 60) extends Resolution

case class ImageRequest(reference: String, imageType: Resolution, sourceDomain: String)

@Service
class ImageService @Autowired()(val imageRepository: ImageRepository) extends ImageOperations {
  private lazy val filename: ImageRequest => String = { _.reference }
  private lazy val optimizedFilename: ImageRequest => String = { imageRequest =>
    val sanitizedFilename = imageRequest.reference.replace('/', '_')
    sanitizedFilename.grouped(4).toList match {
      case firstSubdir :: tail if firstSubdir.length < 4 => sanitizedFilename
      case firstSubdir :: secondSubdir :: thirdSubdir :: tail => s"/$firstSubdir/$secondSubdir/$sanitizedFilename"
      case firstSubdir :: tail => s"/$firstSubdir/$sanitizedFilename"
    }
  }

  private lazy val originalFilename: ImageRequest => String = { imageRequest => s"/original/${optimizedFilename(imageRequest)}" }
  private lazy val resolutionFilename: ImageRequest => String = { imageRequest => s"${imageRequest.imageType.name}${optimizedFilename(imageRequest)}" }

  def getImage(imageRequest: ImageRequest): Try[ImageFile] = {
    imageRepository.get(resolutionFilename(imageRequest)) orElse processOriginalFoto(imageRequest)
  }

  private def processOriginalFoto(imageRequest: ImageRequest): Try[ImageFile] = (for {
    foto          <- imageRepository.get(originalFilename(imageRequest))
    _             <- optimizeFoto(imageRequest, foto)
    optimizedFoto <- getImage(imageRequest)
  } yield optimizedFoto ) recoverWith { case e => retryWithDownload(imageRequest) }

  private def retryWithDownload(imageRequest: ImageRequest) = for {
    downloadedFoto  <- downloadFoto(imageRequest)
    _               <- imageRepository.create(originalFilename(imageRequest), downloadedFoto.data)
    originalFoto    <- imageRepository.get(originalFilename(imageRequest))
    _               <- optimizeFoto(imageRequest, originalFoto)
    optimizedFoto   <- getImage(imageRequest)
  } yield optimizedFoto

  private def optimizeFoto(imageRequest: ImageRequest, foto: ImageFile) = for {
    optimizedFoto <- resize(imageRequest)(foto)
    _             <- imageRepository.create(resolutionFilename(imageRequest), optimizedFoto.data)
  } yield optimizedFoto

  private def downloadFoto(imageRequest: ImageRequest): Try[ImageFile] = Try {
    val downloadedImage = new URL(s"${imageRequest.sourceDomain}${imageRequest.reference}")
    ImageFile(downloadedImage.openStream)
  }
}
