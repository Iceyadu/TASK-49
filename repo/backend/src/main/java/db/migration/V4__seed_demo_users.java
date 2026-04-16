package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class V4__seed_demo_users extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        ensureUserWithRole(connection,
                "student.integration",
                "student.integration@scholarops.local",
                "Integration Student",
                "Student@12345",
                "STUDENT");

        ensureUserWithRole(connection,
                "curator.integration",
                "curator.integration@scholarops.local",
                "Integration Curator",
                "Curator@12345",
                "CONTENT_CURATOR");

        ensureUserWithRole(connection,
                "curator.content.api",
                "curator.content.api@scholarops.local",
                "Content API Curator",
                "Curator@12345",
                "CONTENT_CURATOR");

        ensureUserWithRole(connection,
                "instructor.quiz.api",
                "instructor.quiz.api@scholarops.local",
                "Quiz API Instructor",
                "Instructor@12345",
                "INSTRUCTOR");

        ensureUserWithRole(connection,
                "instructor.qbank.api",
                "instructor.qbank.api@scholarops.local",
                "Question Bank API Instructor",
                "Instructor@12345",
                "INSTRUCTOR");

        ensureUserWithRole(connection,
                "student.quiz.api",
                "student.quiz.api@scholarops.local",
                "Quiz API Student",
                "Student@12345",
                "STUDENT");

        ensureUserWithRole(connection,
                "student.schedule.update",
                "student.schedule.update@scholarops.local",
                "Schedule API Student",
                "Student@12345",
                "STUDENT");

        ensureUserWithRole(connection,
                "student.timetable.api",
                "student.timetable.api@scholarops.local",
                "Timetable API Student",
                "Student@12345",
                "STUDENT");

        ensureUserWithRole(connection,
                "student.timetable.other",
                "student.timetable.other@scholarops.local",
                "Timetable Other Student",
                "Student@12345",
                "STUDENT");

        ensureUserWithRole(connection,
                "student.submission.api",
                "student.submission.api@scholarops.local",
                "Submission API Student One",
                "Student@12345",
                "STUDENT");

        ensureUserWithRole(connection,
                "student.submission.api.2",
                "student.submission.api.2@scholarops.local",
                "Submission API Student Two",
                "Student@12345",
                "STUDENT");

        ensureUserWithRole(connection,
                "ta.integration",
                "ta.integration@scholarops.local",
                "Integration Teaching Assistant",
                "Ta@12345",
                "TEACHING_ASSISTANT");
    }

    private static void ensureUserWithRole(Connection connection,
                                           String username,
                                           String email,
                                           String fullName,
                                           String rawPassword,
                                           String roleName) throws SQLException {
        Long userId = findUserId(connection, username);
        if (userId == null) {
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
            try (PreparedStatement insertUser = connection.prepareStatement(
                    "INSERT INTO users (username, email, password_hash, full_name, enabled, account_locked) " +
                            "VALUES (?, ?, ?, ?, TRUE, FALSE)")) {
                insertUser.setString(1, username);
                insertUser.setString(2, email);
                insertUser.setString(3, hashedPassword);
                insertUser.setString(4, fullName);
                insertUser.executeUpdate();
            }
            userId = findUserId(connection, username);
        }

        Long roleId = findRoleId(connection, roleName);
        if (userId == null || roleId == null) {
            throw new IllegalStateException("Missing user or role while seeding demo users");
        }

        if (!hasUserRole(connection, userId, roleId)) {
            try (PreparedStatement insertUserRole = connection.prepareStatement(
                    "INSERT INTO user_roles (user_id, role_id, assigned_by) VALUES (?, ?, NULL)")) {
                insertUserRole.setLong(1, userId);
                insertUserRole.setLong(2, roleId);
                insertUserRole.executeUpdate();
            }
        }
    }

    private static Long findUserId(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE username = ? LIMIT 1")) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return null;
            }
        }
    }

    private static Long findRoleId(Connection connection, String roleName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM roles WHERE name = ? LIMIT 1")) {
            statement.setString(1, roleName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return null;
            }
        }
    }

    private static boolean hasUserRole(Connection connection, Long userId, Long roleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM user_roles WHERE user_id = ? AND role_id = ? LIMIT 1")) {
            statement.setLong(1, userId);
            statement.setLong(2, roleId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }
}
