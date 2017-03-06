package com.example.realm.base;

import java.util.ArrayList;
import java.util.List;

import charting.data.BaseDataSet;
import charting.data.DataSet;
import charting.data.Entry;
import io.realm.DynamicRealmObject;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;


/**
 * Created by seobink on 2017-02-28.
 */

public abstract class RealmBaseDataSet<T extends RealmObject, S extends Entry> extends BaseDataSet<S> {

    /**
     * a list of queried realm objects
     */
    protected RealmResults<T> results;

    /**
     * a cached list of all data read from the database
     */
    protected List<S> mValues;

    /**
     * maximum y-value in the value array
     */
    protected float mYMax = -Float.MAX_VALUE;

    /**
     * minimum y-value in the value array
     */
    protected float mYMin = Float.MAX_VALUE;

    /**
     * maximum x-value in the value array
     */
    protected float mXMax = -Float.MAX_VALUE;

    /**
     * minimum x-value in the value array
     */
    protected float mXMin = Float.MAX_VALUE;

    /**
     * fieldname of the column that contains the y-values of this dataset
     */
    protected String mYValuesField;

    /**
     * fieldname of the column that contains the x-values of this dataset
     */
    protected String mXValuesField;

    public RealmBaseDataSet(RealmResults<T> results, String yValuesField) {
        this.results = results;
        this.mYValuesField = yValuesField;
        this.mValues = new ArrayList<S>();

        if (mXValuesField != null)
            this.results.sort(mXValuesField, Sort.ASCENDING);
    }

    /**
     * Constructor that takes the realm RealmResults, sorts & stores them.
     *
     * @param results
     * @param xValuesField
     * @param yValuesField
     */
    public RealmBaseDataSet(RealmResults<T> results, String xValuesField, String yValuesField) {
        this.results = results;
        this.mYValuesField = yValuesField;
        this.mXValuesField = xValuesField;
        this.mValues = new ArrayList<S>();

        if (mXValuesField != null)
            this.results.sort(mXValuesField, Sort.ASCENDING);
    }

    /**
     * Rebuilds the DataSet based on the given RealmResults.
     */
    public void build(RealmResults<T> results) {

        int xIndex = 0;
        for (T object : results) {
            mValues.add(buildEntryFromResultObject(object, xIndex++));
        }
    }

    public S buildEntryFromResultObject(T realmObject, float x) {
        DynamicRealmObject dynamicObject = new DynamicRealmObject(realmObject);

        return (S) new Entry(mXValuesField == null ? x : dynamicObject.getFloat(mXValuesField), dynamicObject.getFloat(mYValuesField));
    }

    @Override
    public float getYMin() {
        //return results.min(mYValuesField).floatValue();
        return mYMin;
    }

    @Override
    public float getYMax() {
        //return results.max(mYValuesField).floatValue();
        return mYMax;
    }

    @Override
    public float getXMin() {
        return mXMin;
    }

    @Override
    public float getXMax() {
        return mXMax;
    }

    @Override
    public int getEntryCount() {
        return mValues.size();
    }

    @Override
    public void calcMinMax() {

        if (mValues == null || mValues.isEmpty())
            return;

        mYMax = -Float.MAX_VALUE;
        mYMin = Float.MAX_VALUE;
        mXMax = -Float.MAX_VALUE;
        mXMin = Float.MAX_VALUE;

        for (S e : mValues) {
            calcMinMax(e);
        }
    }

    @Override
    public void calcMinMaxY(float fromX, float toX) {

        if (mValues == null || mValues.isEmpty())
            return;

        mYMax = -Float.MAX_VALUE;
        mYMin = Float.MAX_VALUE;

        int indexFrom = getEntryIndex(fromX, Float.NaN, DataSet.Rounding.DOWN);
        int indexTo = getEntryIndex(toX, Float.NaN, DataSet.Rounding.UP);

        for (int i = indexFrom; i <= indexTo; i++) {

            // only recalculate y
            calcMinMaxY(mValues.get(i));
        }
    }

    /**
     * Updates the min and max x and y value of this DataSet based on the given Entry.
     *
     * @param e
     */
    protected void calcMinMax(S e) {

        if (e == null)
            return;

        calcMinMaxX(e);

        calcMinMaxY(e);
    }

    protected void calcMinMaxX(S e) {

        if (e.getX() < mXMin)
            mXMin = e.getX();

        if (e.getX() > mXMax)
            mXMax = e.getX();
    }

    protected void calcMinMaxY(S e) {

        if (e.getY() < mYMin)
            mYMin = e.getY();

        if (e.getY() > mYMax)
            mYMax = e.getY();
    }

    @Override
    public S getEntryForXValue(float xValue, float closestToY, DataSet.Rounding rounding) {

        int index = getEntryIndex(xValue, closestToY, rounding);
        if (index > -1)
            return mValues.get(index);
        return null;
    }

    @Override
    public S getEntryForXValue(float xValue, float closestToY) {
        return getEntryForXValue(xValue, closestToY, DataSet.Rounding.CLOSEST);
    }

    @Override
    public List<S> getEntriesForXValue(float xVal) {

        List<S> entries = new ArrayList<>();

        if (mXValuesField != null) {
            RealmResults<T> foundObjects = results.where().equalTo(mXValuesField, xVal).findAll();

            for (T e : foundObjects)
                entries.add(buildEntryFromResultObject(e, xVal));
        }

        return entries;
    }

    @Override
    public S getEntryForIndex(int index) {
        //DynamicRealmObject o = new DynamicRealmObject(results.get(index));
        //return new Entry(o.getFloat(mYValuesField), o.getInt(mXValuesField));
        return mValues.get(index);
    }

    @Override
    public int getEntryIndex(float xValue, float closestToY, DataSet.Rounding rounding) {

        if (mValues == null || mValues.isEmpty())
            return -1;

        int low = 0;
        int high = mValues.size() - 1;
        int closest = high;

        while (low < high) {
            int m = (low + high) / 2;

            final float d1 = mValues.get(m).getX() - xValue,
                    d2 = mValues.get(m + 1).getX() - xValue,
                    ad1 = Math.abs(d1), ad2 = Math.abs(d2);

            if (ad2 < ad1) {
                // [m + 1] is closer to xValue
                // Search in an higher place
                low = m + 1;
            } else if (ad1 < ad2) {
                // [m] is closer to xValue
                // Search in a lower place
                high = m;
            } else {
                // We have multiple sequential x-value with same distance

                if (d1 >= 0.0) {
                    // Search in a lower place
                    high = m;
                } else if (d1 < 0.0) {
                    // Search in an higher place
                    low = m + 1;
                }
            }

            closest = high;
        }

        if (closest != -1) {
            float closestXValue = mValues.get(closest).getX();
            if (rounding == DataSet.Rounding.UP) {
                // If rounding up, and found x-value is lower than specified x, and we can go upper...
                if (closestXValue < xValue && closest < mValues.size() - 1) {
                    ++closest;
                }
            } else if (rounding == DataSet.Rounding.DOWN) {
                // If rounding down, and found x-value is upper than specified x, and we can go lower...
                if (closestXValue > xValue && closest > 0) {
                    --closest;
                }
            }

            // Search by closest to y-value
            if (!Float.isNaN(closestToY)) {
                while (closest > 0 && mValues.get(closest - 1).getX() == closestXValue)
                    closest -= 1;

                float closestYValue = mValues.get(closest).getY();
                int closestYIndex = closest;

                while (true) {
                    closest += 1;
                    if (closest >= mValues.size())
                        break;

                    final Entry value = mValues.get(closest);

                    if (value.getX() != closestXValue)
                        break;

                    if (Math.abs(value.getY() - closestToY) < Math.abs(closestYValue - closestToY)) {
                        closestYValue = closestToY;
                        closestYIndex = closest;
                    }
                }

                closest = closestYIndex;
            }
        }

        return closest;
    }

    @Override
    public int getEntryIndex(S e) {
        return mValues.indexOf(e);
    }

    @Override
    public boolean addEntry(S e) {

        if (e == null)
            return false;

        float val = e.getY();

        if (mValues == null) {
            mValues = new ArrayList<S>();
        }

        calcMinMax(e);

        // add the entry
        mValues.add(e);
        return true;
    }

    @Override
    public boolean removeEntry(S e) {

        if (e == null)
            return false;

        if (mValues == null)
            return false;

        // remove the entry
        boolean removed = mValues.remove(e);

        if (removed) {
            calcMinMax();
        }

        return removed;
    }

    @Override
    public void addEntryOrdered(S e) {

        if (e == null)
            return;

        if (mValues == null) {
            mValues = new ArrayList<S>();
        }

        calcMinMax(e);

        if (mValues.size() > 0 && mValues.get(mValues.size() - 1).getX() > e.getX()) {
            int closestIndex = getEntryIndex(e.getX(), e.getY(), DataSet.Rounding.UP);
            mValues.add(closestIndex, e);
        } else {
            mValues.add(e);
        }
    }

    /**
     * Returns the List of values that has been extracted from the RealmResults
     * using the provided fieldnames.
     *
     * @return
     */
    public List<S> getValues() {
        return mValues;
    }

    @Override
    public void clear() {
        mValues.clear();
        notifyDataSetChanged();
    }

    public RealmResults<T> getResults() {
        return results;
    }

    /**
     * Returns the fieldname that represents the "y-values" in the realm-data.
     *
     * @return
     */
    public String getYValuesField() {
        return mYValuesField;
    }

    /**
     * Sets the field name that is used for getting the y-values out of the RealmResultSet.
     *
     * @param yValuesField
     */
    public void setYValuesField(String yValuesField) {
        this.mYValuesField = yValuesField;
    }

    /**
     * Returns the fieldname that represents the "x-values" in the realm-data.
     *
     * @return
     */
    public String getXValuesField() {
        return mXValuesField;
    }

    /**
     * Sets the field name that is used for getting the x-values out of the RealmResultSet.
     *
     * @param xValuesField
     */
    public void setXValuesField(String xValuesField) {
        this.mXValuesField = xValuesField;
    }
}
