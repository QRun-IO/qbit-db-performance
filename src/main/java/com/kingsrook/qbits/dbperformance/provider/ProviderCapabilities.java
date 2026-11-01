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

package com.kingsrook.qbits.dbperformance.provider;


import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/*******************************************************************************
 ** What one provider can read on one connection; no persistence or side effects.
 ** {@code statsAccess} is a stable code persisted on settings (FULL, or the most
 ** important missing prerequisite); {@code message} is the provider's own
 ** next step; {@code features} lists the statistics areas available right now.
 *******************************************************************************/
public record ProviderCapabilities(boolean supported, int version, boolean statements, boolean readAllStats, String statsAccess, String message, Set<StatsFeature> features)
{
   /*******************************************************************************
    ** Defensive copy.
    *******************************************************************************/
   public ProviderCapabilities
   {
      features = features == null || features.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(features));
   }



   /*******************************************************************************
    ** Pre-feature form: a supported provider is assumed to offer every area.
    *******************************************************************************/
   public ProviderCapabilities(boolean supported, int version, boolean statements, boolean readAllStats, String statsAccess, String message)
   {
      this(supported, version, statements, readAllStats, statsAccess, message, supported ? EnumSet.allOf(StatsFeature.class) : Set.of());
   }



   /*******************************************************************************
    ** Whether an area is available on this connection.
    *******************************************************************************/
   public boolean has(StatsFeature feature)
   {
      return (features.contains(feature));
   }
}
