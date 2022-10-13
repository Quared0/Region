package me.quared.region.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConnection {

	private final String host, database, username, password;
	private final int port;

	private final HikariDataSource hikariDataSource;

	public DatabaseConnection(String host, int port, String database, String username, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;

		this.hikariDataSource = new HikariDataSource(getHikariConfig());
	}

	private HikariConfig getHikariConfig() {
		HikariConfig config = new HikariConfig();

		config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
		config.setPoolName("region-hikari");
		config.setUsername(username);
		config.setPassword(password);
		config.setMaximumPoolSize(8);
		config.setMinimumIdle(8);
		config.setMaxLifetime(1800000);
		config.setConnectionTimeout(5000);

		config.addDataSourceProperty("cachePrepStmts", true);
		config.addDataSourceProperty("prepStmtCacheSize", 250);
		config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
		config.addDataSourceProperty("useServerPrepStmts", true);
		config.addDataSourceProperty("useLocalSessionState", true);
		config.addDataSourceProperty("rewriteBatchedStatements", true);
		config.addDataSourceProperty("cacheResultSetMetadata", true);
		config.addDataSourceProperty("cacheServerConfiguration", true);
		config.addDataSourceProperty("elideSetAutoCommits", true);
		config.addDataSourceProperty("maintainTimeStats", false);

		return config;
	}

	public HikariDataSource getDataSource() {
		return hikariDataSource;
	}

	public String getHost() {
		return host;
	}

	public String getDatabase() {
		return database;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public int getPort() {
		return port;
	}

	public void close() {
		hikariDataSource.close();
	}

}
