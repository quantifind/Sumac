/**
 * Copyright 2012-2020 Quantifind, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.quantifind.sumac.types

import collection.mutable.LinkedHashSet

class SelectInput[T](var value: Option[T], val options: LinkedHashSet[T])

object SelectInput{
  def apply[T](options: T*) = new SelectInput[T](value = None, options = (LinkedHashSet.empty ++ options))
  def apply[T](value: Option[T], options: Traversable[T]) = new SelectInput[T](value = value, options = (LinkedHashSet.empty ++ options))
}
