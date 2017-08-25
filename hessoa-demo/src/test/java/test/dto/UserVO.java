package test.dto;

import com.pugwoo.hessoa.test.api.entity.UserDO;

public class UserVO extends UserDO {

	private static final long serialVersionUID = 7405018399707021231L;

	private String mything;

	public String getMything() {
		return mything;
	}

	public void setMything(String mything) {
		this.mything = mything;
	}
	
}
