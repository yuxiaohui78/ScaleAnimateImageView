package com.aries.scaleimageview.gesture;

public interface OnGesture {

    void onDrag(float dx, float dy);

    void onFling(float startX, float startY, float velocityX, float velocityY);

    void onScale(float rate, float focusX, float focusY);

}
