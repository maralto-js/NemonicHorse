package com.nemonichorse.repository;

import com.nemonichorse.model.HorseData;
import com.nemonichorse.model.HorseRace;
import com.nemonichorse.model.HorseRarity;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class SQLiteHorseRepository implements HorseRepository {

    private static final String CREATE_HORSES = """
            CREATE TABLE IF NOT EXISTS nh_horses (
                id TEXT PRIMARY KEY,
                owner_id TEXT NOT NULL,
                name TEXT,
                rarity TEXT NOT NULL DEFAULT 'COMMON',
                race TEXT NOT NULL DEFAULT 'PLAINS',
                level INTEGER NOT NULL DEFAULT 1,
                xp REAL NOT NULL DEFAULT 0,
                bond_level INTEGER NOT NULL DEFAULT 0,
                bond_xp REAL NOT NULL DEFAULT 0,
                equipped_skill TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )""";

    private static final String CREATE_COOLDOWNS = """
            CREATE TABLE IF NOT EXISTS nh_skill_cooldowns (
                horse_id TEXT NOT NULL,
                skill_id TEXT NOT NULL,
                last_used INTEGER NOT NULL,
                PRIMARY KEY (horse_id, skill_id),
                FOREIGN KEY (horse_id) REFERENCES nh_horses(id) ON DELETE CASCADE
            )""";

    private static final String CREATE_INDEX_OWNER =
            "CREATE INDEX IF NOT EXISTS idx_nh_horses_owner ON nh_horses(owner_id)";

    private static final String UPSERT_HORSE = """
            INSERT INTO nh_horses
                (id,owner_id,name,rarity,race,level,xp,bond_level,bond_xp,equipped_skill,created_at,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
                owner_id=excluded.owner_id, name=excluded.name, rarity=excluded.rarity,
                race=excluded.race, level=excluded.level, xp=excluded.xp,
                bond_level=excluded.bond_level, bond_xp=excluded.bond_xp,
                equipped_skill=excluded.equipped_skill, updated_at=excluded.updated_at""";

    private static final String UPSERT_COOLDOWN =
            "INSERT INTO nh_skill_cooldowns(horse_id,skill_id,last_used) VALUES(?,?,?) " +
            "ON CONFLICT(horse_id,skill_id) DO UPDATE SET last_used=excluded.last_used";

    private static final String SELECT_HORSE =
            "SELECT * FROM nh_horses WHERE id=?";

    private static final String SELECT_COOLDOWNS =
            "SELECT skill_id, last_used FROM nh_skill_cooldowns WHERE horse_id=?";

    private static final String SELECT_BY_OWNER =
            "SELECT * FROM nh_horses WHERE owner_id=?";

    private static final String DELETE_HORSE =
            "DELETE FROM nh_horses WHERE id=?";

    private final File dbFile;
    private final Logger logger;
    private Connection connection;

    public SQLiteHorseRepository(File dbFile, Logger logger) {
        this.dbFile = dbFile;
        this.logger = logger;
    }

    // All public methods are synchronized to prevent concurrent access to the single
    // JDBC Connection from both the main thread (loadOrCreate) and async save tasks.
    // SQLite WAL allows concurrent readers but the Connection object itself is not thread-safe.

    @Override
    public synchronized void initialize() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA cache_size=1000");
            stmt.execute("PRAGMA temp_store=memory");
            stmt.execute("PRAGMA foreign_keys=ON");

            stmt.execute(CREATE_HORSES);
            stmt.execute(CREATE_COOLDOWNS);
            stmt.execute(CREATE_INDEX_OWNER);
        }
        logger.info("[NemonicHorse] Database initialized: " + dbFile.getName());
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.severe("[NemonicHorse] Error closing database: " + e.getMessage());
        }
    }

    @Override
    public synchronized Optional<HorseData> findById(UUID horseId) {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_HORSE)) {
            ps.setString(1, horseId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                HorseData data = mapRow(rs);
                loadCooldowns(data);
                return Optional.of(data);
            }
        } catch (SQLException e) {
            logger.warning("[NemonicHorse] findById error: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public synchronized List<HorseData> findByOwner(UUID ownerId) {
        List<HorseData> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_OWNER)) {
            ps.setString(1, ownerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HorseData data = mapRow(rs);
                    loadCooldowns(data);
                    result.add(data);
                }
            }
        } catch (SQLException e) {
            logger.warning("[NemonicHorse] findByOwner error: " + e.getMessage());
        }
        return result;
    }

    @Override
    public synchronized void save(HorseData data) {
        try {
            upsertHorse(data);
            upsertCooldowns(data);
            data.markClean();
        } catch (SQLException e) {
            logger.warning("[NemonicHorse] save error for " + data.getHorseId() + ": " + e.getMessage());
        }
    }

    @Override
    public synchronized void saveBatch(Collection<HorseData> dataList) {
        try {
            connection.setAutoCommit(false);
            for (HorseData data : dataList) {
                upsertHorse(data);
                upsertCooldowns(data);
                data.markClean();
            }
            connection.commit();
        } catch (SQLException e) {
            logger.warning("[NemonicHorse] saveBatch error: " + e.getMessage());
            try { connection.rollback(); } catch (SQLException ignored) {}
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public synchronized void delete(UUID horseId) {
        try (PreparedStatement ps = connection.prepareStatement(DELETE_HORSE)) {
            ps.setString(1, horseId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[NemonicHorse] delete error: " + e.getMessage());
        }
    }

    // ── Private helpers (called only from synchronized methods) ──────

    private void upsertHorse(HorseData d) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_HORSE)) {
            ps.setString(1, d.getHorseId().toString());
            ps.setString(2, d.getOwnerId().toString());
            ps.setString(3, d.getName());
            ps.setString(4, d.getRarity().name());
            ps.setString(5, d.getRace().name());
            ps.setInt(6, d.getLevel());
            ps.setDouble(7, d.getXp());
            ps.setInt(8, d.getBondLevel());
            ps.setDouble(9, d.getBondXp());
            ps.setString(10, d.getEquippedSkillId());
            ps.setLong(11, d.getCreatedAt());
            ps.setLong(12, d.getUpdatedAt());
            ps.executeUpdate();
        }
    }

    private void upsertCooldowns(HorseData d) throws SQLException {
        if (d.getSkillCooldowns().isEmpty()) return;
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_COOLDOWN)) {
            for (Map.Entry<String, Long> entry : d.getSkillCooldowns().entrySet()) {
                ps.setString(1, d.getHorseId().toString());
                ps.setString(2, entry.getKey());
                ps.setLong(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void loadCooldowns(HorseData data) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_COOLDOWNS)) {
            ps.setString(1, data.getHorseId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.getSkillCooldowns().put(
                            rs.getString("skill_id"),
                            rs.getLong("last_used"));
                }
            }
        }
    }

    private HorseData mapRow(ResultSet rs) throws SQLException {
        return new HorseData(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("owner_id")),
                rs.getString("name"),
                HorseRarity.fromName(rs.getString("rarity")),
                HorseRace.fromName(rs.getString("race")),
                rs.getInt("level"),
                rs.getDouble("xp"),
                rs.getInt("bond_level"),
                rs.getDouble("bond_xp"),
                rs.getString("equipped_skill"),
                new HashMap<>(),
                rs.getLong("created_at"),
                rs.getLong("updated_at")
        );
    }
}
