package edu.giorgio;

import java.util.concurrent.atomic.AtomicInteger;

public class Whisky {
	
	private static final AtomicInteger COUNTER = new AtomicInteger();
	
	private final int id;
	
	private String name;
	
	private String origin;
	
	public Whisky() {
		this.id = COUNTER.getAndIncrement();
	}
	
	public Whisky(String name, String origin) {
		this.id = COUNTER.getAndIncrement();
		this.name = name;
		this.origin = origin;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
	}

}
