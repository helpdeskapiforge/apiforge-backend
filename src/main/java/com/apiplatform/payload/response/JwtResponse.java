package com.apiplatform.payload.response;

import lombok.Data;

@Data
public class JwtResponse {
	private String token;
	private String type = "Bearer";
	private Long id;
	private String username;
    private String email;
    private String firstName;
    private String lastName;


	public JwtResponse(String accessToken, Long id, String username, String email, String firstName, String lastName) {
		this.token = accessToken;
		this.id = id;
		this.username = username;
		this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
	}
}