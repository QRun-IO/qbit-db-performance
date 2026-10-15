/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.dbperformance.collect;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import com.kingsrook.qbits.dbperformance.provider.StatementStats;


/*******************************************************************************
 ** Pure cumulative-counter arithmetic with explicit baselines and reset detection.
 *******************************************************************************/
public final class DeltaCalculator
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DeltaCalculator()
   {
   }


   /*******************************************************************************
    ** A first observation is not historical activity. Only an entry created
    ** within the previous capture interval can count from zero immediately.
    *******************************************************************************/
   public static Delta calculate(StatementStats previous, StatementStats current, Instant previousCapture, Instant globalReset)
   {
      boolean fresh = previous == null;
      boolean baseline = fresh && (previousCapture == null || current.statsSince() == null || !current.statsSince().isAfter(previousCapture));
      boolean reset = !fresh && (current.calls() < previous.calls()
         || current.statsSince() != null && !Objects.equals(current.statsSince(), previous.statsSince())
         || globalReset != null && previousCapture != null && globalReset.isAfter(previousCapture));
      if(baseline)
      {
         return (new Delta(0, BigDecimal.ZERO, 0, 0, 0, 0, false, true));
      }
      boolean zero = fresh || reset;
      return (new Delta(counter(zero ? 0L : previous.calls(), current.calls(), false),
         current.totalExecMs().subtract(zero ? BigDecimal.ZERO : previous.totalExecMs()).max(BigDecimal.ZERO),
         counter(zero ? 0L : previous.rows(), current.rows(), false),
         counter(zero ? 0L : previous.sharedBlksHit(), current.sharedBlksHit(), false),
         counter(zero ? 0L : previous.sharedBlksRead(), current.sharedBlksRead(), false),
         counter(zero ? 0L : previous.tempBlksWritten(), current.tempBlksWritten(), false), reset, false));
   }


   /*******************************************************************************
    ** Null means no baseline; a decrease means reset for this counter.
    *******************************************************************************/
   public static long counter(Long previous, long current, boolean reset)
   {
      return (previous == null ? 0 : reset || current < previous ? current : current - previous);
   }


   /*******************************************************************************
    ** Interval statement activity; maximum duration remains a since-reset gauge.
    *******************************************************************************/
   public record Delta(long calls, BigDecimal totalMs, long rows, long sharedBlksHit, long sharedBlksRead, long tempBlksWritten, boolean reset, boolean baseline)
   {
   }
}
