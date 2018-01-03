package com.acevedo.images.repositories

import java.io._
import java.nio.channels.FileChannel
import java.nio.file.{Path, Paths}

import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.stereotype.Repository

import scala.language.{higherKinds, implicitConversions, postfixOps}
import scala.util.Try
case class ImageFile(data: InputStream)

trait ImageRepository {
  def get(id: String): Try[ImageFile]
  def create(id: String, file: File): Try[Unit]
  def create(id: String, data: InputStream): Try[Unit]
  def delete(id: String): Try[Unit]
}

@Repository
class LocalImageRepository extends ImageRepository {
  implicit def toPath (filename: String): Path = Paths.get(filename)
  //val storage = new File(ImageConfig.bucketName)
  private val storage = new File("/tmp")

  override def get(id: String): Try[ImageFile] = Try {
    val imgFile = newFile(id)
    ImageFile(new BufferedInputStream(new FileInputStream(imgFile)))
  }

  override def create(id: String, file: File): Try[Unit] = for {
    source      <- openInput(file)
    destination <- openOutput(newFile(id))
  } yield destination.transferFrom(source, 0 , source.size)

  private def newFile(id: String) = {
    val f = new File(storage, id)
    f.getParentFile.mkdirs()
    f
  }

  override def create(id: String, data: InputStream): Try[Unit] = Try {
     IOUtils.copy(data, new FileOutputStream(newFile(id)))
  }

  private def openInput: File => Try[FileChannel] = { file => Try(new FileInputStream(file).getChannel) }
  private def openOutput: File => Try[FileChannel] = { file => Try(new FileOutputStream(file).getChannel) }

  override def delete(id: String): Try[Unit] = Try {
    newFile(id).delete()
  }
}
