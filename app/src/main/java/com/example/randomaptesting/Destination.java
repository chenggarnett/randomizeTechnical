package com.example.randomaptesting;

import java.io.Serializable;

public class Destination implements Serializable{

    private String name;
    private String address;
    private String id;
    private double distance;
    private double rating;
    private int price;

    public Destination(String n, String a , String i, double d, double r, int p) {
        name = n;
        address = a;
        id = i;
        distance = d;
        rating = r;
        price = p;
    }

    public Destination(String n, String a , String i, double d, double r) {
        name = n;
        address = a;
        id = i;
        distance = d;
        rating = r;
    }

    public Destination(String n, String a, String i, double d) {
        name = n;
        address = a;
        id = i;
        distance = d;
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

    public boolean equals(Object x) {
        Destination d = (Destination)x;
        if (name.length() != d.getName().length())
            return false;

        if (this.hashCode() != d.hashCode())
            return false;

        return true;
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
                + "Rating: " + rating;
    }
}
