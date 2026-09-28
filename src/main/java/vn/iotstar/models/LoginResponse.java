package vn.iotstar.models;
// expiresIn uses milliseconds to match the lecture.
public record LoginResponse(String token, long expiresIn) {}
