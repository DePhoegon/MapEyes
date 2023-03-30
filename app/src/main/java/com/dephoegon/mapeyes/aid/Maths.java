package com.dephoegon.mapeyes.aid;

import androidx.annotation.NonNull;

public class Maths {
    public static double Fahrenheit(double Kelvin) {
        double numerator = (Kelvin - 273.15)*9;
        double denominator = 5;
        return (numerator/denominator)+32;
    }
    @NonNull
    public static String Direction(double degree) {
        int index = Math.toIntExact(Math.round(((degree %= 360) < 0 ? degree + 360 : degree) / 45) % 8);
        return index == 1 ? "North" : index == 2 ? "North-West" : index == 3 ? "West" : index == 4 ? "South-West" : index == 5 ? "South" : index == 6 ? "South-East" : index == 7 ? "East" : index == 8 ? "North-East" :"invalid";
    }
}