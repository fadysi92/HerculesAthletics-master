package com.capstone.solemate.solemate;

import android.app.Application;

/**
 * Created by Shayan Mukhtar on 4/2/2015.
 */
public class MyApplication extends Application {
    private double RMSValueLeftBicep;
    private double RMSValueRightBicep;

    public double getRMSValueLeftBicep()
    {
        return RMSValueLeftBicep;
    }

    public void setRMSValueLeftBicep(double value)
    {
        RMSValueLeftBicep = value;
    }

    public double getRMSValueRightBicep()
    {
        return RMSValueRightBicep;
    }

    public void setRMSValueRightBicep(double value)
    {
        RMSValueRightBicep = value;
    }
}
