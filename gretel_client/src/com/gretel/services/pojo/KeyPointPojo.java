package com.gretel.services.pojo;

public class KeyPointPojo {
	private double x;
	
	private double y;

    private float size;

	public KeyPointPojo(double x, double y, float size) {
		this.x = x;
		this.y = y;
        this.size = size;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }
}
