package com.ocs.xd.module.scriptjdbc.itemwriter;

import com.ocs.xd.module.scriptjdbc.model.Script;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcParameterUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Goodarzi on 7/28/2016.
 */
public class MyJdbcBatchItemWriter<T> implements ItemWriter<T>, InitializingBean {


    protected static final Log logger = LogFactory.getLog(JdbcBatchItemWriter.class);

    private NamedParameterJdbcOperations namedParameterJdbcTemplate;

    private JdbcTemplate jdbcTemplate;

    private ItemPreparedStatementSetter<T> itemPreparedStatementSetter;

    private ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider;

    private String sql;

    private boolean assertUpdates = true;

    private int parameterCount;

    private boolean usingNamedParameters;

    /**
     * Public setter for the flag that determines whether an assertion is made
     * that all items cause at least one row to be updated.
     * @param assertUpdates the flag to set. Defaults to true;
     */
    public void setAssertUpdates(boolean assertUpdates) {
        this.assertUpdates = assertUpdates;
    }

    /**
     * Public setter for the query string to execute on write. The parameters
     * should correspond to those known to the
     * {@link ItemPreparedStatementSetter}.
     * @param sql the query to set
     */
    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * Public setter for the {@link ItemPreparedStatementSetter}.
     * @param preparedStatementSetter the {@link ItemPreparedStatementSetter} to
     * set. This is required when using traditional '?' placeholders for the SQL statement.
     */
    public void setItemPreparedStatementSetter(ItemPreparedStatementSetter<T> preparedStatementSetter) {
        this.itemPreparedStatementSetter = preparedStatementSetter;
    }

    /**
     * Public setter for the {@link ItemSqlParameterSourceProvider}.
     * @param itemSqlParameterSourceProvider the {@link ItemSqlParameterSourceProvider} to
     * set. This is required when using named parameters for the SQL statement and the type
     * to be written does not implement {@link Map}.
     */
    public void setItemSqlParameterSourceProvider(ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider) {
        this.itemSqlParameterSourceProvider = itemSqlParameterSourceProvider;
    }

    /**
     * Public setter for the data source for injection purposes.
     *
     * @param dataSource {@link javax.sql.DataSource} to use for querying against
     */
    public void setDataSource(DataSource dataSource) {
        if (jdbcTemplate == null) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }

    /**
     * Public setter for the {@link NamedParameterJdbcOperations}.
     * @param jdbcTemplate the {@link NamedParameterJdbcOperations} to set
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Check mandatory properties - there must be a SimpleJdbcTemplate and an SQL statement plus a
     * parameter source.
     */
    @Override
    public void afterPropertiesSet() {
        Assert.notNull(jdbcTemplate, "A DataSource or a jdbcTemplate is required.");
        Assert.notNull(sql, "An SQL statement is required.");
        List<String> namedParameters = new ArrayList<String>();
        parameterCount = JdbcParameterUtils.countParameterPlaceholders(sql, namedParameters);
        if (namedParameters.size() > 0) {
            if (parameterCount != namedParameters.size()) {
                throw new InvalidDataAccessApiUsageException("You can't use both named parameters and classic \"?\" placeholders: " + sql);
            }
            usingNamedParameters = true;
        }
        if (!usingNamedParameters) {
            Assert.notNull(itemPreparedStatementSetter, "Using SQL statement with '?' placeholders requires an ItemPreparedStatementSetter");
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void write(final List<? extends T> items) throws Exception {

        if (!items.isEmpty()) {

            //if (logger.isDebugEnabled()) {
                logger.info("Executing batch with " + items.size() + " items.");
            //}

            int[] updateCounts = null;

            if (usingNamedParameters) {
                if(items.get(0) instanceof Map) {
                    updateCounts = namedParameterJdbcTemplate.batchUpdate(sql, items.toArray(new Map[0]));
                } else {
                    SqlParameterSource[] batchArgs = new SqlParameterSource[items.size()];
                    int i = 0;
                    for (T item : items) {
                        batchArgs[i++] = itemSqlParameterSourceProvider.createSqlParameterSource(item);
                    }

                    String[] batchString = new String[items.size()];
                    for (int j = 0; j < batchString.length; j++) {
                        batchString[j] = ((Script)items.get(j)).getScriptValue();
                    }
                    updateCounts = jdbcTemplate.batchUpdate(batchString);

                    //updateCounts = namedParameterJdbcTemplate.batchUpdate(sql, batchArgs);
                }
            }
            else {
                updateCounts = namedParameterJdbcTemplate.getJdbcOperations().execute(sql, new PreparedStatementCallback<int[]>() {
                    @Override
                    public int[] doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                        for (T item : items) {
                            itemPreparedStatementSetter.setValues(item, ps);
                            ps.addBatch();
                        }
                        return ps.executeBatch();
                    }
                });
            }

            if (assertUpdates) {
                for (int i = 0; i < updateCounts.length; i++) {
                    int value = updateCounts[i];
                    if (value == 0) {
                        throw new EmptyResultDataAccessException("Item " + i + " of " + updateCounts.length
                                + " did not update any rows: [" + items.get(i) + "]", 1);
                    }
                }
            }
        }
    }

}
