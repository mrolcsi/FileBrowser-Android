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
 * Rendezés módja (mappák mindig elöl)
 * <ul>
 * <li>Név szerint növekvő: {@link #BY_NAME_ASC BY_NAME_ASC}</li>
 * <li>Név szerint csökkenő: {@link #BY_NAME_DESC BY_NAME_DESC}</li>
 * <li>Kiterjesztés szerint növekvő: {@link #BY_EXTENSION_ASC BY_EXTENSION_ASC}</li>
 * <li>Kiterjesztés szerint csökkenő: {@link #BY_EXTENSION_DESC BY_EXTENSION_DESC}</li>
 * <li>Módosítás dátuma szerint növekvő: {@link #BY_DATE_ASC BY_DATE_ASC}</li>
 * <li>Módosítás dátuma szerint csökkenő: {@link #BY_DATE_DESC BY_DATE_DESC}</li>
 * <li>Méret szerint növekvő: {@link #BY_SIZE_ASC BY_SIZE_ASC}</li>
 * <li>Méret szerint növekvő: {@link #BY_SIZE_DESC BY_SIZE_DESC}</li>
 * </ul>
 * (Alapértelmezett: fájlnév szerint növekvő)
 */
public enum SortMode {
  /**
   * Név szerint rendezés, növekvő
   */
  BY_NAME_ASC,
  /**
   * Név szerint rendezés,. csökkenő
   */
  BY_NAME_DESC,
  /**
   * Kiterjesztés szerint rendezés, növekvő
   */
  BY_EXTENSION_ASC,
  /**
   * Kiterjesztés szerint rendezés, csökkenő
   */
  BY_EXTENSION_DESC,
  /**
   * Módosítás dátuma szerint rendezés, növekvő
   */
  BY_DATE_ASC,
  /**
   * Módosítás dátuma szerint rendezés, csökkenő
   */
  BY_DATE_DESC,
  /**
   * Méret szerint rendezés, növekvő
   * (Pontatlan, a nem olvasható mappák mérete 0 byte)
   */
  BY_SIZE_ASC,
  /**
   * Méret szerint rendezés, csökkenő
   * (Pontatlan, a nem olvasható mappák mérete 0 byte)
   */
  BY_SIZE_DESC
}
