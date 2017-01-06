
package com.gs.textimagecapturedemo;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;

import java.util.HashSet;
import java.util.Set;

public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    public static Set<TextBlock> myItems = new HashSet<>();

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    public OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay) {
        mGraphicOverlay = ocrGraphicOverlay;
    }

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        mGraphicOverlay.clear();

        if(myItems != null && !myItems.isEmpty()){

            myItems.clear();

        }

        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                Log.d("OcrDetectorProcessor", "Text detected! " + item.getValue());
            }
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
            mGraphicOverlay.add(graphic);
            myItems.add(item);
        }

    }

    @Override
    public void release() {
        mGraphicOverlay.clear();
//        myItems = null;
    }

}
