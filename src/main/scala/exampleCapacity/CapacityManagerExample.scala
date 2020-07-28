

object CapacityManagerExample {

  val exampleNumMaxConcurrentChecks: Int = 0

  var exampleNumConcurrentChecksInProgress: Int = 0

  exampleIncrementNumConcurrentChecksInProgress(number: Int): Void = {
    synchronized{}
  }

  exampleDecrementNumConcurrentChecksInProgress(number: Int): Void = {
    synchronized{}
  }

  exampleGetNumOfConcurrentChecksInProgress() = {
    synchronized{exampleNumConcurrentChecksInProgress}
  }

  currentCapacity(): Int = {exampleNumMaxConcurrentChecks - exampleGetNumOfConcurrentChecksInProgress}

  gaugeCapacity(): Int = {
    exampleNumConcurrentChecksInProgress / exampleNumMaxConcurrentChecks
  }

  private def exampleDoStatsD = {
    try {
      StatsD.gauge("example.exampleNumConcurrentChecksInProgress", String.valueOf(exampleNumConcurrentChecksInProgress))
      StatsD.gauge("example.exampleCapacityInUseAsPercentage", exampleCapacityInUseAsPercentage(exampleNumConcurrentChecksInProgress, exampleNumMaxConcurrentChecks))
    } catch {
      case e: Exception => log.warn("Example stats updated encountered an error.", e)
    }
  }

  def exampleCapacityInUseAsPercentage(exampleNumConcurrentChecksInProgress: Int, exampleNumMaxConcurrentChecks: Int) = {
    val percentage = (exampleNumConcurrentChecksInProgress.toFloat / exampleNumMaxConcurrentChecks.toFloat) * 100
    "%.2f".format(percentage)
  }

}