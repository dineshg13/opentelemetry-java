/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logs.export;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogData;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An exporter is responsible for taking a collection of {@link LogData}s and transmitting them to
 * their ultimate destination.
 */
public interface LogRecordExporter extends Closeable {

  /**
   * Returns a {@link LogRecordExporter} which delegates all exports to the {@code exporters} in
   * order.
   *
   * <p>Can be used to export to multiple backends using the same {@link LogProcessor} like a {@link
   * SimpleLogProcessor} or a {@link BatchLogProcessor}.
   */
  static LogRecordExporter composite(LogRecordExporter... exporters) {
    return composite(Arrays.asList(exporters));
  }

  /**
   * Returns a {@link LogRecordExporter} which delegates all exports to the {@code exporters} in
   * order.
   *
   * <p>Can be used to export to multiple backends using the same {@link LogProcessor} like a {@link
   * SimpleLogProcessor} or a {@link BatchLogProcessor}.
   */
  static LogRecordExporter composite(Iterable<LogRecordExporter> exporters) {
    List<LogRecordExporter> exportersList = new ArrayList<>();
    for (LogRecordExporter exporter : exporters) {
      exportersList.add(exporter);
    }
    if (exportersList.isEmpty()) {
      return NoopLogRecordExporter.getInstance();
    }
    if (exportersList.size() == 1) {
      return exportersList.get(0);
    }
    return MultiLogRecordExporter.create(exportersList);
  }

  /**
   * Exports the collections of given {@link LogData}.
   *
   * @param logs the collection of {@link LogData} to be exported
   * @return the result of the export, which is often an asynchronous operation
   */
  CompletableResultCode export(Collection<LogData> logs);

  /**
   * Exports the collection of {@link LogData} that have not yet been exported.
   *
   * @return the result of the flush, which is often an asynchronous operation
   */
  CompletableResultCode flush();

  /**
   * Shutdown the log exporter. Called when {@link SdkLoggerProvider#shutdown()} is called when this
   * exporter is registered to the provider via {@link BatchLogProcessor} or {@link
   * SimpleLogProcessor}.
   *
   * @return a {@link CompletableResultCode} which is completed when shutdown completes
   */
  CompletableResultCode shutdown();

  /** Closes this {@link LogRecordExporter}, releasing any resources. */
  @Override
  default void close() {
    shutdown().join(10, TimeUnit.SECONDS);
  }
}