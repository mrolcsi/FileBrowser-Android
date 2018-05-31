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

package hu.mrolcsi.android.filebrowser.util.itemclicksupport;

import android.support.v7.widget.RecyclerView;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.View;
import hu.mrolcsi.android.filebrowser.R;

public class ItemClickSupport {

  private final RecyclerView mRecyclerView;
  private final TouchListener mTouchListener;
  private OnItemClickListener mItemClickListener;
  private OnItemLongClickListener mItemLongClickListener;

  private ItemClickSupport(RecyclerView recyclerView) {
    mRecyclerView = recyclerView;

    mTouchListener = new TouchListener(recyclerView);
    recyclerView.addOnItemTouchListener(mTouchListener);
  }

  public static ItemClickSupport addTo(RecyclerView recyclerView) {
    ItemClickSupport itemClickSupport = from(recyclerView);
    if (itemClickSupport == null) {
      itemClickSupport = new ItemClickSupport(recyclerView);
      recyclerView.setTag(R.id.browser_item_click_support, itemClickSupport);
    }  // TODO: Log warning

    return itemClickSupport;
  }

  @SuppressWarnings("unused")
  public static void removeFrom(RecyclerView recyclerView) {
    final ItemClickSupport itemClickSupport = from(recyclerView);
    if (itemClickSupport == null) {
      // TODO: Log warning
      return;
    }

    recyclerView.removeOnItemTouchListener(itemClickSupport.mTouchListener);
    recyclerView.setTag(R.id.browser_item_click_support, null);
  }

  public static ItemClickSupport from(RecyclerView recyclerView) {
    if (recyclerView == null) {
      return null;
    }

    return (ItemClickSupport) recyclerView.getTag(R.id.browser_item_click_support);
  }

  /**
   * Register a callback to be invoked when an item in the
   * RecyclerView has been clicked.
   *
   * @param listener The callback that will be invoked.
   */
  public void setOnItemClickListener(OnItemClickListener listener) {
    mItemClickListener = listener;
  }

  /**
   * Register a callback to be invoked when an item in the
   * RecyclerView has been clicked and held.
   *
   * @param listener The callback that will be invoked.
   */
  public void setOnItemLongClickListener(OnItemLongClickListener listener) {
    if (!mRecyclerView.isLongClickable()) {
      mRecyclerView.setLongClickable(true);
    }

    mItemLongClickListener = listener;
  }

  /**
   * Interface definition for a callback to be invoked when an item in the
   * RecyclerView has been clicked.
   */
  public interface OnItemClickListener {

    /**
     * Callback method to be invoked when an item in the RecyclerView
     * has been clicked.
     *
     * @param parent The RecyclerView where the click happened.
     * @param view The view within the RecyclerView that was clicked
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was clicked.
     */
    void onItemClick(RecyclerView parent, View view, int position, long id);
  }

  /**
   * Interface definition for a callback to be invoked when an item in the
   * RecyclerView has been clicked and held.
   */
  public interface OnItemLongClickListener {

    /**
     * Callback method to be invoked when an item in the RecyclerView
     * has been clicked and held.
     *
     * @param parent The RecyclerView where the click happened
     * @param view The view within the RecyclerView that was clicked
     * @param position The position of the view in the list
     * @param id The row id of the item that was clicked
     * @return true if the callback consumed the long click, false otherwise
     */
    boolean onItemLongClick(RecyclerView parent, View view, int position, long id);
  }

  private class TouchListener extends ClickItemTouchListener {

    TouchListener(RecyclerView recyclerView) {
      super(recyclerView);
    }

    @Override
    boolean performItemClick(RecyclerView parent, View view, int position, long id) {
      if (mItemClickListener != null) {
        view.playSoundEffect(SoundEffectConstants.CLICK);
        mItemClickListener.onItemClick(parent, view, position, id);
        return true;
      }

      return false;
    }

    @Override
    boolean performItemLongClick(RecyclerView parent, View view, int position, long id) {
      if (mItemLongClickListener != null) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        return mItemLongClickListener.onItemLongClick(parent, view, position, id);
      }

      return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
  }
}