package top.bogey.yolo.service;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class YoloResult implements Parcelable {
    private final RectF area;
    private final String name;
    private final float similar;

    public YoloResult(RectF area, String name, float similar) {
        this.area = area;
        this.name = name;
        this.similar = similar;
    }

    protected YoloResult(Parcel in) {
        area = in.readParcelable(RectF.class.getClassLoader());
        name = in.readString();
        similar = in.readFloat();
    }

    public static final Creator<YoloResult> CREATOR = new Creator<>() {
        @Override
        public YoloResult createFromParcel(Parcel in) {
            return new YoloResult(in);
        }

        @Override
        public YoloResult[] newArray(int size) {
            return new YoloResult[size];
        }
    };

    public RectF getArea() {
        return area;
    }

    public float getSimilar() {
        return similar;
    }

    public String getName() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "YoloResult{" +
                "area=" + area +
                ", name='" + name + '\'' +
                ", similar=" + similar +
                '}';
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(area, flags);
        dest.writeString(name);
        dest.writeFloat(similar);
    }
}
