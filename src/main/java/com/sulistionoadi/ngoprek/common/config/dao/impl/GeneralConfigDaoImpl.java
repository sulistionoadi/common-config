package com.sulistionoadi.ngoprek.common.config.dao.impl;

import static com.sulistionoadi.ngoprek.common.constant.ErrorCode.*;
import static com.sulistionoadi.ngoprek.common.pss.constant.PssConstant.*;
import static com.sulistionoadi.ngoprek.common.pss.utils.PssUtils.*;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.sulistionoadi.ngoprek.common.config.dao.GeneralConfigDao;
import com.sulistionoadi.ngoprek.common.config.dto.GeneralConfigDTO;
import com.sulistionoadi.ngoprek.common.config.rowmapper.GeneralConfigRowMapper;
import com.sulistionoadi.ngoprek.common.dto.RequestDelete;
import com.sulistionoadi.ngoprek.common.dto.RequestSetActive;
import com.sulistionoadi.ngoprek.common.dto.StatusActive;
import com.sulistionoadi.ngoprek.common.exception.CommonRuntimeException;
import com.sulistionoadi.ngoprek.common.pss.dto.PssFilter;
import com.sulistionoadi.ngoprek.common.utils.CombinedSqlParameterSource;
import com.sulistionoadi.ngoprek.common.utils.DaoUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class GeneralConfigDaoImpl extends DaoUtils implements GeneralConfigDao {

	private static final long serialVersionUID = 2704013855451564560L;
	
	private final DataSource datasource;
	private final String appname;
	
	@Autowired
	public GeneralConfigDaoImpl(DataSource datasource, @Value("${app.name:MYAPP}") String appname) {
		this.datasource = datasource;
		this.appname = appname;
	}

	@Override
	public void save(GeneralConfigDTO t) {
		Optional<GeneralConfigDTO> existsByCode = this.findByCode(t.getConfigCode());
		if(existsByCode.isPresent()) {
				throw new CommonRuntimeException(RC_DATA_ALREADY_EXIST, "Config code already exists");
		}
		
		String sql = "INSERT INTO cm_general_config ("
				   + "    created_by, created_date, updated_by, updated_date,"
				   + "    is_active, appname, config_code, config_value "
				   + ") VALUES ("
				   + "    :createdBy, :createdDate, :updatedBy, :updatedDate, "
				   + "    :isActive, :appname, :configCode, :configValue "
				   + ")";

		try {
			CombinedSqlParameterSource params = new CombinedSqlParameterSource(t);
			params.addValue("appname", this.appname);
			
			getNamedParameterJdbcTemplate(this.datasource).update(sql, params);
			log.info("Save GeneralConfig successfully");
		} catch (Exception ex) {
			if(ex.getMessage().toLowerCase().indexOf("unique constraint")>-1)
				throw new CommonRuntimeException(RC_DATA_ALREADY_EXIST, "Data already exists");
			else
				throw new CommonRuntimeException(RC_DB_QUERY_ERROR, "Cannot save GeneralConfig", ex);
		}
	}

	@Override
	public void update(GeneralConfigDTO t) {
		Optional<GeneralConfigDTO> op = findOne(t.getId());
		if (!op.isPresent()) {
			throw new CommonRuntimeException(RC_DATA_NOT_FOUND, "GeneralConfig with id:" + t.getId() + " not found");
		}
		
		Optional<GeneralConfigDTO> existsByCode = this.findByCode(t.getConfigCode());
		if(existsByCode.isPresent() && !existsByCode.get().getId().equals(t.getId())) {
				throw new CommonRuntimeException(RC_DATA_ALREADY_EXIST, "Config code already exists");
		}
		
		String sql = "UPDATE cm_general_config SET "
				   + "    updated_by=:updatedBy, updated_date=:updatedDate, "
				   + "    config_code=:configCode, config_value=:configValue "
				   + "WHERE id=:id "
				   + "  AND appname=:appname";

		try {
			validateRecordBeforeUpdate(op.get());
			CombinedSqlParameterSource params = new CombinedSqlParameterSource(t);
			params.addValue("appname", this.appname);
			
			getNamedParameterJdbcTemplate(this.datasource).update(sql, params);
			log.info("Update GeneralConfig with id:{} successfully", t.getId());
		} catch (Exception ex) {
			if(ex.getMessage().toLowerCase().indexOf("unique constraint")>-1)
				throw new CommonRuntimeException(RC_DATA_ALREADY_EXIST, "Data already exists");
			else
				throw new CommonRuntimeException(RC_DB_QUERY_ERROR, "Cannot update GeneralConfig", ex);
		}
	}

	@Override
	public Optional<GeneralConfigDTO> findOne(Long id) {
		String sql = "SELECT c.* FROM cm_general_config c WHERE c.id=? AND c.appname=? AND c.is_deleted=0";
		try {
			log.debug("Get GeneralConfig with id:{}", id);
			GeneralConfigDTO dto = getJdbcTemplate(datasource).queryForObject(sql, 
					new Object[] { id, this.appname }, new GeneralConfigRowMapper());
			return Optional.of(dto);
		} catch (EmptyResultDataAccessException ex) {
			log.warn("GeneralConfig with id:{} not found", id);
			return Optional.empty();
		} catch (Exception ex) {
			throw new CommonRuntimeException(RC_DB_QUERY_ERROR, MessageFormat.format("Cannot get GeneralConfig with id:{0}", 
					id != null ? id.toString() : "null"), ex);
		}
	}

	@Override
	public Long count(PssFilter filter, StatusActive statusActive) {
		Map<String, Object> param = generateCountPssParameter(filter);
		param.put("appname", this.appname);

		String sql = "SELECT COUNT(c.id) FROM cm_general_config c " 
				   + "WHERE c.appname = :appname AND c.is_deleted=0 ";
		if(statusActive!=null) {
			sql += "    AND c.is_active=:isActive ";
			param.put("isActive", statusActive.equals(StatusActive.YES) ? 1 : 0);
		}
		if (StringUtils.hasText(filter.getSearch().get(PSS_SEARCH_VAL))) {
			sql += "    AND ( ";
			sql += "            lower(c.config_code) LIKE :filter ";
			sql += "            lower(c.config_value) LIKE :filter ";
			sql += "    ) ";
		}

		log.debug("Count list data GeneralConfig filter by {}", param);

		try {
			return getNamedParameterJdbcTemplate(datasource).queryForObject(sql, param, Long.class);
		} catch (Exception ex) {
			throw new CommonRuntimeException(RC_DB_QUERY_ERROR, "Cannot get count list data GeneralConfig", ex);
		}
	}

	@Override
	public List<GeneralConfigDTO> filter(PssFilter filter, StatusActive statusActive) {
		String[] orderableColums = new String[]{"config_code", "config_value"};
		Map<String, Object> param = generatePssParameter(filter);
		param.put("appname", this.appname);

		String q= "SELECT rs.* FROM ( " 
				+ "    SELECT dt.*, " 
				+ "           row_number() over ( " 
				+ "               ORDER BY DT." + getOrderBy(filter, "ID", orderableColums)
				+ "           ) line_number " 
				+ "    FROM ( " 
				+ "        SELECT c.* FROM cm_general_config c " 
				+ "        WHERE c.appname = :appname AND c.is_deleted=0 "; 
		
		if(statusActive!=null) {
			q += "           AND c.is_active=:isActive ";
			param.put("isActive", statusActive.equals(StatusActive.YES) ? 1 : 0);
		}
		if (StringUtils.hasText(filter.getSearch().get(PSS_SEARCH_VAL))) {
			q += "           AND ( ";
			q += "            lower(c.config_code) LIKE :filter OR lower(c.config_value) LIKE :filter ";
			q += "         ) ";
		}
			q += "    ) dt ) rs WHERE line_number BETWEEN :start_row AND :end_row ORDER BY line_number";

		log.debug("Get list data GeneralConfig filter by {}", param);

		try {
			List<GeneralConfigDTO> datas = getNamedParameterJdbcTemplate(datasource).query(q, param, new GeneralConfigRowMapper());
			return datas;
		} catch (Exception ex) {
			throw new CommonRuntimeException(RC_DB_QUERY_ERROR, "Cannot get list data GeneralConfig", ex);
		}
	}

	@Override
	public void delete(RequestDelete<Long> req) {
		Optional<GeneralConfigDTO> op = this.findOne(req.getId());
		if (!op.isPresent()) {
			throw new CommonRuntimeException(RC_DATA_NOT_FOUND, "GeneralConfig with id:" + req.getId() + " not found");
		}

		String q = "DELETE FROM cm_general_config WHERE id=? AND appname=?";
		try {
			validateRecordBeforeUpdate(op.get());
			getJdbcTemplate(datasource).update(q, req.getId(), this.appname);
			log.info("Delete GeneralConfig with id:{} successfully", req.getId());
		} catch (Exception ex) {
			if (ex.getMessage().toLowerCase().indexOf("constraint")>-1) {
				this.setAsDelete(req);
			} else {
				throw new CommonRuntimeException(RC_DB_QUERY_ERROR, "Cannot delete GeneralConfig with id:" + req.getId(), ex);				
			}
		}
	}

	private void setAsDelete(RequestDelete<Long> req) {
		try {
			String q = "UPDATE cm_general_config SET "
					 + "       is_deleted=1, "
					 + "       updated_by=?, updated_date=? "
					 + "WHERE id=? AND appname=?";
			
			log.warn("GeneralConfig with id:{} will be flag as isDeleted", req.getId());
			getJdbcTemplate(datasource).update(q, req.getUpdatedBy(), new Date(), req.getId(), this.appname);
			log.info("Flag isDeleted for GeneralConfig with id:{} successfully", req.getId());
		} catch(Exception ex) {
			throw new CommonRuntimeException(RC_DB_QUERY_ERROR, "Cannot flag isDeleted for GeneralConfig with id:" + req.getId(), ex);
		}
	}

	@Override
	public void setActive(RequestSetActive<Long> req) {
		Optional<GeneralConfigDTO> op = this.findOne(req.getId());
		if (!op.isPresent()) {
			throw new CommonRuntimeException(RC_DATA_NOT_FOUND, "GeneralConfig with id:" + req.getId() + " not found");
		}
		
		String q = "UPDATE cm_general_config SET is_active=?, updated_by=?, updated_date=? WHERE id=? AND appname=?";
		try {
			validateRecordBeforeUpdate(op.get());
			Integer boolVal = req.getStatus()!=null && req.getStatus().equals(StatusActive.YES) ? 1:0;
			getJdbcTemplate(datasource).update(q, boolVal, req.getUpdatedBy(), new Date(), req.getId(), this.appname);
			log.info("Flag isActive={} for GeneralConfig with id:{} successfully", req.getStatus(), req.getId());
		} catch (Exception ex) {
			throw new CommonRuntimeException(RC_DB_QUERY_ERROR, MessageFormat
					.format("Cannot update flag isActive={0} for GeneralConfig with id:{1}", req.getStatus(), req.getId().toString()), ex);
		}
	}

	@Override
	public Optional<GeneralConfigDTO> findByCode(String code) {
		String sql = "SELECT c.* FROM cm_general_config c WHERE c.config_code=? AND c.appname=? AND c.is_deleted=0 AND c.is_active=1";
		try {
			log.debug("Get GeneralConfig with code:{}", code);
			GeneralConfigDTO dto = getJdbcTemplate(datasource).queryForObject(sql, 
					new Object[] { code, this.appname }, new GeneralConfigRowMapper());
			return Optional.of(dto);
		} catch (EmptyResultDataAccessException ex) {
			log.warn("GeneralConfig with code:{} not found", code);
			return Optional.empty();
		} catch (Exception ex) {
			throw new CommonRuntimeException(RC_DB_QUERY_ERROR, MessageFormat.format("Cannot get GeneralConfig with code:{0}", code), ex);
		}
	}

}
