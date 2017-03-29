package com.pugwoo.hessoa.exceptions;

public class WrongHessoaUrlException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public WrongHessoaUrlException() {
		
	}
	
	public WrongHessoaUrlException(String errmsg) {
		super(errmsg);
	}
	
}
