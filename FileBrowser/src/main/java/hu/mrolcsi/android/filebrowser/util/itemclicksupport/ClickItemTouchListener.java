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

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

abstract class ClickItemTouchListener implements OnItemTouchListener {

  private final GestureDetectorCompat mGestureDetector;
  private final ClickGestureListener mClickGestureListener;

  ClickItemTouchListener(RecyclerView hostView) {
    mClickGestureListener = new ClickGestureListener(hostView);
    mGestureDetector = new GestureDetectorCompat(hostView.getContext(), mClickGestureListener);
  }

  @TargetApi(19)
  private boolean isAttachedToWindow(RecyclerView hostView) {
    if (Build.VERSION.SDK_INT >= 19) {
      return hostView.isAttachedToWindow();
    } else {
      return hostView.getHandler() != null;
    }
  }

  private boolean hasAdapter(RecyclerView hostView) {
    return hostView.getAdapter() != null;
  }

  @Override
  public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
    if (!isAttachedToWindow(recyclerView) || !hasAdapter(recyclerView)) {
      return false;
    }

    mGestureDetector.onTouchEvent(event);

    final int action = event.getAction() & MotionEvent.ACTION_MASK;
    if (action == MotionEvent.ACTION_UP) {
      mClickGestureListener.dispatchSingleTapUpIfNeeded(event);
    }

    return false;
  }

  @Override
  public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {
    // We can silently track tap and and long presses by silently
    // intercepting touch events in the host RecyclerView.
  }

  abstract boolean performItemClick(RecyclerView parent, View view, int position, long id);

  abstract boolean performItemLongClick(RecyclerView parent, View view, int position, long id);

  private class ClickGestureListener extends SimpleOnGestureListener {

    private final RecyclerView mHostView;
    private View mTargetChild;

    public ClickGestureListener(RecyclerView hostView) {
      mHostView = hostView;
    }

    public void dispatchSingleTapUpIfNeeded(MotionEvent event) {
      // When the long press hook is called but the long press listener
      // returns false, the target child will be left around to be
      // handled later. In this case, we should still treat the gesture
      // as potential item click.
      if (mTargetChild != null) {
        onSingleTapUp(event);
      }
    }

    @Override
    public boolean onDown(MotionEvent event) {
      final int x = (int) event.getX();
      final int y = (int) event.getY();

      mTargetChild = mHostView.findChildViewUnder(x, y);
      return mTargetChild != null;
    }

    @Override
    public void onShowPress(MotionEvent event) {
      if (mTargetChild != null) {
        mTargetChild.setPressed(true);
      }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
      boolean handled = false;

      if (mTargetChild != null) {
        mTargetChild.setPressed(false);

        final int position = mHostView.getChildAdapterPosition(mTargetChild);
        final long id = mHostView.getAdapter().getItemId(position);
        handled = performItemClick(mHostView, mTargetChild, position, id);

        mTargetChild = null;
      }

      return handled;
    }

    @Override
    public boolean onScroll(MotionEvent event, MotionEvent event2, float v, float v2) {
      if (mTargetChild != null) {
        mTargetChild.setPressed(false);
        mTargetChild = null;

        return true;
      }

      return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
      if (mTargetChild == null) {
        return;
      }

      final int position = mHostView.getChildAdapterPosition(mTargetChild);
      final long id = mHostView.getAdapter().getItemId(position);
      final boolean handled = performItemLongClick(mHostView, mTargetChild, position, id);

      if (handled) {
        mTargetChild.setPressed(false);
        mTargetChild = null;
      }
    }
  }
}