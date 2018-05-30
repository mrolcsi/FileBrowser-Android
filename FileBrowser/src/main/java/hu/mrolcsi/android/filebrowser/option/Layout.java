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
