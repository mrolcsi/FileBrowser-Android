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
 * Tallózás módja:
 * <ul>
 * <li>Fájl megnyitása: {@link #OPEN_FILE OPEN_FILE}</li>
 * <li>Mappa kiválasztása: {@link #SELECT_DIR SELECT_DIR}</li>
 * <li>Fájl mentése: {@link #SAVE_FILE SAVE_FILE}</li>
 * </ul>
 * (Alapértelmezett: fájl megnyitása)
 */
public enum BrowseMode {
  /**
   * Fájl megnyitása
   */
  OPEN_FILE,
  /**
   * Mappa kiválasztása
   */
  SELECT_DIR,
  /**
   * Fájl mentése
   */
  SAVE_FILE
}
