package com.pugwoo.hessoa.test.api.entity;

import java.io.Serializable;

public class StudentDO implements Serializable {

	private static final long serialVersionUID = 9056428080125236141L;

	private String name;
	
	private String school;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSchool() {
		return school;
	}

	public void setSchool(String school) {
		this.school = school;
	}
	
}
