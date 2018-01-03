package com.acevedo.images.controllers

import javax.servlet.http.HttpServletRequest

import com.acevedo.images.Loggable
import com.acevedo.images.services.ImageService
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.core.io.InputStreamResource
import org.springframework.http.{HttpHeaders, HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam}

import scala.language.{higherKinds, implicitConversions, postfixOps}
import scala.util.{Failure, Success}

@Controller
class ImageController @Autowired()(val imageService: ImageService) extends Loggable with DTOValidation {
  @Value("${app.source-root-url}") private var sourceRootUrl: String = _

  @RequestMapping(Array("/image/show/{typeName}/{seoName}/"))
  def showImage(@PathVariable typeName: String,
                @PathVariable seoName: String,
                @RequestParam("reference") reference: String,
                request: HttpServletRequest): ResponseEntity[InputStreamResource] = (for {
    imageRequest <- parseImageShow(typeName, reference, sourceRootUrl)
    image        <- imageService.getImage(imageRequest)
  } yield image) match {
      case Success(imageFile) =>
        val headers = new HttpHeaders()
        headers.setContentType(MediaType.IMAGE_JPEG)
        new ResponseEntity[InputStreamResource](new InputStreamResource(imageFile.data), headers, HttpStatus.OK)
      case Failure(e) =>
        logger.error(s"${e.getMessage}")
        logger.error(s"${e.getStackTraceString}")
        new ResponseEntity[InputStreamResource](HttpStatus.NOT_FOUND)
  }
}
