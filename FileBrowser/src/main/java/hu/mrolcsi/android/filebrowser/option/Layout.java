/*
 * Copyright 2018 Roland Matusinka <http://github.com/mrolcsi>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.mrolcsi.android.filebrowser.option;

/**
 * Kezdeti elrendezés (futás közben váltogatható)
 * <ul>
 * <li>Lista {@link #LIST LIST}</li>
 * <li>Négyzetrácsos(grid) {@link #GRID GRID}</li>
 * </ul>
 * Alapértelmezett: lista.
 */
public enum Layout {
  /**
   * Lista nézet
   */
  LIST,
  /**
   * Négyzetrács (grid) nézet
   */
  GRID
}
