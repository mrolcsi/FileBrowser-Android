package hu.mrolcsi.android.filebrowser.option;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.02.11.
 * Time: 22:05
 */

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
