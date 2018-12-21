package com.github.zzt93.syncer.common.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * update(set field)/delete by query
 * @see ExtraQuery
 */
public class SyncByQuery implements com.github.zzt93.syncer.data.SyncByQuery {

  private static final Logger logger = LoggerFactory.getLogger(SyncByQuery.class);

  private final HashMap<String, Object> syncBy = new HashMap<>();

  public SyncByQuery filter(String syncWithCol, Object value) {
    if (syncWithCol == null || value == null) {
      logger.warn("filter with {}={}", syncWithCol, value);
      return this;
    }
    syncBy.put(syncWithCol, value);
    return this;
  }

  boolean isSyncWithoutId() {
    return !syncBy.isEmpty();
  }

  HashMap<String, Object> getSyncBy() {
    return syncBy;
  }

  @Override
  public String toString() {
    return "SyncByQuery{" +
        "syncBy=" + syncBy +
        '}';
  }
}
