package uk.co.flxs.auth.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 *
 * @author paul
 */
public class UserRepo {

    private final DataSource dataSource;

    public UserRepo(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private UserDTO rowToUser(ResultSet rs) throws SQLException {
        UserDTO user = new UserDTO(
                rs.getString("id"),
                rs.getString("nickname"),
                rs.getString("passwordHash"),
                rs.getString("roles"));
        return user;
    }

    public Boolean isNicknameRegistered(String nickname) {
        try {
            UserDTO user = getUserByNickname(nickname);
            return true;
        } catch (UserNotFoundException ex) {
            return false;
        }
    }

    public UserDTO getUserByNickname(String nickname) throws UserStorageException {
        try (
            Connection conn = this.dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
        ){
            stmt.setString(1, nickname);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UserDTO user = rowToUser(rs);
                return user;
            }

            throw new UserNotFoundException("User not found");
        } catch (SQLException e) {
            throw new UserStorageException("Unable to fetch user record", e);
        }
    }

    public List<UserDTO> getAllUsers() {
        try (
            Connection conn = this.dataSource.getConnection();  
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
        ) {
            ResultSet rs = stmt.executeQuery();

            List<UserDTO> userList = new ArrayList<>();
            while (rs.next()) {
                userList.add(rowToUser(rs));
            }
            stmt.close();

            return userList;
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    public void insertUser(UserDTO user) throws UserStorageException {

        try (
            Connection conn = this.dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users "
                + "(id, nickname, passwordHash, roles) "
                + "VALUES (?, ?, ?, ?)");
        ) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getNickname());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRoles());

            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            if (e.getMessage().startsWith("Duplicate entry")) {
                throw new DuplicateUserException("User nickname has already been registered.");
            }
            throw new UserStorageException("Unable to insert user", e);
        }
    }
}
