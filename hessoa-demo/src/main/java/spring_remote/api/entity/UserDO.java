package spring_remote.api.entity;

import java.io.Serializable;

/**
 * 2012年11月21日 10:54:30
 * 数据结构类
 */
public class UserDO implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private String name; // 姓名
	private int score; // 分数

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}
}
