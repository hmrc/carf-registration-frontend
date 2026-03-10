package types

import cats.data.EitherT
import models.error.ApiError

import scala.concurrent.{ExecutionContext, Future}

type ResultT[T] = EitherT[Future, ApiError, T]

object ResultT {
  def fromFuture[T](value: Future[Either[ApiError, T]]): ResultT[T] = {
    EitherT(value)
  }
}