package com.example.randomaptesting;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Comparator;

public class Destination implements Serializable, Comparable<Destination> {

    private String name;
    private String address;
    private String id;
    private double distance;
    private double rating;
    private int price;
    private String telNo;
    private String website;

    public Destination(String n, String a , String i, double d) {
        name = n;
        address = a;
        id = i;
        distance = d;
        rating = 0;
        price = 0;
        telNo = "";
        website = "";
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getId() {
        return id;
    }

    public double getDistance() { return distance; }

    public double getRating() {
        return rating;
    }

    public int getPrice() { return price; }

    public String getTelNo() { return telNo; }

    public String getWebsite() { return website; }

    public void setName(String n) { name = n; }

    public void setAddress(String a) { address = a; }

    public void setId(String i) { id = i; }

    public void setDistance(double d) { distance = d; }

    public void setRating(double r) { rating = r; }

    public void setPrice(int p) { price = p; }

    public void setTelNo(String t) { telNo = t; }

    public void setWebsite(String w) { website = w; }



    public boolean equals(Object x) {
        Destination d = (Destination)x;
        return (compareTo(d) == 0);
    }

    public int hashCode() {
        char[] nameArray = name.toCharArray();
        int sum = 0;
        for (int i = 0; i < nameArray.length; i++) {
            sum +=  (int)nameArray[i];
        }
//        System.out.println("Name:" + name + " Sum: " + sum); // debug purpose
        return sum;
    }

    @Override
    public String toString() {
        return "Name: " + name + "\n"
                + "Address: " + address + "\n"
                + "Place ID: " + id + "\n"
                + "Distance: " + distance + " meters" + "\n"
                + "Rating: " + rating +"\n"
                + "Price Level: " + price +"\n"
                + "Telephone No: " + telNo +"\n"
                + "Website: " + website;
    }

    @Override
    public int compareTo(@NonNull Destination d) {

        if (this.name.length() == d.name.length() && this.hashCode() == d.hashCode())
            return 0;

        else if (this.name.length() < d.name.length())
            return -1;

        else
            return 1;
    }

    public static final Comparator<Destination> DistanceComparator = new Comparator<Destination>() {
        @Override
        public int compare(Destination d1, Destination d2) {
            return (int)Math.signum(d1.distance - d2.distance);
        }
    };

    public static final Comparator<Destination> RatingComparator = new Comparator<Destination>() {
        @Override
        public int compare(Destination d1, Destination d2) {
            return (int)Math.signum(d1.rating - d2.rating);
        }
    };

    public static final Comparator<Destination> PriceComparator = new Comparator<Destination>() {
        @Override
        public int compare(Destination d1, Destination d2) {
            return (int)Math.signum(d1.price - d2.price);
        }
    };
}
