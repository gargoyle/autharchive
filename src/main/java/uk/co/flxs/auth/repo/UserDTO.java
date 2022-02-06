package uk.co.flxs.auth.repo;

/**
 * A simple DTO that matches the user table structure.
 */
public class UserDTO {

    private final String id;
    private final String nickname;
    private final String passwordHash;
    private final String roles;

    /**
     *
     * @param id
     * @param nickname
     * @param passwordHash BCrypt password hash
     * @param roles
     */
    public UserDTO(String id, String nickname, String passwordHash, String roles) {
        this.id = id;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.roles = roles;
    }

    public String getId() {
        return id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }


    public String getNickname() {
        return nickname;
    }

    public String getRoles() {
        return roles;
    }

}
