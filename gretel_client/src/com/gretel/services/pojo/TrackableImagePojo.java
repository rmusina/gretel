package com.gretel.services.pojo;

public class TrackableImagePojo {
	private double lat;
	
	private double lon;
	
	private String image;
	
	private String[][] imageDesc;
	
	private KeyPointPojo[] imageFeatures;

    public TrackableImagePojo() {
    }

    public TrackableImagePojo(double lat, double lon, String image, String[][] imageDesc, KeyPointPojo[] imageFeatures) {
        this.lat = lat;
        this.lon = lon;
        this.image = image;
        this.imageDesc = imageDesc;
        this.imageFeatures = imageFeatures;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getImage() {
        return image;
    }

    public String[][] getImageDesc() {
        return imageDesc;
    }

    public KeyPointPojo[] getImageFeatures() {
        return imageFeatures;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setImageDesc(String[][] imageDesc) {
        this.imageDesc = imageDesc;
    }

    public void setImageFeatures(KeyPointPojo[] imageFeatures) {
        this.imageFeatures = imageFeatures;
    }
}
