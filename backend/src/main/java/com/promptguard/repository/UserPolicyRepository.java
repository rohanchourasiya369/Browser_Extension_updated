package com.promptguard.repository;

import com.promptguard.model.UserKeywordPolicy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserPolicyRepository {

    private final JdbcTemplate db;

    public UserPolicyRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<UserKeywordPolicy> findPolicies(String userId, String subUser) {
        String sql = "SELECT id, user_id AS userId, sub_user AS subUser, " +
                "keyword_list AS keywordList, " +
                "allow_col AS allowCol, redacted_col AS redactedCol, " +
                "critial_col AS critialCol, block_col AS blockCol, " +
                "prompt_col AS promptCol " +
                "FROM user_keyword_policies " +
                "WHERE user_id = ? AND sub_user = ?";
        return db.query(sql, new BeanPropertyRowMapper<>(UserKeywordPolicy.class), userId, subUser);
    }
}
