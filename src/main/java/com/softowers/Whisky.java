package com.softowers;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

public class Whisky {
	
	private static final AtomicInteger COUNTER = new AtomicInteger();
	
	private final Integer id;
	
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

	public Whisky(Integer id, String name, String origin) {
		this.id = id;
		this.name = name;
		this.origin = origin;
	}

	public Whisky(JsonObject json) {
		if(json != null) {
			this.id = json.getInteger("ID");
			this.name = json.getString("NAME");
			this.origin = json.getString("ORIGIN");
		} else {
			this.id = null;
		}
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
