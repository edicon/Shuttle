package com.simplecity.amp_library.model.cue;

public class CueParseException extends Exception{
	
	private static final long serialVersionUID = 1L;

    public CueParseException() {}

    public CueParseException(String message){
       super(message);
    }
}
