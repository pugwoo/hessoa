package com.pugwoo.hessoa.test.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pugwoo.hessoa.annotation.HessianService;
import com.pugwoo.hessoa.test.api.entity.StudentDO;
import com.pugwoo.hessoa.test.api.service.ISchoolService;

@HessianService(ISchoolService.class)
@Service
public class SchoolServiceImpl implements ISchoolService {

	@Override
	public List<StudentDO> getAll() {
		StudentDO studentDO = new StudentDO();
		studentDO.setName("nick");
		studentDO.setSchool("sysu");
		
		List<StudentDO> list = new ArrayList<StudentDO>();
		list.add(studentDO);
		return list;
	}

}
