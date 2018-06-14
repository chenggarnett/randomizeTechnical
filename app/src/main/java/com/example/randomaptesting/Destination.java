package com.example.randomaptesting;

import java.io.Serializable;

public class Destination implements Serializable{

    private String name;
    private String address;
    private String id;
    private double rating;

    public Destination(String n, String a , String i,  double r) {
        name = n;
        address = a;
        id = i;
        rating = r;
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

    public double getRating() {
        return rating;
    }

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
}
