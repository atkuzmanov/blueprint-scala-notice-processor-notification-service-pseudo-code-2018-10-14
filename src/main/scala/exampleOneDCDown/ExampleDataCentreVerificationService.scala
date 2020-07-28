import example.package.StatsD
import example.package.ExampleDatacentreResponse
import example.package.ExampleDao

import org.slf4j.LoggerFactory

import com.ning.http.client.Response

import scala.util.{Failure, Success, Try}
import scala.concurrent.Future

trait ExampleDataCentreVerificationService {
  private val logger = LoggerFactory getLogger getClass

  import example.CustomExecutionContext.executionContext

  //  private val failedAllDCAttempts = CloudWatch.meter("example-datacentre-content-verification-failure")

  val exampleDAO: ExampleDao
  val attempts: Int = 2
  val retryDelay: Int = 500


  private def exampleRetryANumberOfAttempts(exampleAttemptsRemaining: Int = attempts)(exampleBlockOfCodeToExecute: => Future[Boolean]): Future[Boolean] = {
    //    StatsD.increment("example.attempt")
    def exampleRetry: Future[Boolean] =
      if (exampleAttemptsRemaining == 0) {
        //        StatsD.increment("example.failed_all_attempts")
        //        failedAllDCAttempts.mark()
        logger.error(s"Verification of content after all retries failed for one or both datacentres.")
        Future.successful(false)
      } else {
        Thread.sleep(retryDelay)
        exampleRetryANumberOfAttempts(exampleAttemptsRemaining - 1)(exampleBlockOfCodeToExecute)
      }
    Try {
      exampleBlockOfCodeToExecute
    } match {
      case Success(exampleResult: Future[Boolean]) =>
        exampleResult.flatMap {
          case true =>
            StatsD.increment("datacentre.success")
            Future.successful(true)
          case false =>
            exampleRetry
        }
      case Failure(error) =>
        exampleRetry
    }
  }

  private def exampleVerifyOneDatacentre(exampleDatacentreResult: Future[Response]): Future[ExampleDatacentreResponse] = {
    (for {
      exampleVerifyInDC <- exampleDatacentreResult
      exampleDcResponce = ExampleDatacentreResponse.exampleGetFromResponse(exampleVerifyInDC)
    } yield exampleDcResponce) recover {
      case e => ExampleDatacentreResponse.exampleGetFromFailedResponse(e)
    }
  }

  private def exampleVerifyAvailability(datacentre1: ExampleDatacentreResponse, datacentre2: ExampleDatacentreResponse, urlLocator: String, lastModTimestamp: Option[String]): Boolean = {
    val datacentre1Status: _root_.example.package.ExampleDatacentreStatus.Value = datacentre1.exampleGetStatus(lastModTimestamp)
    val datacentre2Status: _root_.example.package.ExampleDatacentreStatus.Value = datacentre2.exampleGetStatus(lastModTimestamp)
    val dcIsAvailable: Boolean = ExampleDatacentreResponse.exampleIsAvailable(datacentre1, datacentre2, lastModTimestamp)

    if (dcIsAvailable) {
      logger.info(s"Successfully verified $urlLocator.")
      val isOneOfTheDCsDown: Boolean = ExampleDatacentreResponse.exampleIsOneDatacentreDown(datacentre1, datacentre2)
      if (isOneOfTheDCsDown) {
        logger.warn(s"One datacenter down when verifying [$urlLocator]: DC1 status: [$datacentre1Status $datacentre1] | DC2 status: [$datacentre2Status $datacentre2] | lastModTimestamp: [$lastModTimestamp]")
        //        StatsD.increment("example.datacenter_down")
      }
    } else {
      if (ExampleDatacentreResponse.exampleIsItATimeConflict(datacentre1, datacentre2, lastModTimestamp))
        logger.info(s"example.timestamp_failure")
      //        StatsD.increment("example.timestamp_failure")

      if (ExampleDatacentreResponse.exampleIsItANotFound(datacentre1, datacentre2))
        logger.info(s"example.not_found_failure")
      //        StatsD.increment("example.not_found_failure")

      if (ExampleDatacentreResponse.exampleIsItAnUnexpectedStatus(datacentre1, datacentre2))
        logger.info(s"example.unknown_failure")
      //        StatsD.increment("example.unknown_failure")

      logger.warn(s"Verification of [$urlLocator] failed: DC1 status: [$datacentre1Status $datacentre1] | DC2 status: [$datacentre2Status $datacentre2] | lastModTimestamp: [$lastModTimestamp]")
    }
    dcIsAvailable
  }

  private def exampleCheckDCResults(urlLocator: String, lastModTimestamp: Option[String], datacentre1Result: Future[Response], datacentre2Result: Future[Response]): Future[Boolean] = {
    for {
      checkInDC1: ExampleDatacentreResponse <- exampleVerifyOneDatacentre(datacentre1Result)
      checkInDC2: ExampleDatacentreResponse <- exampleVerifyOneDatacentre(datacentre2Result)
    } yield exampleVerifyAvailability(checkInDC1, checkInDC2, urlLocator, lastModTimestamp)
  }

  private def exampleCheckItemExists(urlLocator: String, lastModTimestamp: Option[String])(exampleRetrieveItems: => (Future[Response], Future[Response])): Future[Boolean] =
    StatsD.timeAsync("example.try_duration") {
      logger.info(s"Verifying content for item [$urlLocator]")
      exampleRetryANumberOfAttempts() {
        val (checkInDC1Future, checkInDC2Future) = exampleRetrieveItems
        exampleCheckDCResults(urlLocator, lastModTimestamp, checkInDC1Future, checkInDC2Future)
      }
    }

  def exampleCheckItemByURL(itemUrl: String, lastModTimestamp: Option[String]): Future[Boolean] =
    exampleCheckItemExists(itemUrl, lastModTimestamp) {
      exampleDAO.retrieveAssetsByUrl(itemUrl)
    }
}

object ExampleDataCentreVerificationService extends ExampleDataCentreVerificationService {
  override val exampleDAO = ExampleDao
}


