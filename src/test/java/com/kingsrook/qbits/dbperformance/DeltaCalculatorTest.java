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

package com.kingsrook.qbits.dbperformance;


import java.math.BigDecimal;
import java.time.Instant;
import com.kingsrook.qbits.dbperformance.collect.DeltaCalculator;
import com.kingsrook.qbits.dbperformance.provider.StatementStats;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Counter baseline, reset and monotonic interval arithmetic.
 *******************************************************************************/
class DeltaCalculatorTest
{
   private static final Instant CAPTURE = Instant.parse("2026-10-01T12:00:00Z");


   /*******************************************************************************
    ** Old unknown entries baseline; new entries inside the interval count.
    *******************************************************************************/
   @Test
   void baselines()
   {
      assertThat(DeltaCalculator.calculate(null, entry(100, null), CAPTURE, null).baseline()).isTrue();
      assertThat(DeltaCalculator.calculate(null, entry(100, CAPTURE.minusSeconds(1)), CAPTURE, null).calls()).isZero();
      assertThat(DeltaCalculator.calculate(null, entry(100, CAPTURE.plusSeconds(1)), CAPTURE, null).calls()).isEqualTo(100);
      assertThat(DeltaCalculator.calculate(null, entry(100, CAPTURE.plusSeconds(1)), null, null).calls()).isZero();
   }


   /*******************************************************************************
    ** Decreased calls, changed entry epoch and global resets all count from zero.
    *******************************************************************************/
   @Test
   void resetsAndDeltas()
   {
      StatementStats before = entry(10, CAPTURE.minusSeconds(60));
      DeltaCalculator.Delta delta = DeltaCalculator.calculate(before, entry(15, before.statsSince()), CAPTURE, null);
      assertThat(delta.calls()).isEqualTo(5);
      assertThat(delta.totalMs()).isEqualByComparingTo("50");
      assertThat(delta.rows()).isEqualTo(5);
      assertThat(delta.reset()).isFalse();
      assertThat(DeltaCalculator.calculate(before, entry(3, before.statsSince()), CAPTURE, null).reset()).isTrue();
      assertThat(DeltaCalculator.calculate(before, entry(15, CAPTURE), CAPTURE, null).calls()).isEqualTo(15);
      assertThat(DeltaCalculator.calculate(before, entry(15, before.statsSince()), CAPTURE, CAPTURE.plusSeconds(1)).reset()).isTrue();
      assertThat(DeltaCalculator.counter(null, 100, false)).isZero();
      assertThat(DeltaCalculator.counter(10L, 3, false)).isEqualTo(3);
      assertThat(DeltaCalculator.counter(10L, 30, true)).isEqualTo(30);
      assertThat(DeltaCalculator.counter(10L, 30, false)).isEqualTo(20);
   }


   /*******************************************************************************
    ** A coherent counter sample.
    *******************************************************************************/
   private static StatementStats entry(long calls, Instant since)
   {
      return (new StatementStats(1, "app", true, "select $1", calls, BigDecimal.valueOf(calls * 10), BigDecimal.TEN, BigDecimal.TEN, calls, calls, calls, calls, since, null));
   }
}
