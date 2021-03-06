/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.dataflow.sdk.runners.inprocess;

import com.google.auto.value.AutoValue;
import com.google.cloud.dataflow.sdk.runners.inprocess.CommittedResult.OutputType;
import com.google.cloud.dataflow.sdk.runners.inprocess.InMemoryWatermarkManager.TimerUpdate;
import com.google.cloud.dataflow.sdk.runners.inprocess.InProcessPipelineRunner.UncommittedBundle;
import com.google.cloud.dataflow.sdk.transforms.AppliedPTransform;
import com.google.cloud.dataflow.sdk.transforms.windowing.BoundedWindow;
import com.google.cloud.dataflow.sdk.util.WindowedValue;
import com.google.cloud.dataflow.sdk.util.common.CounterSet;
import com.google.cloud.dataflow.sdk.util.state.CopyOnAccessInMemoryStateInternals;
import com.google.common.collect.ImmutableList;
import org.joda.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An immutable {@link InProcessTransformResult}.
 */
@AutoValue
public abstract class StepTransformResult implements InProcessTransformResult {
  @Override
  public abstract AppliedPTransform<?, ?, ?> getTransform();

  @Override
  public abstract Iterable<? extends UncommittedBundle<?>> getOutputBundles();

  @Override
  public abstract Iterable<? extends WindowedValue<?>> getUnprocessedElements();

  @Override
  @Nullable
  public abstract CounterSet getCounters();

  @Override
  public abstract Instant getWatermarkHold();

  @Nullable
  @Override
  public abstract CopyOnAccessInMemoryStateInternals<?> getState();

  @Override
  public abstract TimerUpdate getTimerUpdate();

  @Override
  public abstract Set<OutputType> getOutputTypes();

  public static Builder withHold(AppliedPTransform<?, ?, ?> transform, Instant watermarkHold) {
    return new Builder(transform, watermarkHold);
  }

  public static Builder withoutHold(AppliedPTransform<?, ?, ?> transform) {
    return new Builder(transform, BoundedWindow.TIMESTAMP_MAX_VALUE);
  }

  /**
   * A builder for creating instances of {@link StepTransformResult}.
   */
  public static class Builder {
    private final AppliedPTransform<?, ?, ?> transform;
    private final ImmutableList.Builder<UncommittedBundle<?>> bundlesBuilder;
    private final ImmutableList.Builder<WindowedValue<?>> unprocessedElementsBuilder;
    private CopyOnAccessInMemoryStateInternals<?> state;
    private TimerUpdate timerUpdate;
    private CounterSet counters;
    private final Set<OutputType> producedOutputs;
    private final Instant watermarkHold;

    private Builder(AppliedPTransform<?, ?, ?> transform, Instant watermarkHold) {
      this.transform = transform;
      this.watermarkHold = watermarkHold;
      this.bundlesBuilder = ImmutableList.builder();
      this.producedOutputs = EnumSet.noneOf(OutputType.class);
      this.unprocessedElementsBuilder = ImmutableList.builder();
      this.timerUpdate = TimerUpdate.builder(null).build();
    }

    public StepTransformResult build() {
      return new AutoValue_StepTransformResult(
          transform,
          bundlesBuilder.build(),
          unprocessedElementsBuilder.build(),
          counters,
          watermarkHold,
          state,
          timerUpdate,
          producedOutputs);
    }

    public Builder withCounters(CounterSet counters) {
      this.counters = counters;
      return this;
    }

    public Builder withState(CopyOnAccessInMemoryStateInternals<?> state) {
      this.state = state;
      return this;
    }

    public Builder withTimerUpdate(TimerUpdate timerUpdate) {
      this.timerUpdate = timerUpdate;
      return this;
    }

    public Builder addUnprocessedElements(Iterable<? extends WindowedValue<?>> unprocessed) {
      unprocessedElementsBuilder.addAll(unprocessed);
      return this;
    }

    public Builder addOutput(
        UncommittedBundle<?> outputBundle, UncommittedBundle<?>... outputBundles) {
      bundlesBuilder.add(outputBundle);
      bundlesBuilder.add(outputBundles);
      return this;
    }

    public Builder addOutput(Collection<UncommittedBundle<?>> outputBundles) {
      bundlesBuilder.addAll(outputBundles);
      return this;
    }

    public Builder withAdditionalOutput(OutputType producedAdditionalOutput) {
      producedOutputs.add(producedAdditionalOutput);
      return this;
    }
  }
}

