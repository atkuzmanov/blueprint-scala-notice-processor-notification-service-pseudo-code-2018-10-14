
import example.package.ExampleDatacentreStatus._

import scala.util.{Failure, Success, Try}
import com.ning.http.client.Response
import java.util.Date

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

case class ExampleDatacentreResponse(statusCode: Try[Int], lastModTimestamp: Try[Long]) {

  def exampleIsRecentEnough(lastChanged: Option[String]): Boolean = {
    val lastModTime: Option[Try[Long]] = lastChanged map ExampleDatacentreResponse.exampleLastChangedToMillis
    lastModTime match {
      case Some(changed) => (changed.isSuccess && lastModTimestamp.isSuccess) && (lastModTimestamp.get >= changed.get)
      case None => lastModTimestamp.isSuccess
    }
  }

  def exampleIsNotFoundResp: Boolean = {
    statusCode.getOrElse(0) == 404
  }

  def exampleIs5xxResp: Boolean = {
    statusCode.getOrElse(0) >= 500
  }

  def exampleIsConnectionFailureResp: Boolean = {
    statusCode.isFailure
  }

  def exampleIsOkResp: Boolean = {
    statusCode.getOrElse(0) == 200
  }

  def exampleGetStatus(lastChanged: Option[String] = None): ExampleDatacentreStatus.Value = {
    if (exampleIsConnectionFailureResp || exampleIs5xxResp) return NotAvailableStatus
    if (!exampleIsOkResp) return UnexpectedStatus
    if (exampleIsNotFoundResp) return NotFoundStatus
    if (!exampleIsRecentEnough(lastChanged)) return TimeConflictStatus
    AvailableStatus
  }

  private def exampleConvertToHumanDateTime(milliseconds: Long): String = {
    new Date(milliseconds).toString
  }

  override def toString: String = {
    val modified: String = if (lastModTimestamp.isSuccess) exampleConvertToHumanDateTime(lastModTimestamp.get) else "Getting modified failed."
    val status: String = if (statusCode.isSuccess) s"${statusCode.get}" else "Getting status failed."
    s"ExampleDatacenterResponse[lastModified: $modified, statusCode: $status]"
  }
}

object ExampleDatacentreResponse {
  private val lastChangededDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss+00:00")
  private val lastModTimeDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz")

  def exampleLastChangedToMillis(value: String): Try[Long] = {
    Try(lastChangededDateFormat.parseDateTime(value).getMillis)
  }

  def exampleLastModTimeToMillis(value: String): Try[Long] = {
    Try(lastModTimeDateFormat.parseDateTime(value).getMillis)
  }

  def exampleGetFromResponse(resp: Response): ExampleDatacentreResponse = {
    ExampleDatacentreResponse(Success(resp.getStatusCode), exampleLastModTimeToMillis(resp.getHeader("Last-Modified")))
  }

  def exampleGetFromFailedResponse(e: Throwable): ExampleDatacentreResponse = {
    ExampleDatacentreResponse(Failure(e), Failure(e))
  }

  def exampleIsAvailable(dc1: ExampleDatacentreResponse, dc2: ExampleDatacentreResponse, lastChnaged: Option[String]): Boolean = {
    val datacentre1Status: _root_.example.package.ExampleDatacentreStatus.Value = dc1.exampleGetStatus(lastChnaged)
    val datacentre2Status: _root_.example.package.ExampleDatacentreStatus.Value = dc2.exampleGetStatus(lastChnaged)
    //
    (datacentre1Status == AvailableStatus && datacentre2Status == AvailableStatus) ||
      (datacentre1Status == AvailableStatus && datacentre2Status == NotAvailableStatus) ||
      (datacentre1Status == NotAvailableStatus && datacentre2Status == AvailableStatus)
  }

  def exampleIsItATimeConflict(datacentre1: ExampleDatacentreResponse, datacentre2: ExampleDatacentreResponse, lastChanged: Option[String]): Boolean = {
    datacentre1.exampleGetStatus(lastChanged) == TimeConflictStatus || datacentre2.exampleGetStatus(lastChanged) == TimeConflictStatus
  }

  def exampleIsItANotFound(datacentre1: ExampleDatacentreResponse, datacentre2: ExampleDatacentreResponse): Boolean = {
    datacentre1.exampleGetStatus() == NotFoundStatus || datacentre2.exampleGetStatus() == NotFoundStatus
  }

  def exampleIsItAnUnexpectedStatus(datacentre1: ExampleDatacentreResponse, datacentre2: ExampleDatacentreResponse): Boolean = {
    datacentre1.exampleGetStatus() == UnexpectedStatus || datacentre2.exampleGetStatus() == UnexpectedStatus
  }

  // != as XOR operator
  def exampleIsOneDatacentreDown(datacentre1: ExampleDatacentreResponse, datacentre2: ExampleDatacentreResponse): Boolean = {
    (datacentre1.exampleGetStatus() == NotAvailableStatus) != (datacentre2.exampleGetStatus() == NotAvailableStatus)
  }
}


