


object MessageProcessorExample {

  processSQSMessage(): Future[List[Unit]] = {
    val messages = receiveMessagesFromSQS()
    //Future.sequence() transforms from List[Future[Unit]] to Future[List[Unit]]
    Future.sequence(messages map { message =>
      processOneSQSMessage(message)
    }
    )
  }

  receiveMessagesFromSQS() = {
    val processingCapacity: Int = currentCapacity()
    if(processingCapacity > 0) {
      //pick up messages from AWS SQS
      //do processing...
      List(awsSQSMessages.toList)
    } else {
      log.warn(s"Some warning.")
      List.empty
    }
  }


}