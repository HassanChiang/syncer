package com.github.zzt93.syncer.common.thread;

import com.github.zzt93.syncer.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author zzt
 */
public class WaitingAckHook extends Thread {

  private final Logger logger = LoggerFactory.getLogger(WaitingAckHook.class);
  /**
   * effectively final, fixed before shutdown hook thread start
   */
  private final List<Starter> starters;

  public WaitingAckHook(List<Starter> starters) {
    this.starters = starters;
  }

  @Override
  public void run() {
    for (Starter starter : starters) {
      try {
        starter.close();
      } catch (Throwable e) {
        logger.error("Fail to close starter", e);
      }
    }
  }

}
