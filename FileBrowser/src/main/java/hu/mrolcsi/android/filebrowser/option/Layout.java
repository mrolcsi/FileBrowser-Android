package hu.mrolcsi.android.filebrowser.option;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.02.11.
 * Time: 22:23
 */

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
