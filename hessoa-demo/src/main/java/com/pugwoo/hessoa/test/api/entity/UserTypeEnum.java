package com.pugwoo.hessoa.test.api.entity;

import java.util.Objects;

public enum UserTypeEnum {

	COMMON("COMMON", "普通会员"),
	VIP("VIP", "VIP会员");

	private String code;

	private String name;

	private UserTypeEnum(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public static UserTypeEnum getByCode(String code) {
		for (UserTypeEnum e : UserTypeEnum.values()) {
			if (Objects.equals(e.getCode(), code)) {
				return e;
			}
		}
		return null;
	}

	public static String getNameByCode(String code) {
		UserTypeEnum e = getByCode(code);
		return e == null ? null : e.getName();
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
