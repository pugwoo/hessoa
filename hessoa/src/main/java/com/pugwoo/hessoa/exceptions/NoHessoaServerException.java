package com.pugwoo.hessoa.exceptions;

public class NoHessoaServerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoHessoaServerException() {
		
	}
	
	public NoHessoaServerException(String errmsg) {
		super(errmsg);
	}
	
}
