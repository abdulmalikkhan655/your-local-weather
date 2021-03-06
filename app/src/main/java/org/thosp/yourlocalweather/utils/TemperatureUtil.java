package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Weather;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TemperatureUtil {

    private static double G0 = 1395; // solar constant (w/m2)
    private static double transmissionCoefficientClearDay = 0.81;
    private static double transmissionCoefficientCloudy = 0.62;

    public static float getApparentTemperature(double dryBulbTemperature,
                                               int humidity,
                                               double windSpeed,
                                               int cloudiness,
                                               double latitude,
                                               long timestamp) {
        return getApparentTemperatureWithoutSolarIrradiation(dryBulbTemperature, humidity, windSpeed);
    }

    public static float getApparentTemperatureWithoutSolarIrradiation(double dryBulbTemperature, int humidity, double windSpeed) {
        double e = (humidity / 100) * 6.105 * Math.exp((17.27*dryBulbTemperature) / (237.7 + dryBulbTemperature));
        double apparentTemperature = dryBulbTemperature + (0.33*e)-(0.70*windSpeed)-4.00;
        return (float)apparentTemperature;
    }

    public static float getApparentTemperatureWithSolarIrradiation2(double dryBulbTemperature,
                                                                   int humidity,
                                                                   double windSpeed,
                                                                   int cloudiness,
                                                                   double latitude,
                                                                   long timestamp) {
        double e = (humidity / 100) * 6.105 * Math.exp((17.27*dryBulbTemperature) / (237.7 + dryBulbTemperature));
        double zenithAngle = getZenithAngle(latitude, timestamp);
        double cosOfZenithAngle = Math.cos(zenithAngle);
        double secOfZenithAngle = 1/cosOfZenithAngle;
        double transmissionCoefficient = transmissionCoefficientCloudy +
                (transmissionCoefficientClearDay - transmissionCoefficientCloudy) * cloudiness;
        double Q = (G0*Math.cos(zenithAngle)*Math.pow(transmissionCoefficient, secOfZenithAngle))/16;
        double apparentTemperature = dryBulbTemperature + (0.348*e)-(0.70*windSpeed) + ((0.70*Math.abs(Q))/(windSpeed + 10)) - 4.25;
        return (float)apparentTemperature;
    }

    private static double getZenithAngle(double latitude, long timestamp) {
        Calendar measuredTime = Calendar.getInstance();
        measuredTime.setTimeInMillis(timestamp);
        double declination = -23.44 * Math.cos(360/365 * (9 + measuredTime.get(Calendar.DAY_OF_YEAR)));
        double hourAngle = measuredTime.get(Calendar.HOUR_OF_DAY)*(360/24) + (measuredTime.get(Calendar.MINUTE) * (360/1440));
        return Math.acos(Math.sin(latitude)*Math.sin(declination)) + (Math.cos(latitude) * Math.cos(declination) + Math.cos(hourAngle));
    }

    public static float getCanadianStandardTemperature(double dryBulbTemperature, double windSpeed) {
        double windWithPow = Math.pow(windSpeed, 0.16);
        return (float) (13.12 + (0.6215 * dryBulbTemperature) - (13.37 * windWithPow) + (0.486 * dryBulbTemperature * windWithPow));
    }

    public static String getSecondTemperatureWithLabel(Context context, Weather weather, double latitude, long timestamp) {
        if (weather == null) {
            return null;
        }
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        if ("measured_only".equals(temperatureTypeFromPreferences) || "appearance_only".equals(temperatureTypeFromPreferences)) {
            return null;
        }
        int label = R.string.label_measured_temperature;
        if ("measured_appearance_primary_measured".equals(temperatureTypeFromPreferences)) {
            label = R.string.label_apparent_temperature;
        }
        return context.getString(label,
                getSecondTemperatureWithUnit(
                        context,
                        weather,
                        latitude,
                        timestamp));
    }

    public static String getSecondTemperatureWithUnit(Context context, Weather weather, double latitude, long timestamp) {
        if (weather == null) {
            return null;
        }
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        if ("measured_only".equals(temperatureTypeFromPreferences) || "appearance_only".equals(temperatureTypeFromPreferences)) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String apparentSign = "";
        double value = weather.getTemperature();
        if ("measured_appearance_primary_measured".equals(temperatureTypeFromPreferences)) {
            apparentSign = "~";
            value = TemperatureUtil.getApparentTemperature(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed(),
                    weather.getClouds(),
                    latitude,
                    timestamp);
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return apparentSign + String.format(Locale.getDefault(), "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return apparentSign + String.format(Locale.getDefault(), "%d",
                    Math.round(value)) + getTemperatureUnit(context);
        }
    }

    public static String getTemperatureWithUnit(Context context, Weather weather, double latitude, long timestamp) {
        if (weather == null) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        String apparentSign = "";
        double value = weather.getTemperature();
        if ("appearance_only".equals(temperatureTypeFromPreferences) ||
                ("measured_appearance_primary_appearance".equals(temperatureTypeFromPreferences))) {
            apparentSign = "~";
            //value = TemperatureUtil.getApparentTemperature(weather.getTemperature(), weather.getHumidity(), weather.getWindSpeed());
            value = TemperatureUtil.getApparentTemperature(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed(),
                    weather.getClouds(),
                    latitude,
                    timestamp);
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return apparentSign + String.format(Locale.getDefault(), "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return apparentSign + String.format(Locale.getDefault(), "%d",
                    Math.round(value)) + getTemperatureUnit(context);
        }
    }

    public static String getForecastedTemperatureWithUnit(Context context, DetailedWeatherForecast weather) {
        if (weather == null) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        String apparentSign = "";
        double value = weather.getTemperature();
        if ("appearance_only".equals(temperatureTypeFromPreferences) ||
                ("measured_appearance_primary_appearance".equals(temperatureTypeFromPreferences))) {
            apparentSign = "~";
            value = TemperatureUtil.getApparentTemperatureWithoutSolarIrradiation(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed());
        }
        if (value > 0) {
            apparentSign += "+";
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return apparentSign + String.format(Locale.getDefault(), "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return apparentSign + String.format(Locale.getDefault(), "%d",
                    Math.round(value)) + getTemperatureUnit(context);
        }
    }

    public static String getTemperatureUnit(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return "°F";
        } else {
            return "°C";
        }
    }

    public static double getTemperature(Context context, DetailedWeatherForecast weather) {
        if (weather == null) {
            return 0;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        double value = weather.getTemperature();
        if ("appearance_only".equals(temperatureTypeFromPreferences) ||
                ("measured_appearance_primary_appearance".equals(temperatureTypeFromPreferences))) {
            value = TemperatureUtil.getApparentTemperatureWithoutSolarIrradiation(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed());
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return (value * 1.8d) + 32;
        } else {
            return value;
        }
    }
}
