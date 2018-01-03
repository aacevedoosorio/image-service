package com.acevedo.images.controllers

import com.acevedo.images.services.{ImageRequest, Resolution, Thumbnail}

import scala.util.{Failure, Success, Try}

trait DTOValidation {
  def parseImageShow(typeName: String, reference: String, requestUri: String): Try[ImageRequest] = for {
    validTypeName <- findTypeName(typeName)
  } yield ImageRequest(reference, validTypeName, requestUri)

  private def findTypeName: String => Try[Resolution] = {
    case typeName@"thumbnail" => Success(Thumbnail())
    case _                    => Failure(new Error("Invalid type name"))
  }
}
