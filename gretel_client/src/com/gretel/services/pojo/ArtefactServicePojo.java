package com.gretel.services.pojo;

import java.util.List;

public class ArtefactServicePojo {
	private int count;
	
	private int next;
	
	private int previous;
	
	private List<TrackableImagePojo> results;

	public ArtefactServicePojo(int count, int next, int previous,
			List<TrackableImagePojo> results) {
		super();
		this.count = count;
		this.next = next;
		this.previous = previous;
		this.results = results;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getNext() {
		return next;
	}

	public void setNext(int next) {
		this.next = next;
	}

	public int getPrevious() {
		return previous;
	}

	public void setPrevious(int previous) {
		this.previous = previous;
	}

	public List<TrackableImagePojo> getResults() {
		return results;
	}

	public void setResults(List<TrackableImagePojo> results) {
		this.results = results;
	}
}
