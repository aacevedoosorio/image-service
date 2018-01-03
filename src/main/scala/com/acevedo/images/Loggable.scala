package com.acevedo.images

import org.slf4j.{Logger, LoggerFactory}

trait Loggable {
  def logger: Logger = LoggerFactory.getLogger(this.getClass)
}
