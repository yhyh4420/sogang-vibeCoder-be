package com.k.medtour.domain.proposal.repository;

import com.k.medtour.domain.proposal.entity.ProposalItem;
import com.k.medtour.domain.proposal.enums.ProposalItemCategory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcProposalItemRepository implements ProposalItemRepository {

    private final DataSource dataSource;

    public JdbcProposalItemRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ProposalItem save(ProposalItem item) {
        if (item.getId() == null) {
            item.prePersist();
            String sql = """
                INSERT INTO proposal_item (proposal_id, category, name, description, unit_price, quantity, amount,
                                           created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, item.getProposal() != null ? item.getProposal().getId() : 0);
                stmt.setString(2, item.getCategory().name());
                stmt.setString(3, item.getName());
                stmt.setString(4, item.getDescription());
                stmt.setBigDecimal(5, item.getUnitPrice());
                stmt.setInt(6, item.getQuantity());
                stmt.setBigDecimal(7, item.getAmount());
                stmt.setTimestamp(8, Timestamp.valueOf(item.getCreatedAt()));
                stmt.setTimestamp(9, Timestamp.valueOf(item.getUpdatedAt()));
                stmt.executeUpdate();
                try (var rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) item.setId(rs.getLong(1));
                }
                return item;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return item;
    }

    @Override
    public List<ProposalItem> findByProposalId(Long proposalId) {
        String sql = "SELECT * FROM proposal_item WHERE proposal_id = ? ORDER BY id";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, proposalId);
            try (var rs = stmt.executeQuery()) {
                List<ProposalItem> items = new ArrayList<>();
                while (rs.next()) {
                    ProposalItem item = ProposalItem.builder()
                            .category(ProposalItemCategory.valueOf(rs.getString("category")))
                            .name(rs.getString("name"))
                            .description(rs.getString("description"))
                            .unitPrice(rs.getBigDecimal("unit_price"))
                            .quantity(rs.getInt("quantity"))
                            .build();
                    item.setId(rs.getLong("id"));
                    item.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                    item.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
                    items.add(item);
                }
                return items;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
