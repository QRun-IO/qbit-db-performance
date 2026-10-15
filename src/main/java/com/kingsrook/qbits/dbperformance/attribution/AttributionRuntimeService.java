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

package com.kingsrook.qbits.dbperformance.attribution;


import com.kingsrook.qbits.dbperformance.DbPerfRuntime;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** QApplicationLauncher lifecycle, after instance initialization and migrations.
 *******************************************************************************/
public class AttributionRuntimeService implements QRuntimeServiceInterface
{
   private DbPerfRuntime runtime;


   /*******************************************************************************
    ** Name used by the host launcher.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("dbPerformanceAttribution");
   }


   /*******************************************************************************
    ** Start on each application pod, independently of the job scheduler.
    *******************************************************************************/
   @Override
   public void start(QInstance instance)
   {
      runtime = DbPerfRuntime.of(instance);
      runtime.startAttribution();
   }


   /*******************************************************************************
    ** Stop listener delivery and persist the final completed units.
    *******************************************************************************/
   @Override
   public void stop()
   {
      if(runtime != null)
      {
         runtime.stopAttribution();
      }
   }
}
