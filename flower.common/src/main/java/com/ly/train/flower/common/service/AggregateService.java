package com.ly.train.flower.common.service;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ly.train.flower.common.service.containe.ServiceContext;
import com.ly.train.flower.common.service.message.FlowMessage;
import com.ly.train.flower.common.service.message.TimerMessage;

public class AggregateService implements Service, Aggregate {

  private static final long TimeOutMilliseconds = 60000;

  int sourceNumber = 0;

  // <messageId,Set<message>>
  Map<String, Set<Object>> resultMap = new ConcurrentHashMap<String, Set<Object>>();
  // <messageId,sourceNumber>
  Map<String, Integer> resultNumberMap = new ConcurrentHashMap<String, Integer>();
  Map<String, Date> resultDateMap = new ConcurrentHashMap<String, Date>();

  @Override
  public Object process(Object message, ServiceContext context) {

    FlowMessage flowMessage = (FlowMessage) message;

    if(flowMessage instanceof TimerMessage){
      doClean();
      return null;
    }

    // first joint message
    if (!resultMap.containsKey(flowMessage.getTransactionId())) {
      Set<Object> objectSet = new HashSet<Object>();
      resultMap.put(flowMessage.getTransactionId(), objectSet);
      resultNumberMap.put(flowMessage.getTransactionId(), sourceNumber);
      resultDateMap.put(flowMessage.getTransactionId(), new Date());
    }
    resultMap.get(flowMessage.getTransactionId()).add(((FlowMessage) message).getMessage());

    Integer integer = resultNumberMap.get(flowMessage.getTransactionId()) - 1;
    resultNumberMap.put(flowMessage.getTransactionId(), integer);
    if (integer <= 0) {
      Set<Object> returnObject = resultMap.get(flowMessage.getTransactionId());
      resultMap.remove(flowMessage.getTransactionId());
      resultNumberMap.remove(flowMessage.getTransactionId());
      resultDateMap.remove(flowMessage.getTransactionId());

      return buildMessage(returnObject);
    }
    // TODO resultNumberMap&resultMap memory leak
    return null;
  }

  /**
   * subclass should override the method.
   * @param messages: Set<Message>
   * @return
   */
  public Object buildMessage(Set<Object> messages) {
    return messages;
  }

  @Override
  // sourceNumber++ when initialize
  public void setSourceNumber(int number) {
    sourceNumber = number;
  }

  private void doClean(){
    Set<String> transactionIds = resultDateMap.keySet();
    Date now = new Date();
    for (String transactionId: transactionIds){
      if(now.getTime() - resultDateMap.get(transactionId).getTime() > TimeOutMilliseconds){
        resultDateMap.remove(transactionId);
        resultMap.remove(transactionId);
        resultNumberMap.remove(transactionId);
      }
    }
  }
}
